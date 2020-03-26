package uk.ac.warwick.tabula.api.web.controllers.coursework.assignments

import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.{ModelAttribute, PathVariable, RequestMapping, RequestParam}
import uk.ac.warwick.tabula.api.web.controllers.ApiController
import uk.ac.warwick.tabula.api.web.helpers.UpstreamAssessmentsAndExamsToJsonConverter
import uk.ac.warwick.tabula.commands.ViewViewableCommand
import uk.ac.warwick.tabula.data.model.{AssessmentType, Department}
import uk.ac.warwick.tabula.permissions.Permissions
import uk.ac.warwick.tabula.services.AutowiringAssessmentMembershipServiceComponent
import uk.ac.warwick.tabula.web.Mav
import uk.ac.warwick.tabula.web.views.JSONView

@Controller
@RequestMapping(Array("/v1/department/{department}/upstreamassessments"))
class UpstreamAssessmentsAndExamPapersController
  extends ApiController
    with AutowiringAssessmentMembershipServiceComponent
    with UpstreamAssessmentsAndExamsToJsonConverter {

  @ModelAttribute("getUpstreamAssessmentsCommand")
  def command(@PathVariable department: Department): ViewViewableCommand[Department] =
    new ViewViewableCommand(Permissions.Module.ManageExams, mandatory(department))

  @RequestMapping(method = Array(GET), produces = Array("application/json"))
  def list(
    @ModelAttribute("getUpstreamAssessmentsCommand") command: ViewViewableCommand[Department],
    @RequestParam(required = false) assessmentType: AssessmentType,
    @RequestParam(defaultValue = "false") withExamPapersOnly: Boolean): Mav = {

    val department = command.apply()
      val assessmentComponents = assessmentMembershipService.getAssessmentComponents(department, includeSubDepartments = true, Option(assessmentType), withExamPapersOnly)

      Mav(new JSONView(Map(
        "success" -> true,
        "status" -> "ok",
        "assessmentComponents" -> assessmentComponents.map(jsonUpstreamAssessmentObject),
      )))
    }
}
