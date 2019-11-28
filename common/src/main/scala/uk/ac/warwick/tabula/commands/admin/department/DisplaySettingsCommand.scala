package uk.ac.warwick.tabula.commands.admin.department

import org.springframework.validation.Errors
import uk.ac.warwick.tabula.JavaImports._
import uk.ac.warwick.tabula.commands._
import uk.ac.warwick.tabula.commands.admin.department.DisplaySettingsCommand._
import uk.ac.warwick.tabula.data.MitigatingCircumstancesSubmissionFilter
import uk.ac.warwick.tabula.data.Transactions._
import uk.ac.warwick.tabula.data.model.groups.SmallGroupAllocationMethod
import uk.ac.warwick.tabula.data.model.{CourseType, Department, MeetingRecordApprovalType, StudentRelationshipType}
import uk.ac.warwick.tabula.helpers.LazyMaps
import uk.ac.warwick.tabula.permissions._
import uk.ac.warwick.tabula.services.mitcircs.{AutowiringMitCircsSubmissionServiceComponent, MitCircsSubmissionServiceComponent}
import uk.ac.warwick.tabula.services.{AutowiringModuleAndDepartmentServiceComponent, AutowiringRelationshipServiceComponent, ModuleAndDepartmentServiceComponent, RelationshipServiceComponent}
import uk.ac.warwick.tabula.system.permissions.{PermissionsChecking, PermissionsCheckingMethods, RequiresPermissionsChecking}

import scala.jdk.CollectionConverters._

object DisplaySettingsCommand {
  type Result = Department
  type Command = Appliable[Result] with DisplaySettingsCommandState with DisplaySettingsCommandRequest with SelfValidating
  val RequiredPermission: Permission = Permissions.Department.ManageDisplaySettings

  def apply(department: Department): Command =
    new DisplaySettingsCommandInternal(department)
      with ComposableCommand[Result]
      with DisplaySettingsCommandRequest
      with AutowiringModuleAndDepartmentServiceComponent
      with AutowiringRelationshipServiceComponent
      with AutowiringMitCircsSubmissionServiceComponent
      with DisplaySettingsCommandDescription
      with DisplaySettingsCommandPermissions
      with DisplaySettingsCommandValidation
      with PopulateDisplaySettingsCommandRequest
}

trait DisplaySettingsCommandState {
  val department: Department
}

trait DisplaySettingsCommandRequest {
  self: DisplaySettingsCommandState =>

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
}

trait PopulateDisplaySettingsCommandRequest {
  self: DisplaySettingsCommandState
    with DisplaySettingsCommandRequest
    with RelationshipServiceComponent =>

  relationshipService.allStudentRelationshipTypes.foreach { relationshipType =>
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

class DisplaySettingsCommandInternal(val department: Department) extends CommandInternal[Result]
  with DisplaySettingsCommandState {

  self: DisplaySettingsCommandRequest
    with ModuleAndDepartmentServiceComponent =>

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

trait DisplaySettingsCommandPermissions extends RequiresPermissionsChecking with PermissionsCheckingMethods {
  self: DisplaySettingsCommandState =>
  def permissionsCheck(p: PermissionsChecking): Unit =
    p.PermissionCheck(RequiredPermission, mandatory(department))
}

trait DisplaySettingsCommandDescription extends Describable[Result] {
  this: DisplaySettingsCommandState =>
  // describe the thing that's happening.
  override def describe(d: Description): Unit =
    d.department(department)
}

trait DisplaySettingsCommandValidation extends SelfValidating {
  self: DisplaySettingsCommandState
    with DisplaySettingsCommandRequest
    with MitCircsSubmissionServiceComponent =>

  override def validate(errors: Errors): Unit = {
    // TAB-7797 Don't allow mitigating circumstances claims to be orphaned
    if (department.enableMitCircs && !enableMitCircs && mitCircsSubmissionService.submissionsForDepartment(department, Nil, MitigatingCircumstancesSubmissionFilter()).nonEmpty) {
      errors.rejectValue("enableMitCircs", "departmentSettings.enableMitCircs.wouldOrphan")
    }
  }
}
