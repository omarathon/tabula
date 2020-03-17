package uk.ac.warwick.tabula.web.controllers.turnitin.tca.sysadmin

import org.springframework.context.annotation.Profile
import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.{ModelAttribute, PathVariable, RequestMapping}
import uk.ac.warwick.tabula.cm2.web.Routes
import uk.ac.warwick.tabula.commands.cm2.turnitin.tca.sysadmin.TurnitinTcaRetrySubmissionCommand
import uk.ac.warwick.tabula.data.model.{Assignment, FileAttachment}
import uk.ac.warwick.tabula.helpers.Logging
import uk.ac.warwick.tabula.services.turnitintca.AutowiringTurnitinTcaServiceComponent
import uk.ac.warwick.tabula.web.Mav
import uk.ac.warwick.tabula.web.controllers.BaseController

  @Profile(Array("turnitinTca"))
  @Controller
  @RequestMapping(Array("/coursework/sysadmin/assignments/{assignment}/turnitin/tca-resubmit/{attachment}"))
  class TurnitinTcaRetrySubmissionController extends BaseController with Logging with AutowiringTurnitinTcaServiceComponent  {

    type TurnitinTcaRetrySubmissionCommand = TurnitinTcaRetrySubmissionCommand.CommandType

    @ModelAttribute("command")
    def command(@PathVariable assignment: Assignment, @PathVariable attachment: FileAttachment
    ): TurnitinTcaRetrySubmissionCommand = TurnitinTcaRetrySubmissionCommand(mandatory(assignment), mandatory(attachment))

    @RequestMapping(method = Array(POST))
    def getSubmissionInfo(@PathVariable attachment: FileAttachment, @ModelAttribute("command") command: TurnitinTcaRetrySubmissionCommand): Mav = {
      command.apply()
      Redirect(Routes.admin.assignment.submissionsandfeedback(attachment.submissionValue.submission.assignment))
    }

}
