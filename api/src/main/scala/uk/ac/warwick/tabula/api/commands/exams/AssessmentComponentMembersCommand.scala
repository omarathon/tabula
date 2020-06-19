package uk.ac.warwick.tabula.api.commands.exams

import uk.ac.warwick.tabula.AcademicYear
import uk.ac.warwick.tabula.JavaImports.{JList, _}
import uk.ac.warwick.tabula.api.commands.exams.AssessmentComponentMembersCommand._
import uk.ac.warwick.tabula.commands._
import uk.ac.warwick.tabula.data.Transactions._
import uk.ac.warwick.tabula.data.model.{AssessmentComponentKey, Department, UpstreamAssessmentGroupInfo}
import uk.ac.warwick.tabula.permissions.{Permission, Permissions}
import uk.ac.warwick.tabula.services.{AssessmentMembershipServiceComponent, AutowiringAssessmentMembershipServiceComponent}
import uk.ac.warwick.tabula.system.permissions.{PermissionsChecking, PermissionsCheckingMethods, RequiresPermissionsChecking}

import scala.jdk.CollectionConverters._

case class AssessmentComponentMembersResult (
  byPaperCode: Map[String, Seq[UpstreamAssessmentGroupInfo]],
  byAssessmentComponent: Map[String, Seq[UpstreamAssessmentGroupInfo]],
)

object AssessmentComponentMembersCommand {

  type Result = AssessmentComponentMembersResult
  type Command = Appliable[Result] with AssessmentComponentMembersRequest
  val RequiredPermission: Permission = Permissions.Exam.Read

  def apply(department: Department, academicYear: AcademicYear): Command = new AssessmentComponentMembersCommandInternal(department, academicYear)
    with ComposableCommand[Result]
    with AssessmentComponentMembersRequest
    with AssessmentComponentMembersPermissions
    with AutowiringAssessmentMembershipServiceComponent
    with Unaudited
}

class AssessmentComponentMembersCommandInternal(val department: Department, val academicYear: AcademicYear) extends CommandInternal[Result]
  with AssessmentComponentMembersState {

  self: AssessmentComponentMembersRequest with AssessmentMembershipServiceComponent =>

  def applyInternal(): Result = transactional() {
    val paperCodeComponents = assessmentMembershipService
      .getAssessmentComponentsByPaperCode(department, paperCodes.asScala.toSeq)

    val components = assessmentMembershipService.getAssessmentComponents(department, assessmentComponents.asScala.toSeq)

    val memberLookup = assessmentMembershipService.getUpstreamAssessmentGroupMembers(components ++ paperCodeComponents.values.flatten.toSeq, academicYear)

    val paperCodeMembers = paperCodeComponents.view.mapValues(_.flatMap(c => memberLookup.getOrElse(AssessmentComponentKey(c), Nil))).toMap
    val componentMembers = components.map(c => c.id -> memberLookup.getOrElse(AssessmentComponentKey(c), Nil)).toMap
    AssessmentComponentMembersResult(paperCodeMembers, componentMembers)
  }
}

trait AssessmentComponentMembersPermissions extends RequiresPermissionsChecking with PermissionsCheckingMethods {
  self: AssessmentComponentMembersState =>

  def permissionsCheck(p: PermissionsChecking): Unit = {
    p.PermissionCheck(RequiredPermission, department)
  }
}

trait AssessmentComponentMembersState {
  def department: Department
  def academicYear: AcademicYear
}

trait AssessmentComponentMembersRequest {
  var paperCodes: JList[String] = JArrayList()
  var assessmentComponents: JList[String] = JArrayList()
}
