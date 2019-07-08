package uk.ac.warwick.tabula.commands.admin.department

import uk.ac.warwick.tabula.JavaImports._
import uk.ac.warwick.tabula.commands._
import uk.ac.warwick.tabula.data.Transactions._
import uk.ac.warwick.tabula.data.model.groups.SmallGroupAllocationMethod
import uk.ac.warwick.tabula.data.model.{CourseType, Department, MeetingRecordApprovalType, StudentRelationshipType}
import uk.ac.warwick.tabula.helpers.LazyMaps
import uk.ac.warwick.tabula.permissions._
import uk.ac.warwick.tabula.services.{AutowiringModuleAndDepartmentServiceComponent, AutowiringRelationshipServiceComponent, ModuleAndDepartmentServiceComponent, RelationshipServiceComponent}
import uk.ac.warwick.tabula.system.permissions.{PermissionsChecking, RequiresPermissionsChecking}

import scala.collection.JavaConverters._

object DisplaySettingsCommand {
  def apply(department: Department) =
    new DisplaySettingsCommandInternal(department)
      with ComposableCommand[Department]
      with AutowiringModuleAndDepartmentServiceComponent
      with AutowiringRelationshipServiceComponent
      with DisplaySettingsCommandDescription
      with DisplaySettingsCommandPermissions
}

trait DisplaySettingsCommandState {
  val department: Department
}

class DisplaySettingsCommandInternal(val department: Department) extends CommandInternal[Department] with PopulateOnForm
  with DisplaySettingsCommandState {

  this: ModuleAndDepartmentServiceComponent with RelationshipServiceComponent =>

  var showStudentName: Boolean = department.showStudentName
  var plagiarismDetection: Boolean = department.plagiarismDetectionEnabled
  var assignmentGradeValidation: Boolean = department.assignmentGradeValidation
  var assignmentGradeValidationUseDefaultForZero: Boolean = department.assignmentGradeValidationUseDefaultForZero
  var assignmentInfoView: String = department.assignmentInfoView
  var weekNumberingSystem: String = department.weekNumberingSystem
  var autoGroupDeregistration: Boolean = department.autoGroupDeregistration
  var studentsCanScheduleMeetings: Boolean = department.studentsCanScheduleMeetings
  var defaultGroupAllocationMethod: String = department.defaultGroupAllocationMethod.dbValue
  var studentRelationshipDisplayed: JMap[String, JBoolean] =
    JHashMap(department.studentRelationshipDisplayed.map {
      case (id, bString) => id -> java.lang.Boolean.valueOf(bString)
    })
  var studentRelationshipExpected: JMap[StudentRelationshipType, JMap[CourseType, JBoolean]] =
    LazyMaps.create { _: StudentRelationshipType => JMap[CourseType, JBoolean]() }.asJava
  var autoMarkMissedMonitoringPoints: Boolean = department.autoMarkMissedMonitoringPoints
  var meetingRecordApprovalType: MeetingRecordApprovalType = department.meetingRecordApprovalType
  var enableMitCircs: Boolean = department.enableMitCircs
  var mitCircsGuidance: String = department.mitCircsGuidance

  def populate() {
    relationshipService.allStudentRelationshipTypes.foreach { relationshipType => {
      if (!studentRelationshipDisplayed.containsKey(relationshipType.id))
        studentRelationshipDisplayed.put(relationshipType.id, relationshipType.defaultDisplay)

      studentRelationshipExpected.put(relationshipType, JHashMap(
        Seq(CourseType.UG, CourseType.PGT, CourseType.PGR, CourseType.Foundation, CourseType.PreSessional).map(courseType =>
          courseType -> JBoolean(Option(department.getStudentRelationshipExpected(relationshipType, courseType)
            .getOrElse(relationshipType.isDefaultExpected(courseType))))
        ): _*
      ))
    }
    }
  }

  override def applyInternal(): Department = transactional() {
    department.showStudentName = showStudentName
    department.plagiarismDetectionEnabled = plagiarismDetection
    department.assignmentGradeValidation = assignmentGradeValidation
    department.assignmentGradeValidationUseDefaultForZero = assignmentGradeValidationUseDefaultForZero
    department.assignmentInfoView = assignmentInfoView
    department.autoGroupDeregistration = autoGroupDeregistration
    department.studentsCanScheduleMeetings = studentsCanScheduleMeetings
    department.defaultGroupAllocationMethod = SmallGroupAllocationMethod(defaultGroupAllocationMethod)
    department.weekNumberingSystem = weekNumberingSystem
    department.studentRelationshipDisplayed = studentRelationshipDisplayed.asScala.map {
      case (id, bool) => id -> Option(bool).getOrElse(false).toString
    }.toMap
    studentRelationshipExpected.asScala.foreach { case (relationshipType, courseTypeMap) =>
      courseTypeMap.asScala.foreach { case (courseType, isExpected) =>
        department.setStudentRelationshipExpected(relationshipType, courseType, Option(isExpected).exists(_.booleanValue))
      }
    }
    department.autoMarkMissedMonitoringPoints = autoMarkMissedMonitoringPoints
    department.meetingRecordApprovalType = meetingRecordApprovalType
    department.enableMitCircs = enableMitCircs
    department.mitCircsGuidance = mitCircsGuidance

    moduleAndDepartmentService.saveOrUpdate(department)
    department
  }
}

trait DisplaySettingsCommandPermissions extends RequiresPermissionsChecking {
  this: DisplaySettingsCommandState =>
  def permissionsCheck(p: PermissionsChecking) {
    p.PermissionCheck(Permissions.Department.ManageDisplaySettings, department)
  }
}

trait DisplaySettingsCommandDescription extends Describable[Department] {
  this: DisplaySettingsCommandState =>
  // describe the thing that's happening.
  override def describe(d: Description): Unit =
    d.department(department)
}

