package uk.ac.warwick.tabula.api.commands.exams

import uk.ac.warwick.tabula.JavaImports.{JList, _}
import uk.ac.warwick.tabula.api.commands.exams.AssessmentGroupAssignmentsCommand._
import uk.ac.warwick.tabula.commands._
import uk.ac.warwick.tabula.data.Transactions._
import uk.ac.warwick.tabula.data.model.{Assignment, Department, UpstreamAssessmentGroupKey}
import uk.ac.warwick.tabula.helpers.LazyLists
import uk.ac.warwick.tabula.permissions.{Permission, Permissions}
import uk.ac.warwick.tabula.services.{AssessmentMembershipServiceComponent, AutowiringAssessmentMembershipServiceComponent}
import uk.ac.warwick.tabula.system.permissions.{PermissionsChecking, PermissionsCheckingMethods, RequiresPermissionsChecking}

import scala.jdk.CollectionConverters._

object AssessmentGroupAssignmentsCommand {

  type Result = Map[UpstreamAssessmentGroupKey, Seq[Assignment]]
  type Command = Appliable[Result] with AssessmentGroupAssignmentsState with AssessmentGroupAssignmentsRequest
  val RequiredPermission: Permission = Permissions.Exam.Read

  def apply(department: Department) = new AssessmentGroupAssignmentsCommandInternal(department)
    with ComposableCommand[Result]
    with AssessmentGroupAssignmentsRequest
    with AssessmentGroupAssignmentsPermissions
    with AutowiringAssessmentMembershipServiceComponent
    with Unaudited with ReadOnly
}


class AssessmentGroupAssignmentsCommandInternal(val department: Department) extends CommandInternal[Result]
  with AssessmentGroupAssignmentsState {

  self: AssessmentGroupAssignmentsRequest with AssessmentMembershipServiceComponent =>

  def applyInternal(): Result = transactional() {
    assessmentMembershipService.getAssignmentsForAssessmentGroups(assessmentGroups.asScala.toSeq)
  }
}

trait AssessmentGroupAssignmentsPermissions extends RequiresPermissionsChecking with PermissionsCheckingMethods {
  self: AssessmentGroupAssignmentsState =>

  def permissionsCheck(p: PermissionsChecking): Unit = {
    p.PermissionCheck(RequiredPermission, department)
  }
}

trait AssessmentGroupAssignmentsState {
  def department: Department
}

trait AssessmentGroupAssignmentsRequest {

  var assessmentGroups: JList[UpstreamAssessmentGroupKey] = LazyLists.create[UpstreamAssessmentGroupKey]()
}