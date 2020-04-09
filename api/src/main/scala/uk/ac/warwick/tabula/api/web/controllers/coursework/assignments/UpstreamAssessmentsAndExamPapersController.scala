package uk.ac.warwick.tabula.api.web.controllers.coursework.assignments

import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.{ModelAttribute, PathVariable, RequestMapping}
import uk.ac.warwick.tabula.api.web.controllers.ApiController
import uk.ac.warwick.tabula.api.web.helpers.UpstreamAssessmentsAndExamsToJsonConverter
import uk.ac.warwick.tabula.commands._
import uk.ac.warwick.tabula.data.model.{AssessmentComponent, AssessmentType, Department}
import uk.ac.warwick.tabula.helpers.StringUtils._
import uk.ac.warwick.tabula.permissions.Permissions
import uk.ac.warwick.tabula.services.{AssessmentMembershipServiceComponent, AutowiringAssessmentMembershipServiceComponent}
import uk.ac.warwick.tabula.system.permissions.{PermissionsChecking, PermissionsCheckingMethods, RequiresPermissionsChecking}
import uk.ac.warwick.tabula.web.Mav
import uk.ac.warwick.tabula.web.views.JSONView

@Controller
@RequestMapping(Array("/v1/department/{department}/upstreamassessments"))
class UpstreamAssessmentsAndExamPapersController
  extends ApiController
    with UpstreamAssessmentsAndExamsToJsonConverter {

  @ModelAttribute("getUpstreamAssessmentsCommand")
  def command(@PathVariable department: Department): UpstreamAssessmentsAndExamPapersCommand.Command =
    UpstreamAssessmentsAndExamPapersCommand(department)

  @RequestMapping(method = Array(GET), produces = Array("application/json"))
  def list(@ModelAttribute("getUpstreamAssessmentsCommand") command: UpstreamAssessmentsAndExamPapersCommand.Command): Mav = {
    val assessmentComponents = command.apply()

    Mav(new JSONView(Map(
      "success" -> true,
      "status" -> "ok",
      "assessmentComponents" -> assessmentComponents.map(jsonUpstreamAssessmentObject),
    )))
  }
}

object UpstreamAssessmentsAndExamPapersCommand {
  type Command = Appliable[Seq[AssessmentComponent]]

  def apply(department: Department): Command =
    new UpstreamAssessmentsAndExamPapersCommandInternal(department)
      with ComposableCommand[Seq[AssessmentComponent]]
      with UpstreamAssessmentsAndExamPapersRequest
      with UpstreamAssessmentsAndExamPapersPermissions
      with AutowiringAssessmentMembershipServiceComponent
      with Unaudited with ReadOnly
}

abstract class UpstreamAssessmentsAndExamPapersCommandInternal(val department: Department)
  extends CommandInternal[Seq[AssessmentComponent]] with UpstreamAssessmentsAndExamPapersState {
  self: UpstreamAssessmentsAndExamPapersRequest
    with AssessmentMembershipServiceComponent =>

  override def applyInternal(): Seq[AssessmentComponent] =
    assessmentMembershipService.getAssessmentComponents(department, includeSubDepartments = true, Option(assessmentType), withExamPapersOnly, inUseOnly)
      .filter(c => examProfileCode.maybeText.isEmpty || c.scheduledExams(None).exists(s => examProfileCode.maybeText.contains(s.examProfileCode)))

}

trait UpstreamAssessmentsAndExamPapersPermissions extends RequiresPermissionsChecking with PermissionsCheckingMethods {
  self: UpstreamAssessmentsAndExamPapersState =>

  override def permissionsCheck(p: PermissionsChecking): Unit = {
    p.PermissionCheck(Permissions.Module.ManageExams, mandatory(department))
    p.PermissionCheck(Permissions.Profiles.Read.StudentCourseDetails.SpecialExamArrangements, mandatory(department))
  }
}

trait UpstreamAssessmentsAndExamPapersState {
  def department: Department
}

trait UpstreamAssessmentsAndExamPapersRequest {
  var assessmentType: AssessmentType = _
  var withExamPapersOnly: Boolean = false
  var examProfileCode: String = _
  var inUseOnly: Boolean = true
}
