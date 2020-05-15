package uk.ac.warwick.tabula.web.controllers.marks

import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation._
import uk.ac.warwick.tabula.commands.marks.AssessmentComponentMarksTemplateCommand
import uk.ac.warwick.tabula.data.model.{AssessmentComponent, UpstreamAssessmentGroup}
import uk.ac.warwick.tabula.web.controllers.BaseController
import uk.ac.warwick.tabula.web.views.ExcelView

@Controller
@RequestMapping(Array("/marks/admin/assessment-component/{assessmentComponent}/{upstreamAssessmentGroup}/marks/template.xlsx"))
class AssessmentComponentMarksTemplateController extends BaseController {

  @ModelAttribute("command")
  def command(@PathVariable assessmentComponent: AssessmentComponent, @PathVariable upstreamAssessmentGroup: UpstreamAssessmentGroup): AssessmentComponentMarksTemplateCommand.Command =
    AssessmentComponentMarksTemplateCommand(assessmentComponent, upstreamAssessmentGroup)

  @RequestMapping
  def template(@ModelAttribute("command") command: AssessmentComponentMarksTemplateCommand.Command): ExcelView =
    command.apply()

}
