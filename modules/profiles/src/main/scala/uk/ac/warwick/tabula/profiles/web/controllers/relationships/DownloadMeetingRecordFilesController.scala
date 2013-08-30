package uk.ac.warwick.tabula.profiles.web.controllers.relationships

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.PathVariable
import javax.servlet.http.HttpServletRequest
import uk.ac.warwick.tabula.data.model.MeetingRecord
import uk.ac.warwick.tabula.services.fileserver.FileServer
import uk.ac.warwick.tabula.web.controllers.BaseController
import org.springframework.web.bind.annotation.RequestMethod
import javax.servlet.http.HttpServletResponse
import org.springframework.web.bind.annotation.ModelAttribute
import uk.ac.warwick.tabula.profiles.commands.DownloadMeetingRecordFilesCommand
import uk.ac.warwick.tabula.ItemNotFoundException

@Controller
class DownloadMeetingRecordFilesController extends BaseController {

	@Autowired var fileServer: FileServer = _

	@ModelAttribute def command(@PathVariable("meetingRecord") meetingRecord: MeetingRecord)
		= new DownloadMeetingRecordFilesCommand(meetingRecord)

	// the difference between the RequestMapping paths for these two methods is a bit subtle - the first has
	// attachments plural, the second has attachments singular.
	@RequestMapping(value = Array("/*/meeting/{meetingRecord}/attachments/*"), method = Array(RequestMethod.GET, RequestMethod.HEAD))
	def getAll(command: DownloadMeetingRecordFilesCommand)(implicit request: HttpServletRequest, response: HttpServletResponse): Unit = {
		getOne(command, null)
	}

	@RequestMapping(value = Array("/*/meeting/{meetingRecord}/attachment/{filename}"), method = Array(RequestMethod.GET, RequestMethod.HEAD))
	def getOne(command: DownloadMeetingRecordFilesCommand, @PathVariable("filename") filename: String)
		(implicit request: HttpServletRequest, response: HttpServletResponse): Unit = {
		// specify callback so that audit logging happens around file serving
		command.callback = { (renderable) => fileServer.serve(renderable) }
		command.apply().orElse { throw new ItemNotFoundException() }
	}
}
