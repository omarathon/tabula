package uk.ac.warwick.tabula.web.controllers.cm2

import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.{ModelAttribute, PathVariable, PostMapping, RequestMapping}
import uk.ac.warwick.tabula.commands.cm2.assignments.SubmissionAttemptCommand
import uk.ac.warwick.tabula.data.model.Assignment
import uk.ac.warwick.tabula.web.Mav
import uk.ac.warwick.tabula.web.views.JSONView

@Controller
@RequestMapping(value = Array("/coursework/submission/{assignment}/attempt"))
class SubmissionAttemptController extends CourseworkController {

  @ModelAttribute("command")
  def command(@PathVariable assignment: Assignment): SubmissionAttemptCommand.Command =
    SubmissionAttemptCommand(mandatory(assignment), user)

  @PostMapping
  def submit(@ModelAttribute("command") cmd: SubmissionAttemptCommand.Command): Mav = {
    cmd.apply()
    Mav(new JSONView(Map(
      "success" -> true
    )))
  }

}
