package uk.ac.warwick.tabula.profiles.web.controllers.tutor

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import javax.servlet.http.HttpServletRequest
import uk.ac.warwick.spring.Wire
import uk.ac.warwick.tabula.data.model.MeetingRecord
import uk.ac.warwick.tabula.services.ZipService
import uk.ac.warwick.tabula.services.fileserver.FileServer
import uk.ac.warwick.tabula.services.fileserver.RenderableZip
import uk.ac.warwick.tabula.web.controllers.BaseController
import org.springframework.web.bind.annotation.RequestMethod
import javax.servlet.http.HttpServletResponse
import uk.ac.warwick.tabula.services.fileserver.RenderableFile
import uk.ac.warwick.tabula.services.fileserver.RenderableAttachment

@Controller
class DownloadMeetingRecordFilesController extends BaseController {

	var zipService = Wire.auto[ZipService]
	@Autowired var fileServer: FileServer = _

	@RequestMapping(value = Array("/tutor/meeting/{meetingRecord}/attachment/{filename}.zip"), method = Array(RequestMethod.GET, RequestMethod.HEAD))
	def getAttachmentsZipped(@PathVariable("meetingRecord") meetingRecord: MeetingRecord, @PathVariable("filename") filename: String)(implicit request: HttpServletRequest, out: HttpServletResponse) {

		// the zip name, though part of the URL, is not actually used
		var zip = zipService.getSomeMeetingRecordAttachmentsZip(meetingRecord);
		val renderable = new RenderableZip(zip)
		fileServer.serve(renderable)
	}

	@RequestMapping(value = Array("/tutor/meeting/{meetingRecord}/attachment/{filename}"), method = Array(RequestMethod.GET, RequestMethod.HEAD))
	def getSingleAttachment(@PathVariable("meetingRecord") meetingRecord: MeetingRecord, @PathVariable("filename") filename: String)(implicit request: HttpServletRequest, out: HttpServletResponse) {

		val attachment = meetingRecord.attachments.get(0)
		val renderable = new RenderableAttachment(attachment)
		fileServer.serve(renderable)
	}
}

