package uk.ac.warwick.tabula.api.web.controllers.exams

import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.{ModelAttribute, PathVariable, RequestMapping}
import uk.ac.warwick.tabula.AcademicYear
import uk.ac.warwick.tabula.api.commands.exams.AssessmentComponentMembersCommand
import uk.ac.warwick.tabula.api.web.controllers.ApiController
import uk.ac.warwick.tabula.data.model.Department
import uk.ac.warwick.tabula.web.Mav
import uk.ac.warwick.tabula.web.views.JSONView
import uk.ac.warwick.tabula.api.web.helpers.UpstreamAssessmentsAndExamsToJsonConverter

/**
 * Lists the students registered on the specified assessment components
 */
@Controller
@RequestMapping(Array("/v1/department/{department}/{academicYear}/assessmentComponentMembers"))
class AssessmentComponentMembersController extends ApiController with UpstreamAssessmentsAndExamsToJsonConverter {

  @ModelAttribute("command")
  def command(@PathVariable department: Department, @PathVariable academicYear: AcademicYear): AssessmentComponentMembersCommand.Command =
    AssessmentComponentMembersCommand(department, academicYear)

  @RequestMapping(method = Array(GET), produces = Array("application/json"))
  def assessmentComponentMembers(@ModelAttribute("command") command: AssessmentComponentMembersCommand.Command): Mav = {
    val info = command.apply()

    Mav(new JSONView(Map(
      "success" -> true,
      "status" -> "ok",
      "assessmentComponents" -> info.byAssessmentComponent.view.mapValues(v => v.map(jsonUpstreamAssessmentGroupInfoObject)).toMap,
      "paperCodes" -> info.byPaperCode.view.mapValues(v => v.map(jsonUpstreamAssessmentGroupInfoObject)).toMap,
    )))
  }
}
