package uk.ac.warwick.tabula.web.controllers.turnitin.tca

import org.springframework.context.annotation.Profile
import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.{ModelAttribute, PathVariable, RequestMapping}
import uk.ac.warwick.tabula.cm2.web.Routes
import uk.ac.warwick.tabula.commands.cm2.turnitin.tca.TurnitinTcaSendSubmissionCommand
import uk.ac.warwick.tabula.data.model.Assignment
import uk.ac.warwick.tabula.helpers.Logging
import uk.ac.warwick.tabula.web.Mav
import uk.ac.warwick.tabula.web.controllers.cm2.CourseworkController

@Profile(Array("turnitinTca"))
@Controller
@RequestMapping(Array("/coursework/admin/assignments/{assignment}/turnitin/tca"))
class TurnitinTcaSendSubmissionController extends CourseworkController with Logging {

  type TurnitinTcaSendSubmissionCommand = TurnitinTcaSendSubmissionCommand.CommandType

  @ModelAttribute("command")
  def command(@PathVariable assignment:Assignment): TurnitinTcaSendSubmissionCommand = TurnitinTcaSendSubmissionCommand(assignment)

  @RequestMapping(method = Array(POST))
  def submissionComplete(@PathVariable assignment:Assignment, @ModelAttribute("command") command: TurnitinTcaSendSubmissionCommand): Mav = {
    command.apply()
    Redirect(Routes.admin.assignment.submissionsandfeedback(assignment))
  }
}
