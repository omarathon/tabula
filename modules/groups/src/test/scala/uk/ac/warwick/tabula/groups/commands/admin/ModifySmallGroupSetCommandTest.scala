package uk.ac.warwick.tabula.groups.commands.admin

import org.joda.time.DateTime
import org.springframework.validation.BindException
import uk.ac.warwick.tabula._
import uk.ac.warwick.tabula.commands.{Appliable, Describable, DescriptionImpl, SelfValidating}
import uk.ac.warwick.tabula.data.model.groups.{SmallGroupAllocationMethod, SmallGroupFormat, SmallGroupSet}
import uk.ac.warwick.tabula.permissions.Permissions
import uk.ac.warwick.tabula.services.{SmallGroupService, SmallGroupServiceComponent}
import uk.ac.warwick.tabula.system.permissions.PermissionsChecking

class ModifySmallGroupSetCommandTest extends TestBase with Mockito {

	private trait CommandTestSupport extends SmallGroupServiceComponent {
		val smallGroupService = mock[SmallGroupService]
	}

	private trait Fixture {
		val module = Fixtures.module("in101", "Introduction to Scala")
		module.id = "moduleId"
	}

	private trait ExistingSetFixture extends Fixture {
		val set = new SmallGroupSet(module)
		set.id = "existingId"
		set.name = "Existing set"
	}

	private trait CreateCommandFixture extends Fixture {
		val command = new CreateSmallGroupSetCommandInternal(module) with CommandTestSupport
	}

	private trait EditCommandFixture extends ExistingSetFixture {
		val command = new EditSmallGroupSetCommandInternal(module, set) with CommandTestSupport
	}

	@Test def create { new CreateCommandFixture {
		command.name = "Set name"
		command.academicYear = AcademicYear.guessByDate(DateTime.now)

		val set = command.applyInternal()
		set.name should be ("Set name")
		set.academicYear should be (AcademicYear.guessByDate(DateTime.now))
		set.members should not be (null)

		there was one (command.smallGroupService).saveOrUpdate(set)
	}}

	@Test def edit { new EditCommandFixture {
		command.name should be (set.name)
		command.academicYear should be (set.academicYear)

		command.name = "Set name"
		command.academicYear = AcademicYear.guessByDate(DateTime.now)

		command.applyInternal() should be (set)
		set.name should be ("Set name")
		set.academicYear should be (AcademicYear.guessByDate(DateTime.now))
		set.members should not be (null)

		there was one (command.smallGroupService).saveOrUpdate(set)
	}}

	@Test def createPermissions { new Fixture {
		val theDepartment = module
		val command = new CreateSmallGroupSetPermissions with CreateSmallGroupSetCommandState {
			val module = theDepartment
		}

		val checking = mock[PermissionsChecking]
		command.permissionsCheck(checking)

		there was one(checking).PermissionCheck(Permissions.SmallGroups.Create, module)
	}}

	@Test(expected = classOf[ItemNotFoundException]) def createNoDepartment {
		val command = new CreateSmallGroupSetPermissions with CreateSmallGroupSetCommandState {
			val module = null
		}

		val checking = mock[PermissionsChecking]
		command.permissionsCheck(checking)
	}

	@Test def editPermissions { new ExistingSetFixture {
		val (m, s) = (module, set)

		val command = new EditSmallGroupSetPermissions with EditSmallGroupSetCommandState {
			val module = m
			val set = s
		}

		val checking = mock[PermissionsChecking]
		command.permissionsCheck(checking)

		there was one(checking).PermissionCheck(Permissions.SmallGroups.Update, set)
	}}

	@Test(expected = classOf[ItemNotFoundException]) def editNoDepartment {
		val command = new EditSmallGroupSetPermissions with EditSmallGroupSetCommandState {
			val module = null
			val set = new SmallGroupSet
		}

		val checking = mock[PermissionsChecking]
		command.permissionsCheck(checking)
	}

	@Test(expected = classOf[ItemNotFoundException]) def editNoSet {
		val command = new EditSmallGroupSetPermissions with EditSmallGroupSetCommandState {
			val module = Fixtures.module("in101")
			val set = null
		}

		val checking = mock[PermissionsChecking]
		command.permissionsCheck(checking)
	}

	@Test(expected = classOf[ItemNotFoundException]) def editUnlinkedSet {
		val command = new EditSmallGroupSetPermissions with EditSmallGroupSetCommandState {
			val module = Fixtures.module("in101")
			module.id = "set id"

			val set = new SmallGroupSet(Fixtures.module("other"))
		}

		val checking = mock[PermissionsChecking]
		command.permissionsCheck(checking)
	}

	private trait ValidationFixture extends Fixture {
		val command = new ModifySmallGroupSetValidation with ModifySmallGroupSetCommandState {
			val module = ValidationFixture.this.module
			val existingSet = None
		}
	}

	private trait ValidationFixtureExistingSet extends ExistingSetFixture {
		val command = new ModifySmallGroupSetValidation with ModifySmallGroupSetCommandState {
			val module = ValidationFixtureExistingSet.this.module
			val existingSet = Some(ValidationFixtureExistingSet.this.set)
		}
	}

	@Test def validationPasses { new ValidationFixture {
		command.name = "That's not my name"
		command.academicYear = AcademicYear.guessByDate(DateTime.now)
		command.format = SmallGroupFormat.Seminar
		command.allocationMethod = SmallGroupAllocationMethod.Manual

		val errors = new BindException(command, "command")
		command.validate(errors)

		errors.hasErrors should be (false)
	}}

	@Test def validateNoName { new ValidationFixture {
		command.name = "             "
		command.academicYear = AcademicYear.guessByDate(DateTime.now)
		command.format = SmallGroupFormat.Seminar
		command.allocationMethod = SmallGroupAllocationMethod.Manual

		val errors = new BindException(command, "command")
		command.validate(errors)

		errors.hasErrors should be (true)
		errors.getErrorCount should be (1)
		errors.getFieldError.getField should be ("name")
		errors.getFieldError.getCodes should contain ("smallGroupSet.name.NotEmpty")
	}}

	@Test def validateNameTooLong { new ValidationFixture {
		command.name = (1 to 300).map { _ => "a" }.mkString("")
		command.academicYear = AcademicYear.guessByDate(DateTime.now)
		command.format = SmallGroupFormat.Seminar
		command.allocationMethod = SmallGroupAllocationMethod.Manual

		val errors = new BindException(command, "command")
		command.validate(errors)

		errors.hasErrors should be (true)
		errors.getErrorCount should be (1)
		errors.getFieldError.getField should be ("name")
		errors.getFieldError.getCodes should contain ("smallGroupSet.name.Length")
	}}

	@Test def validateNoFormat { new ValidationFixture {
		command.name = "That's not my name"
		command.academicYear = AcademicYear.guessByDate(DateTime.now)
		command.allocationMethod = SmallGroupAllocationMethod.Manual

		val errors = new BindException(command, "command")
		command.validate(errors)

		errors.hasErrors should be (true)
		errors.getErrorCount should be (1)
		errors.getFieldError.getField should be ("format")
		errors.getFieldError.getCodes should contain ("smallGroupSet.format.NotEmpty")
	}}

	@Test def validateNoAllocationMethod { new ValidationFixture {
		command.name = "That's not my name"
		command.academicYear = AcademicYear.guessByDate(DateTime.now)
		command.format = SmallGroupFormat.Seminar
		command.allocationMethod = null

		val errors = new BindException(command, "command")
		command.validate(errors)

		errors.hasErrors should be (true)
		errors.getErrorCount should be (1)
		errors.getFieldError.getField should be ("allocationMethod")
		errors.getFieldError.getCodes should contain ("smallGroupSet.allocationMethod.NotEmpty")
	}}

	@Test def validateCantChangeAcademicYear { new ValidationFixtureExistingSet {
		set.academicYear = AcademicYear.guessByDate(DateTime.now)

		command.name = "That's not my name"
		command.academicYear = set.academicYear + 1
		command.format = SmallGroupFormat.Seminar
		command.allocationMethod = SmallGroupAllocationMethod.Manual

		val errors = new BindException(command, "command")
		command.validate(errors)

		errors.hasErrors should be (true)
		errors.getErrorCount should be (1)
		errors.getFieldError.getField should be ("academicYear")
		errors.getFieldError.getCodes should contain ("smallGroupSet.academicYear.cantBeChanged")
	}}

	@Test def describeCreate { new Fixture {
		val mod = module
		val command = new CreateSmallGroupSetDescription with CreateSmallGroupSetCommandState {
			override val eventName = "test"
			val module = mod
		}

		command.name = "new name"

		val d = new DescriptionImpl
		command.describe(d)

		d.allProperties should be (Map(
			"module" -> "moduleId",
			"name" -> "new name"
		))
	}}

	@Test def describeEdit { new ExistingSetFixture {
		val (mod, s) = (module, set)
		val command = new EditSmallGroupSetDescription with EditSmallGroupSetCommandState {
			override val eventName = "test"
			val module = mod
			val set = s
		}

		val d = new DescriptionImpl
		command.describe(d)

		d.allProperties should be (Map(
			"module" -> "moduleId",
			"smallGroupSet" -> "existingId"
		))
	}}

	@Test def wiresCreate { new Fixture {
		val command = ModifySmallGroupSetCommand.create(module)

		command should be (anInstanceOf[Appliable[SmallGroupSet]])
		command should be (anInstanceOf[Describable[SmallGroupSet]])
		command should be (anInstanceOf[SelfValidating])
		command should be (anInstanceOf[CreateSmallGroupSetPermissions])
		command should be (anInstanceOf[CreateSmallGroupSetCommandState])
	}}

	@Test def wiresEdit { new ExistingSetFixture {
		val command = ModifySmallGroupSetCommand.edit(module, set)

		command should be (anInstanceOf[Appliable[SmallGroupSet]])
		command should be (anInstanceOf[Describable[SmallGroupSet]])
		command should be (anInstanceOf[SelfValidating])
		command should be (anInstanceOf[EditSmallGroupSetPermissions])
		command should be (anInstanceOf[EditSmallGroupSetCommandState])
	}}

}
