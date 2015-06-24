package uk.ac.warwick.tabula.scheduling.web.controllers.turnitin

import org.springframework.stereotype.Controller
import uk.ac.warwick.tabula.services.fileserver.FileServer
import uk.ac.warwick.tabula.web.controllers.BaseController
import uk.ac.warwick.spring.Wire
import org.springframework.web.bind.annotation._
import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpServletResponse
import uk.ac.warwick.tabula.helpers.Logging
import uk.ac.warwick.tabula.ItemNotFoundException
import uk.ac.warwick.tabula.data.model._
import uk.ac.warwick.tabula.scheduling.commands.turnitin.DownloadFileByTokenCommand
import org.apache.http.HttpStatus

@Controller
@RequestMapping(value = Array("/turnitin/submission/{submission}/attachment/{fileAttachment}"))
class DownloadFileByTokenController extends BaseController with Logging {
	
	var fileServer = Wire.auto[FileServer]

	@ModelAttribute def command(@PathVariable("submission") submission: Submission,
															@PathVariable("fileAttachment") fileAttachment: FileAttachment,
															@RequestParam(value="token", required=true) token: FileAttachmentToken)
	= new DownloadFileByTokenCommand(submission, fileAttachment, token)

	@RequestMapping(method = Array(GET))
	def serve(command: DownloadFileByTokenCommand)(implicit request: HttpServletRequest, response: HttpServletResponse): Unit = {
		if (command.token.used) {
			response.setStatus(HttpStatus.SC_BAD_REQUEST)
			logger.error("Access token has already been used")
		} else if(command.token.expires.isBeforeNow){
			response.setStatus(HttpStatus.SC_BAD_REQUEST)
			logger.error("Expired file access token")
		} else if (!command.token.fileAttachmentId.equals(command.fileAttachment.id)) {
			response.setStatus(HttpStatus.SC_BAD_REQUEST)
			logger.error("Incorrect file access token")
		}	else {
			command.token.used = true
			command.callback = {(renderable) => fileServer.serve(renderable)}
			command.apply().orElse{ throw new ItemNotFoundException() }
		}
	}
}