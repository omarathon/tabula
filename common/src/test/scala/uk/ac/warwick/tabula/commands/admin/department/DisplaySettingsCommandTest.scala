package uk.ac.warwick.tabula.commands.admin.department

import uk.ac.warwick.tabula.commands.{Appliable, Description}
import uk.ac.warwick.tabula.data.model.Assignment.Settings.InfoViewType._
import uk.ac.warwick.tabula.data.model.Department
import uk.ac.warwick.tabula.data.model.groups.SmallGroupAllocationMethod._
import uk.ac.warwick.tabula.data.model.groups.WeekRange.NumberingSystem._
import uk.ac.warwick.tabula.permissions.Permissions
import uk.ac.warwick.tabula.services.{ModuleAndDepartmentService, ModuleAndDepartmentServiceComponent, RelationshipService, RelationshipServiceComponent}
import uk.ac.warwick.tabula.system.permissions.PermissionsChecking
import uk.ac.warwick.tabula.{Mockito, TestBase}

class DisplaySettingsCommandTest extends TestBase with Mockito {

  private trait Fixture {
    val testDepartment = new Department
    testDepartment.defaultGroupAllocationMethod = StudentSignUp
    testDepartment.showStudentName = true
    testDepartment.plagiarismDetectionEnabled = true
    testDepartment.assignmentInfoView = Summary
    testDepartment.weekNumberingSystem = Academic
    testDepartment.autoGroupDeregistration = true

    val commandInternal = new DisplaySettingsCommandInternal(testDepartment) with DisplaySettingsCommandRequest with PopulateDisplaySettingsCommandRequest with ModuleAndDepartmentServiceComponent with RelationshipServiceComponent {
      var moduleAndDepartmentService: ModuleAndDepartmentService = mock[ModuleAndDepartmentService]
      var relationshipService: RelationshipService = mock[RelationshipService]
    }

  }

  @Test
  def objectApplyCreatesCommand() {
    new Fixture {
      val command = DisplaySettingsCommand(testDepartment)

      command.isInstanceOf[Appliable[Department]] should be(true)
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
      commandInternal.autoGroupDeregistration should be(true)
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
      commandInternal.autoGroupDeregistration = false
      commandInternal.autoMarkMissedMonitoringPoints = true

      commandInternal.applyInternal()

      testDepartment.defaultGroupAllocationMethod should be(Manual)
      testDepartment.showStudentName should be(false)
      testDepartment.plagiarismDetectionEnabled should be(false)
      testDepartment.assignmentInfoView should be(Table)
      testDepartment.weekNumberingSystem should be(Term)
      testDepartment.autoGroupDeregistration should be(false)
      testDepartment.autoMarkMissedMonitoringPoints should be(true)

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
  def commandDescriptionDescribedDepartment(): Unit = {
    new Fixture {
      val describable = new DisplaySettingsCommandDescription with DisplaySettingsCommandState {
        val eventName: String = "test"
        val department: Department = testDepartment
      }

      val description: Description = mock[Description]
      describable.describe(description)
      verify(description, times(1)).department(testDepartment)
    }
  }

  @Test
  def permissionsRequireManageDisplaySettingsOnDepartment(): Unit = {
    new Fixture {
      val perms = new DisplaySettingsCommandPermissions() with DisplaySettingsCommandState {
        val department: Department = testDepartment
      }
      val checking: PermissionsChecking = mock[PermissionsChecking]
      perms.permissionsCheck(checking)
      verify(checking, times(1)).PermissionCheck(Permissions.Department.ManageDisplaySettings, testDepartment)
    }
  }

}
