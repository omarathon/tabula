package uk.ac.warwick.tabula.commands.marks

import org.springframework.validation.Errors
import uk.ac.warwick.tabula.CurrentUser
import uk.ac.warwick.tabula.JavaImports._
import uk.ac.warwick.tabula.commands._
import uk.ac.warwick.tabula.commands.marks.RecordAssessmentComponentMarksCommand._
import uk.ac.warwick.tabula.data.model.{AssessmentComponent, RecordedAssessmentComponentStudent, UpstreamAssessmentGroup}
import uk.ac.warwick.tabula.data.{AutowiringTransactionalComponent, TransactionalComponent}
import uk.ac.warwick.tabula.helpers.LazyMaps
import uk.ac.warwick.tabula.helpers.StringUtils._
import uk.ac.warwick.tabula.permissions.{Permission, Permissions}
import uk.ac.warwick.tabula.services.marks.{AssessmentComponentMarksServiceComponent, AutowiringAssessmentComponentMarksServiceComponent}
import uk.ac.warwick.tabula.services.{AssessmentMembershipServiceComponent, AutowiringAssessmentMembershipServiceComponent}
import uk.ac.warwick.tabula.system.permissions.{PermissionsChecking, PermissionsCheckingMethods, RequiresPermissionsChecking}

import scala.jdk.CollectionConverters._

object RecordAssessmentComponentMarksCommand {
  type UniversityID = String
  class StudentMarksItem(val universityID: UniversityID) {
    var mark: String = _ // Easier as a String to treat empty strings correctly
    var grade: String = _
    var comments: String = _
  }

  type Result = Seq[RecordedAssessmentComponentStudent]
  type Command = Appliable[Result]
    with RecordAssessmentComponentMarksRequest
    with SelfValidating

  val AdminPermission: Permission = Permissions.Feedback.Publish

  def apply(assessmentComponent: AssessmentComponent, upstreamAssessmentGroup: UpstreamAssessmentGroup, currentUser: CurrentUser): Command =
    new RecordAssessmentComponentMarksCommandInternal(assessmentComponent, upstreamAssessmentGroup, currentUser)
      with ComposableCommand[Result]
      with RecordAssessmentComponentMarksRequest
      with RecordAssessmentComponentMarksValidation
      with RecordAssessmentComponentMarksPermissions
      with RecordAssessmentComponentMarksDescription
      with AutowiringAssessmentComponentMarksServiceComponent
      with AutowiringAssessmentMembershipServiceComponent
      with AutowiringTransactionalComponent
}

abstract class RecordAssessmentComponentMarksCommandInternal(val assessmentComponent: AssessmentComponent, val upstreamAssessmentGroup: UpstreamAssessmentGroup, currentUser: CurrentUser)
  extends CommandInternal[Result]
    with RecordAssessmentComponentMarksState {
  self: RecordAssessmentComponentMarksRequest
    with AssessmentComponentMarksServiceComponent
    with TransactionalComponent =>

  override def applyInternal(): Result = transactional() {
    students.asScala.values.toSeq
      .filter(_.mark.nonEmpty)
      .map { item =>
        val upstreamAssessmentGroupMember =
          upstreamAssessmentGroup.members.asScala
            .find(_.universityId == item.universityID)
            .get // We validate that this exists

        val recordedAssessmentComponentStudent: RecordedAssessmentComponentStudent =
          assessmentComponentMarksService.getOrCreateRecordedStudent(upstreamAssessmentGroupMember)

        recordedAssessmentComponentStudent.addMark(
          uploaderId = currentUser.userId,
          mark = item.mark.toInt,
          grade = item.grade.maybeText,
          comments = item.comments
        )

        assessmentComponentMarksService.saveOrUpdate(recordedAssessmentComponentStudent)

        recordedAssessmentComponentStudent
      }
  }
}

trait RecordAssessmentComponentMarksState {
  def assessmentComponent: AssessmentComponent
  def upstreamAssessmentGroup: UpstreamAssessmentGroup
}

trait RecordAssessmentComponentMarksRequest {
  var students: JMap[UniversityID, StudentMarksItem] =
    LazyMaps.create { id: String => new StudentMarksItem(id) }
      .asJava
}

trait RecordAssessmentComponentMarksValidation extends SelfValidating {
  self: RecordAssessmentComponentMarksState
    with RecordAssessmentComponentMarksRequest
    with AssessmentMembershipServiceComponent =>

  override def validate(errors: Errors): Unit = {
    val doGradeValidation = assessmentComponent.module.adminDepartment.assignmentGradeValidation
    students.asScala.foreach { case (universityID, item) =>
      errors.pushNestedPath(s"students[$universityID]")

      // We allow returning marks for PWD students so we don't need to filter by "current" members here
      if (!upstreamAssessmentGroup.members.asScala.exists(_.universityId == universityID)) {
        errors.rejectValue("", "id.wrong.marker")
      }

      if (item.mark.hasText) {
        try {
          val asInt = item.mark.toInt
          if (asInt < 0 || asInt > 100) {
            errors.rejectValue("mark", "actualMark.range")
          } else if (doGradeValidation && item.grade.hasText) {
            val validGrades = assessmentMembershipService.gradesForMark(assessmentComponent, asInt)
            if (validGrades.nonEmpty && !validGrades.exists(_.grade == item.grade)) {
              errors.rejectValue("grade", "actualGrade.invalidSITS", Array(validGrades.map(_.grade).mkString(", ")), "")
            }
          }
        } catch {
          case _ @ (_: NumberFormatException | _: IllegalArgumentException) =>
            errors.rejectValue("mark", "actualMark.format")
        }
      } else if (doGradeValidation && item.grade.hasText) {
        errors.rejectValue("mark", "actualMark.validateGrade.adjustedGrade")
      }

      errors.popNestedPath()
    }
  }
}

trait RecordAssessmentComponentMarksPermissions extends RequiresPermissionsChecking with PermissionsCheckingMethods {
  self: RecordAssessmentComponentMarksState =>

  override def permissionsCheck(p: PermissionsChecking): Unit = {
    mustBeLinked(upstreamAssessmentGroup, assessmentComponent)
    p.PermissionCheck(AdminPermission, mandatory(assessmentComponent.module))
  }
}

trait RecordAssessmentComponentMarksDescription extends Describable[Result] {
  self: RecordAssessmentComponentMarksState =>

  override lazy val eventName: String = "RecordAssessmentComponentMarks"

  override def describe(d: Description): Unit =
    d.assessmentComponent(assessmentComponent)
     .upstreamAssessmentGroup(upstreamAssessmentGroup)

  override def describeResult(d: Description, result: Result): Unit =
    d.property(
      "marks" -> result.map { student =>
        student.universityId -> student.latestMark.get
      }.toMap
    )
}
