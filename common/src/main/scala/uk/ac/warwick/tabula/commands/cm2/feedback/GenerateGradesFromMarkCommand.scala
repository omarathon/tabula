package uk.ac.warwick.tabula.commands.cm2.feedback

import uk.ac.warwick.tabula.JavaImports._
import uk.ac.warwick.tabula.commands._
import uk.ac.warwick.tabula.data.model._
import uk.ac.warwick.tabula.helpers.StringUtils._
import uk.ac.warwick.tabula.permissions.Permissions
import uk.ac.warwick.tabula.services.{AssessmentMembershipServiceComponent, AutowiringAssessmentMembershipServiceComponent, GeneratesGradesFromMarks}
import uk.ac.warwick.tabula.system.permissions.{PermissionsChecking, PermissionsCheckingMethods, RequiresPermissionsChecking}
import uk.ac.warwick.userlookup.User

import scala.jdk.CollectionConverters._

object GenerateGradesFromMarkCommand {
  def apply(assessment: Assignment) =
    new GenerateGradesFromMarkCommandInternal(assessment)
      with AutowiringAssessmentMembershipServiceComponent
      with ComposableCommand[Map[String, Seq[GradeBoundary]]]
      with GenerateGradesFromMarkPermissions
      with GenerateGradesFromMarkCommandState
      with GenerateGradesFromMarkCommandRequest
      with ReadOnly with Unaudited
}

class GenerateGradesFromMarkCommandInternal(val assessment: Assignment)
  extends CommandInternal[Map[String, Seq[GradeBoundary]]] with GeneratesGradesFromMarks {

  self: GenerateGradesFromMarkCommandRequest with AssessmentMembershipServiceComponent =>

  lazy val assignmentUpstreamAssessmentGroupInfoMap: Map[AssessmentGroup, Option[UpstreamAssessmentGroupInfo]] = assessment.assessmentGroups.asScala.map { group =>
    group -> group.toUpstreamAssessmentGroupInfo(assessment.academicYear)
  }.toMap

  private def isNotNullAndInt(intString: String): Boolean = {
    if (intString == null) {
      false
    } else {
      try {
        intString.toInt
        true
      } catch {
        case _@(_: NumberFormatException | _: IllegalArgumentException) =>
          false
      }
    }
  }

  override def applyInternal(): Map[String, Seq[GradeBoundary]] = {
    //we are allowing PWD to upload to SITS as long as they are in the assessment components so find grades for them also (if present)
    val membership = assessmentMembershipService.determineMembershipUsersIncludingPWD(assessment)
    val studentMarksMap: Map[User, Int] = studentMarks.asScala
      .filter { case (_, mark) => isNotNullAndInt(mark) }
      .flatMap { case (uniID, mark) =>
        membership.find(_.getWarwickId == uniID).map(u => u -> mark.toInt)
      }.toMap

    val studentAssessmentComponentMap: Map[String, AssessmentComponent] = studentMarksMap.flatMap { case (student, _) =>
      assignmentUpstreamAssessmentGroupInfoMap.find { case (_, upstreamGroupInfo) =>
        upstreamGroupInfo.exists(_.upstreamAssessmentGroup.membersIncludes(student))
      }.map { case (group, _) => student.getWarwickId -> group.assessmentComponent }
    }

    studentMarks.asScala.map { case (uniId, mark) =>
      uniId -> studentAssessmentComponentMap.get(uniId).map(component => assessmentMembershipService.gradesForMark(component, mark.maybeText.map(_.toInt), assessment.resitAssessment)).getOrElse(Seq())
    }.toMap
  }

  override def applyForMarks(marks: Map[String, Int]): Map[String, Seq[GradeBoundary]] = {
    studentMarks = marks.view.mapValues(m => m.toString).toMap.asJava
    applyInternal()
  }

}

trait GenerateGradesFromMarkPermissions extends RequiresPermissionsChecking with PermissionsCheckingMethods {
  self: GenerateGradesFromMarkCommandState =>

  override def permissionsCheck(p: PermissionsChecking): Unit = {
    p.PermissionCheck(Permissions.MarkerFeedback.Manage, assessment)
  }
}

trait GenerateGradesFromMarkCommandState {
  def assessment: Assignment
}

trait GenerateGradesFromMarkCommandRequest {
  var studentMarks: JMap[String, String] = JHashMap()
  var selected: JMap[String, String] = JHashMap()
}
