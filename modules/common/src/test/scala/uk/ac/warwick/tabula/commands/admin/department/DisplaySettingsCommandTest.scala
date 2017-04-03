package uk.ac.warwick.tabula.commands.admin.department

import org.springframework.validation.BindException
import uk.ac.warwick.tabula.commands.{Appliable, Description}
import uk.ac.warwick.tabula.data.model.Assignment.Settings.InfoViewType._
import uk.ac.warwick.tabula.data.model.groups.SmallGroupAllocationMethod._
import uk.ac.warwick.tabula.data.model.groups.WeekRange
import uk.ac.warwick.tabula.data.model.groups.WeekRange.NumberingSystem._
import uk.ac.warwick.tabula.data.model.{Assignment, Department}
import uk.ac.warwick.tabula.permissions.Permissions
import uk.ac.warwick.tabula.services.{ModuleAndDepartmentService, ModuleAndDepartmentServiceComponent, RelationshipService, RelationshipServiceComponent}
import uk.ac.warwick.tabula.system.permissions.PermissionsChecking
import uk.ac.warwick.tabula.{Mockito, TestBase}

class OldDisplaySettingsCommandTest extends TestBase with Mockito {

	private trait Fixture {
		val testDepartment = new Department
		testDepartment.defaultGroupAllocationMethod = StudentSignUp
		testDepartment.showStudentName = true
		testDepartment.plagiarismDetectionEnabled = true
		testDepartment.assignmentInfoView = Summary
		testDepartment.weekNumberingSystem = Academic
		testDepartment.autoGroupDeregistration = true

		val commandInternal = new OldDisplaySettingsCommandInternal(testDepartment) with ModuleAndDepartmentServiceComponent with RelationshipServiceComponent {
			var moduleAndDepartmentService: ModuleAndDepartmentService = mock[ModuleAndDepartmentService]
			var relationshipService: RelationshipService = mock[RelationshipService]
		}

	}

	@Test
	def objectApplyCreatesCommand() {
		new Fixture {
			val command = OldDisplaySettingsCommand(testDepartment)

			command.isInstanceOf[Appliable[Department]] should be(true)
			command.isInstanceOf[OldDisplaySettingsCommandState] should be(true)
			command.asInstanceOf[OldDisplaySettingsCommandState].department should be(testDepartment)
		}
	}

	@Test
	def commandSetsStateFromDepartmentWhenConstructing() {
		new Fixture {

			commandInternal.defaultGroupAllocationMethod should be(StudentSignUp.dbValue)
			commandInternal.showStudentName should be(true)
			commandInternal.plagiarismDetection should be(true)
			commandInternal.assignmentInfoView should be(Summary)
			commandInternal.weekNumberingSystem should be(Academic)
			commandInternal.autoGroupDeregistration should be (true)
		}
	}

	@Test
	def commandUpdatesDepartmentWhenApplied() {
		new Fixture {

			commandInternal.defaultGroupAllocationMethod = Manual.dbValue
			commandInternal.showStudentName = false
			commandInternal.plagiarismDetection = false
			commandInternal.turnitinExcludeBibliography = false
			commandInternal.turnitinExcludeQuotations = false
			commandInternal.turnitinSmallMatchPercentageLimit = 5
			commandInternal.turnitinSmallMatchWordLimit = 0
			commandInternal.assignmentInfoView = Table
			commandInternal.weekNumberingSystem = Term
			commandInternal.autoGroupDeregistration = false
			commandInternal.autoMarkMissedMonitoringPoints = true

			commandInternal.applyInternal()

			testDepartment.defaultGroupAllocationMethod should be(Manual)
			testDepartment.showStudentName should be(false)
			testDepartment.plagiarismDetectionEnabled should be(false)
			testDepartment.turnitinExcludeBibliography should be (false)
			testDepartment.turnitinExcludeQuotations should be (false)
			testDepartment.turnitinSmallMatchPercentageLimit should be (5)
			testDepartment.turnitinSmallMatchWordLimit should be (0)
			testDepartment.assignmentInfoView should be(Table)
			testDepartment.weekNumberingSystem should be(Term)
			testDepartment.autoGroupDeregistration should be (false)
			testDepartment.autoMarkMissedMonitoringPoints should be (true)

		}
	}

	@Test
	def commandApplyInvokesSaveOnDepartmentService() {
		new Fixture {

			commandInternal.applyInternal()
			verify(commandInternal.moduleAndDepartmentService, times(1)).saveOrUpdate(testDepartment)
		}
	}

	@Test
	def commandDescriptionDescribedDepartment() {
		new Fixture {
			val describable = new OldDisplaySettingsCommandDescription with OldDisplaySettingsCommandState {
				val eventName: String = "test"
				val department: Department = testDepartment
			}

			val description: Description = mock[Description]
			describable.describe(description)
			verify(description, times(1)).department(testDepartment)
		}
	}

	@Test
	def permissionsRequireManageDisplaySettingsOnDepartment {
		new Fixture {
			val perms = new OldDisplaySettingsCommandPermissions() with OldDisplaySettingsCommandState{
				val department: Department = testDepartment
			}
			val checking: PermissionsChecking = mock[PermissionsChecking]
			perms.permissionsCheck(checking)
			verify(checking, times(1)).PermissionCheck(Permissions.Department.ManageDisplaySettings, testDepartment)
		}
	}

	@Test
	def percentageValidation {
		new Fixture {
			commandInternal.turnitinSmallMatchPercentageLimit = 101

			var errors = new BindException(commandInternal, "command")
			commandInternal.validate(errors)
			errors.hasErrors should be (true)

			commandInternal.turnitinSmallMatchPercentageLimit = -5
			errors = new BindException(commandInternal, "command")
			commandInternal.validate(errors)
			errors.hasErrors should be (true)

			commandInternal.turnitinSmallMatchPercentageLimit = 5
			errors = new BindException(commandInternal, "command")
			commandInternal.validate(errors)
			errors.hasErrors should be (false)
		}
	}

	@Test
	def wordLimitValidation {
		new Fixture {
			commandInternal.turnitinSmallMatchPercentageLimit = -1

			var errors = new BindException(commandInternal, "command")
			commandInternal.validate(errors)
			errors.hasErrors should be (true)

			commandInternal.turnitinSmallMatchPercentageLimit = 25
			errors = new BindException(commandInternal, "command")
			commandInternal.validate(errors)
			errors.hasErrors should be (false)
		}
	}

	@Test
	def smallMatchSingleValidation {
		new Fixture {
			commandInternal.turnitinSmallMatchPercentageLimit = 5
			commandInternal.turnitinSmallMatchWordLimit = 5
			var errors = new BindException(commandInternal, "command")
			commandInternal.validate(errors)
			errors.hasErrors should be (true)

			commandInternal.turnitinSmallMatchPercentageLimit = 5
			commandInternal.turnitinSmallMatchWordLimit = 0
			errors = new BindException(commandInternal, "command")
			commandInternal.validate(errors)
			errors.hasErrors should be (false)
		}
	}

}
