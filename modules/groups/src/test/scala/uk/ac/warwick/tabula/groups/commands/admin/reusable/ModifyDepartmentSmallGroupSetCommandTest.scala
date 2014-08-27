package uk.ac.warwick.tabula.groups.commands.admin.reusable

import org.joda.time.DateTime
import org.springframework.validation.BindException
import uk.ac.warwick.tabula.commands.{DescriptionImpl, SelfValidating, Describable, Appliable}
import uk.ac.warwick.tabula.data.model.groups.DepartmentSmallGroupSet
import uk.ac.warwick.tabula.permissions.Permissions
import uk.ac.warwick.tabula.services.{SmallGroupService, SmallGroupServiceComponent}
import uk.ac.warwick.tabula.system.permissions.PermissionsChecking
import uk.ac.warwick.tabula._

class ModifyDepartmentSmallGroupSetCommandTest extends TestBase with Mockito {

	private trait CommandTestSupport extends SmallGroupServiceComponent {
		val smallGroupService = mock[SmallGroupService]
	}

	private trait Fixture {
		val department = Fixtures.department("in", "IT Services")
	}

	private trait ExistingSetFixture extends Fixture {
		val set = new DepartmentSmallGroupSet(department)
		set.id = "existingId"
		set.name = "Existing set"
	}

	private trait CreateCommandFixture extends Fixture {
		val command = new CreateDepartmentSmallGroupSetCommandInternal(department) with CommandTestSupport
	}

	private trait EditCommandFixture extends ExistingSetFixture {
		val command = new EditDepartmentSmallGroupSetCommandInternal(department, set) with CommandTestSupport
	}

	@Test def create { new CreateCommandFixture {
		command.name = "Set name"
		command.academicYear = AcademicYear.guessSITSAcademicYearByDate(DateTime.now)

		val set = command.applyInternal()
		set.name should be ("Set name")
		set.academicYear should be (AcademicYear.guessSITSAcademicYearByDate(DateTime.now))
		set.members should not be (null)

		there was one (command.smallGroupService).saveOrUpdate(set)
	}}

	@Test def edit { new EditCommandFixture {
		command.name should be (set.name)
		command.academicYear should be (set.academicYear)

		command.name = "Set name"
		command.academicYear = AcademicYear.guessSITSAcademicYearByDate(DateTime.now)

		command.applyInternal() should be (set)
		set.name should be ("Set name")
		set.academicYear should be (AcademicYear.guessSITSAcademicYearByDate(DateTime.now))
		set.members should not be (null)

		there was one (command.smallGroupService).saveOrUpdate(set)
	}}

	@Test def createPermissions { new Fixture {
		val theDepartment = department
		val command = new CreateDepartmentSmallGroupSetPermissions with CreateDepartmentSmallGroupSetCommandState {
			val department = theDepartment
		}

		val checking = mock[PermissionsChecking]
		command.permissionsCheck(checking)

		there was one(checking).PermissionCheck(Permissions.SmallGroups.Create, department)
	}}

	@Test(expected = classOf[ItemNotFoundException]) def createNoDepartment {
		val command = new CreateDepartmentSmallGroupSetPermissions with CreateDepartmentSmallGroupSetCommandState {
			val department = null
		}

		val checking = mock[PermissionsChecking]
		command.permissionsCheck(checking)
	}

	@Test def editPermissions { new ExistingSetFixture {
		val (d, s) = (department, set)

		val command = new EditDepartmentSmallGroupSetPermissions with EditDepartmentSmallGroupSetCommandState {
			val department = d
			val smallGroupSet = s
		}

		val checking = mock[PermissionsChecking]
		command.permissionsCheck(checking)

		there was one(checking).PermissionCheck(Permissions.SmallGroups.Update, set)
	}}

	@Test(expected = classOf[ItemNotFoundException]) def editNoDepartment {
		val command = new EditDepartmentSmallGroupSetPermissions with EditDepartmentSmallGroupSetCommandState {
			val department = null
			val smallGroupSet = new DepartmentSmallGroupSet
		}

		val checking = mock[PermissionsChecking]
		command.permissionsCheck(checking)
	}

	@Test(expected = classOf[ItemNotFoundException]) def editNoSet {
		val command = new EditDepartmentSmallGroupSetPermissions with EditDepartmentSmallGroupSetCommandState {
			val department = Fixtures.department("in")
			val smallGroupSet = null
		}

		val checking = mock[PermissionsChecking]
		command.permissionsCheck(checking)
	}

	@Test(expected = classOf[ItemNotFoundException]) def editUnlinkedSet {
		val command = new EditDepartmentSmallGroupSetPermissions with EditDepartmentSmallGroupSetCommandState {
			val department = Fixtures.department("in")
			department.id = "set id"

			val smallGroupSet = new DepartmentSmallGroupSet(Fixtures.department("other"))
		}

		val checking = mock[PermissionsChecking]
		command.permissionsCheck(checking)
	}

	private trait ValidationFixture extends Fixture {
		val command = new ModifyDepartmentSmallGroupSetCommandValidation with ModifyDepartmentSmallGroupSetState {
			val department = ValidationFixture.this.department
			val existingSet = None
		}
	}

	private trait ValidationFixtureExistingSet extends ExistingSetFixture {
		val command = new ModifyDepartmentSmallGroupSetCommandValidation with ModifyDepartmentSmallGroupSetState {
			val department = ValidationFixtureExistingSet.this.department
			val existingSet = Some(ValidationFixtureExistingSet.this.set)
		}
	}

	@Test def validationPasses { new ValidationFixture {
		command.name = "That's not my name"
		command.academicYear = AcademicYear.guessSITSAcademicYearByDate(DateTime.now)

		val errors = new BindException(command, "command")
		command.validate(errors)

		errors.hasErrors should be (false)
	}}

	@Test def validateNoName { new ValidationFixture {
		command.name = "             "
		command.academicYear = AcademicYear.guessSITSAcademicYearByDate(DateTime.now)

		val errors = new BindException(command, "command")
		command.validate(errors)

		errors.hasErrors should be (true)
		errors.getErrorCount should be (1)
		errors.getFieldError.getField should be ("name")
		errors.getFieldError.getCodes should contain ("smallGroupSet.name.NotEmpty")
	}}

	@Test def validateNameTooLong { new ValidationFixture {
		command.name = (1 to 300).map { _ => "a" }.mkString("")
		command.academicYear = AcademicYear.guessSITSAcademicYearByDate(DateTime.now)

		val errors = new BindException(command, "command")
		command.validate(errors)

		errors.hasErrors should be (true)
		errors.getErrorCount should be (1)
		errors.getFieldError.getField should be ("name")
		errors.getFieldError.getCodes should contain ("smallGroupSet.name.Length")
	}}

	@Test def validateCantChangeAcademicYear { new ValidationFixtureExistingSet {
		set.academicYear = AcademicYear.guessSITSAcademicYearByDate(DateTime.now)

		command.name = "That's not my name"
		command.academicYear = set.academicYear + 1

		val errors = new BindException(command, "command")
		command.validate(errors)

		errors.hasErrors should be (true)
		errors.getErrorCount should be (1)
		errors.getFieldError.getField should be ("academicYear")
		errors.getFieldError.getCodes should contain ("smallGroupSet.academicYear.cantBeChanged")
	}}

	@Test def describeCreate { new Fixture {
		val dept = department
		val command = new CreateDepartmentSmallGroupSetDescription with CreateDepartmentSmallGroupSetCommandState {
			override val eventName = "test"
			val department = dept
		}

		command.name = "new name"

		val d = new DescriptionImpl
		command.describe(d)

		d.allProperties should be (Map(
			"department" -> "in",
			"name" -> "new name"
		))
	}}

	@Test def describeEdit { new ExistingSetFixture {
		val (dept, s) = (department, set)
		val command = new EditDepartmentSmallGroupSetDescription with EditDepartmentSmallGroupSetCommandState {
			override val eventName = "test"
			val department = dept
			val smallGroupSet = s
		}

		val d = new DescriptionImpl
		command.describe(d)

		d.allProperties should be (Map(
			"department" -> "in",
			"smallGroupSet" -> "existingId"
		))
	}}

	@Test def wiresCreate { new Fixture {
		val command = ModifyDepartmentSmallGroupSetCommand.create(department)

		command should be (anInstanceOf[Appliable[DepartmentSmallGroupSet]])
		command should be (anInstanceOf[Describable[DepartmentSmallGroupSet]])
		command should be (anInstanceOf[SelfValidating])
		command should be (anInstanceOf[CreateDepartmentSmallGroupSetPermissions])
		command should be (anInstanceOf[CreateDepartmentSmallGroupSetCommandState])
	}}

	@Test def wiresEdit { new ExistingSetFixture {
		val command = ModifyDepartmentSmallGroupSetCommand.edit(department, set)

		command should be (anInstanceOf[Appliable[DepartmentSmallGroupSet]])
		command should be (anInstanceOf[Describable[DepartmentSmallGroupSet]])
		command should be (anInstanceOf[SelfValidating])
		command should be (anInstanceOf[EditDepartmentSmallGroupSetPermissions])
		command should be (anInstanceOf[EditDepartmentSmallGroupSetCommandState])
	}}

}
