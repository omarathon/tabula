package uk.ac.warwick.tabula.groups.commands.admin

import org.joda.time.{DateTimeConstants, DateTime}
import org.springframework.validation.BindException
import uk.ac.warwick.tabula.commands._
import uk.ac.warwick.tabula.data.model.{Department, AssessmentGroup, Module}
import uk.ac.warwick.tabula.data.model.groups.SmallGroupSet
import uk.ac.warwick.tabula.permissions.Permissions
import uk.ac.warwick.tabula.services.{SmallGroupService, SmallGroupServiceComponent}
import uk.ac.warwick.tabula._
import uk.ac.warwick.tabula.JavaImports._
import uk.ac.warwick.tabula.system.permissions.PermissionsChecking

class CopySmallGroupSetsCommandTest extends TestBase with Mockito {

	private trait CommandTestSupport extends SmallGroupServiceComponent with PopulateCopySmallGroupSetsRequestDefaults {
		self: CopySmallGroupSetsRequestState with CopySmallGroupSetsCommandState =>

		val smallGroupService = smartMock[SmallGroupService]
	}

	private trait Fixture {
		val department = Fixtures.department("in", "IT Services")
		department.id = "departmentId"

		val module = Fixtures.module("in101", "Introduction to Scala")
		module.id = "moduleId"
		module.adminDepartment = department
	}

	private trait CommandFixture extends Fixture {
		val command = new CopySmallGroupSetsCommandInternal(department, Seq(module)) with CommandTestSupport
	}

	@Test def populate(): Unit = withFakeTime(new DateTime(2014, DateTimeConstants.OCTOBER, 13, 9, 13, 29, 0)) { new CommandFixture {
		val source = new SmallGroupSet()

		command.smallGroupService.getSmallGroupSets(module, new AcademicYear(2013)) returns (Seq(source))

		command.populate()

		command.sourceAcademicYear should be (new AcademicYear(2013))
		command.targetAcademicYear should be (new AcademicYear(2014))
		command.smallGroupSets.size() should be (1)
		command.smallGroupSets.get(0).smallGroupSet should be (source)
	}}

	@Test def apply(): Unit = new CommandFixture {
		val copy = new SmallGroupSet()
		val source = new SmallGroupSet() {
			override def duplicateTo(module: Module, assessmentGroups: JavaImports.JList[AssessmentGroup], year: AcademicYear, copyGroups: Boolean, copyEvents: Boolean, copyMembership: Boolean): SmallGroupSet =
				copy
		}

		command.smallGroupService.getSmallGroupSets(module, command.sourceAcademicYear) returns (Seq(source))

		command.populate()

		command.smallGroupSets.get(0).copy = true

		// The actual duplicateTo code is tested elsewhere
		command.applyInternal() should be (Seq(copy))

		verify(command.smallGroupService, times(1)).saveOrUpdate(copy)
	}

	@Test def permissions(): Unit = new Fixture {
		val (d, m) = (department, Seq(module))

		val command = new CopySmallGroupSetsPermissions with CopySmallGroupSetsCommandState {
			val department = d
			val modules = m
		}

		val checking = mock[PermissionsChecking]
		command.permissionsCheck(checking)

		verify(checking, times(1)).PermissionCheck(Permissions.SmallGroups.Read, module)
		verify(checking, times(1)).PermissionCheck(Permissions.SmallGroups.Create, module)
	}

	@Test(expected = classOf[ItemNotFoundException]) def permissionsNoDepartment(): Unit = {
		val command = new CopySmallGroupSetsPermissions with CopySmallGroupSetsCommandState {
			val modules = Nil
			val department = null
		}

		val checking = mock[PermissionsChecking]
		command.permissionsCheck(checking)
	}

	@Test(expected = classOf[ItemNotFoundException]) def permissionsUnlinkedModule(): Unit = {
		val command = new CopySmallGroupSetsPermissions with CopySmallGroupSetsCommandState {
			val department = Fixtures.department("in")
			department.id = "d id"

			val modules = Seq(new Module(code = "in101", adminDepartment = Fixtures.department("other")))
		}

		val checking = mock[PermissionsChecking]
		command.permissionsCheck(checking)
	}

	private trait ValidationFixture extends Fixture {
		val command = new CopySmallGroupSetsValidation with CopySmallGroupSetsCommandState with CopySmallGroupSetsRequestState {
			val department = ValidationFixture.this.department
			val modules = Seq(ValidationFixture.this.module)

			// Populate some default values
			targetAcademicYear = new AcademicYear(2014)
			sourceAcademicYear = new AcademicYear(2013)
			smallGroupSets = JArrayList()
		}
	}

	@Test def validationPasses(): Unit = new ValidationFixture {
		val errors = new BindException(command, "command")
		command.validate(errors)

		errors.hasErrors should be (false)
	}

	@Test def validationNoSourceYear(): Unit = new ValidationFixture {
		command.sourceAcademicYear = null

		val errors = new BindException(command, "command")
		command.validate(errors)

		errors.hasErrors should be (true)
		errors.getErrorCount should be (1)
		errors.getFieldError.getField should be ("sourceAcademicYear")
		errors.getFieldError.getCodes should contain ("NotEmpty")
	}

	@Test def validationNoTargetYear(): Unit = new ValidationFixture {
		command.targetAcademicYear = null

		val errors = new BindException(command, "command")
		command.validate(errors)

		errors.hasErrors should be (true)
		errors.getErrorCount should be (1)
		errors.getFieldError.getField should be ("targetAcademicYear")
		errors.getFieldError.getCodes should contain ("NotEmpty")
	}

	@Test def validationTargetEqualsSourceYear(): Unit = new ValidationFixture {
		command.targetAcademicYear = command.sourceAcademicYear

		val errors = new BindException(command, "command")
		command.validate(errors)

		errors.hasErrors should be (true)
		errors.getErrorCount should be (1)
		errors.getFieldError.getField should be ("targetAcademicYear")
		errors.getFieldError.getCodes should contain ("smallGroupSet.copy.sameYear")
	}

	@Test def describe(): Unit = new Fixture {
		val (d, m) = (department, Seq(module))
		val set = new SmallGroupSet {
			id = "smallGroupSetId"
		}

		val command = new CopySmallGroupSetsDescription with CopySmallGroupSetsCommandState with CopySmallGroupSetsRequestState {
			override val eventName = "test"
			val department = d
			val modules = m

			// Populate some default values
			targetAcademicYear = new AcademicYear(2014)
			sourceAcademicYear = new AcademicYear(2013)
			smallGroupSets = JArrayList(new CopySmallGroupSetState(set) { copy = true })
		}

		val description = new DescriptionImpl
		command.describe(description)

		description.allProperties should be (Map(
			"department" -> "in",
			"modules" -> Seq("moduleId"),
			"smallGroupSets" -> Seq("smallGroupSetId")
		))
	}

	@Test def wires(): Unit = new Fixture {
		val command = CopySmallGroupSetsCommand(department, Seq(module))

		command should be (anInstanceOf[Appliable[Seq[SmallGroupSet]]])
		command should be (anInstanceOf[Describable[Seq[SmallGroupSet]]])
		command should be (anInstanceOf[CopySmallGroupSetsPermissions])
		command should be (anInstanceOf[CopySmallGroupSetsCommandState])
		command should be (anInstanceOf[CopySmallGroupSetsRequestState])
		command should be (anInstanceOf[SelfValidating])
		command should be (anInstanceOf[PopulateOnForm])
	}

}