package uk.ac.warwick.tabula.web.controllers.turnitin.tca

import org.springframework.context.annotation.Profile
import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.{ModelAttribute, PathVariable, RequestMapping}
import uk.ac.warwick.tabula.CurrentUser
import uk.ac.warwick.tabula.commands.cm2.turnitin.tca.TurnitinTcaSendSubmissionCommand
import uk.ac.warwick.tabula.data.model.FileAttachment
import uk.ac.warwick.tabula.helpers.Logging
import uk.ac.warwick.tabula.web.Mav
import uk.ac.warwick.tabula.web.controllers.cm2.CourseworkController
import uk.ac.warwick.tabula.web.views.JSONView

@Profile(Array("turnitinTca"))
@Controller
@RequestMapping(Array("/turnitin/tca/submit/{attachment}"))
class TurnitinTcaSendSubmissionController extends CourseworkController with Logging {

  type TurnitinTcaSendSubmissionCommand = TurnitinTcaSendSubmissionCommand.CommandType

  @ModelAttribute("command")
  def command(@PathVariable attachment:FileAttachment, user: CurrentUser): TurnitinTcaSendSubmissionCommand = TurnitinTcaSendSubmissionCommand(attachment, user.apparentUser)

  @RequestMapping(method = Array(POST))
  def submissionComplete(@ModelAttribute("command") command: TurnitinTcaSendSubmissionCommand): Mav = {
    Mav(new JSONView(Map(
      "succeeded" -> command.apply().isDefined
    ))).noLayout()
  }
}