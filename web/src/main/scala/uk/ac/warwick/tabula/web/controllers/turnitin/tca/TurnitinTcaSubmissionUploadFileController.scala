package uk.ac.warwick.tabula.web.controllers.turnitin.tca

import org.springframework.context.annotation.Profile
import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.{ModelAttribute, PathVariable, RequestMapping}
import uk.ac.warwick.tabula.CurrentUser
import uk.ac.warwick.tabula.commands.cm2.turnitin.tca.TurnitinTcaSubmissionUploadFileCommand
import uk.ac.warwick.tabula.data.model.FileAttachment
import uk.ac.warwick.tabula.helpers.Logging
import uk.ac.warwick.tabula.web.Mav
import uk.ac.warwick.tabula.web.controllers.cm2.CourseworkController

@Profile(Array("turnitinTca"))
@Controller
@RequestMapping(Array("/turnitin/tca/submission/attachment/{attachment}"))
class TurnitinTcaSubmissionUploadFileController extends CourseworkController with Logging {

  type TurnitinTcaSubmissionUploadFileCommand = TurnitinTcaSubmissionUploadFileCommand.CommandType

  @ModelAttribute("command")
  def command(@PathVariable attachment:FileAttachment, user: CurrentUser): TurnitinTcaSubmissionUploadFileCommand = TurnitinTcaSubmissionUploadFileCommand(attachment, user.apparentUser)

  @RequestMapping(method = Array(POST))
  def submissionComplete(@ModelAttribute("command") command: TurnitinTcaSubmissionUploadFileCommand): Mav = {
    command.apply()
    Mav.empty()
  }
}