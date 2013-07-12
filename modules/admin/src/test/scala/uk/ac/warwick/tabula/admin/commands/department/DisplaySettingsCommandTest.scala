package uk.ac.warwick.tabula.admin.commands.department

import uk.ac.warwick.tabula.{Mockito, TestBase}
import uk.ac.warwick.tabula.data.model.{Assignment, Department}
import uk.ac.warwick.tabula.commands.{Description, Appliable}
import uk.ac.warwick.tabula.services.{ModuleAndDepartmentServiceComponent, ModuleAndDepartmentService}
import uk.ac.warwick.tabula.data.model.groups.SmallGroupAllocationMethod._
import Assignment.Settings.InfoViewType._
import uk.ac.warwick.tabula.data.model.groups.WeekRange
import WeekRange.NumberingSystem._
import uk.ac.warwick.tabula.system.permissions.PermissionsChecking
import uk.ac.warwick.tabula.permissions.Permissions

class DisplaySettingsCommandTest extends TestBase with Mockito {

	private trait Fixture {
		val testDepartment = new Department
		testDepartment.defaultGroupAllocationMethod = StudentSignUp
		testDepartment.showStudentName = true
		testDepartment.plagiarismDetectionEnabled = true
		testDepartment.assignmentInfoView = Summary
		testDepartment.weekNumberingSystem = Academic
		
		val commandInternal = new DisplaySettingsCommandInternal(testDepartment) with ModuleAndDepartmentServiceComponent {
			var moduleAndDepartmentService = mock[ModuleAndDepartmentService]
		}

	}

	@Test
	def objectApplyCreatesCommand() {
		new Fixture {
			val command = DisplaySettingsCommand(testDepartment)

			command.isInstanceOf[Appliable[Unit]] should be(true)
			command.isInstanceOf[DisplaySettingsCommandState] should be(true)
			command.asInstanceOf[DisplaySettingsCommandState].department should be(testDepartment)
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
		}
	}

	@Test
	def commandUpdatesDepartmentWhenApplied() {
		new Fixture {

			commandInternal.defaultGroupAllocationMethod = Manual.dbValue
			commandInternal.showStudentName = false
			commandInternal.plagiarismDetection = false
			commandInternal.assignmentInfoView = Table
			commandInternal.weekNumberingSystem = Term

			commandInternal.applyInternal()

			testDepartment.defaultGroupAllocationMethod should be(Manual)
			testDepartment.showStudentName should be(false)
			testDepartment.plagiarismDetectionEnabled should be(false)
			testDepartment.assignmentInfoView should be(Table)
			testDepartment.weekNumberingSystem should be(Term)

		}
	}

	@Test
	def commandApplyInvokesSaveOnDepartmentService() {
		new Fixture {

			commandInternal.applyInternal()
			there was one(commandInternal.moduleAndDepartmentService).save(testDepartment)
		}
	}

	@Test
	def commandDescriptionDescribedDepartment() {
		new Fixture {
			val describable = new DisplaySettingsCommandDescription with DisplaySettingsCommandState {
				val eventName: String = "test"
				val department: Department = testDepartment
			}

			val description = mock[Description]
			describable.describe(description)
			there was one(description).department(testDepartment)
		}
	}

	@Test
	def permissionsRequireManageDisplaySettingsOnDepartment {
		new Fixture {
			val perms = new DisplaySettingsCommandPermissions() with DisplaySettingsCommandState{
				val department: Department = testDepartment
			}
			val checking = mock[PermissionsChecking]
			perms.permissionsCheck(checking)
			there was one(checking).PermissionCheck(Permissions.Department.ManageDisplaySettings, testDepartment)
		}



	}

}
