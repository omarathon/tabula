package uk.ac.warwick.tabula.api.web.controllers.attachments

import java.io.InputStream
import javax.servlet.http.HttpServletResponse

import org.springframework.http.HttpStatus
import org.springframework.stereotype.Controller
import org.springframework.validation.BindException
import org.springframework.web.bind.annotation.{RequestParam, RequestHeader, RequestMapping}
import uk.ac.warwick.tabula.CurrentUser
import uk.ac.warwick.tabula.api.web.controllers.ApiController
import uk.ac.warwick.tabula.api.web.helpers.FileAttachmentToJsonConverter
import uk.ac.warwick.tabula.data.model.FileAttachment
import uk.ac.warwick.tabula.data.Transactions._
import uk.ac.warwick.tabula.data.{AutowiringFileDaoComponent, FileDaoComponent}
import uk.ac.warwick.tabula.web.Routes
import uk.ac.warwick.tabula.web.views.{JSONErrorView, JSONView}

@Controller
@RequestMapping(Array("/v1/attachments"))
class FileAttachmentController extends ApiController
	with CreateFileAttachmentApi
	with FileAttachmentToJsonConverter
	with AutowiringFileDaoComponent

trait CreateFileAttachmentApi {
	self: ApiController with FileDaoComponent with FileAttachmentToJsonConverter =>

	// The filename can be passed either as a query parameter or an X-Filename header - don't do PathVariable as URIs
	// have terrible formatting and it'll clash with the URL for getting information about an attachment
	@RequestMapping(method = Array(POST), params = Array("filename"))
	def createFileQueryParam(
		is: InputStream,
		@RequestHeader("Content-Type") contentType: String,
		@RequestHeader("Content-Length") contentLength: Long,
		@RequestParam filename: String,
		user: CurrentUser
	)(implicit response: HttpServletResponse) = createFile(is, contentType, contentLength, filename, user)

	@RequestMapping(method = Array(POST), headers = Array("X-Filename"))
	def createFileHeader(
		is: InputStream,
		@RequestHeader("Content-Type") contentType: String,
		@RequestHeader("Content-Length") contentLength: Long,
		@RequestHeader("X-Filename") filename: String,
		user: CurrentUser
	)(implicit response: HttpServletResponse) = createFile(is, contentType, contentLength, filename, user)

	private def createFile(is: InputStream, contentType: String, contentLength: Long, filename: String, user: CurrentUser)(implicit response: HttpServletResponse) = {
		val a = transactional() {
			val attachment = new FileAttachment
			attachment.name = filename
			attachment.uploadedData = is
			attachment.uploadedDataLength = contentLength
			attachment.uploadedBy = user.apparentId
			fileDao.saveTemporary(attachment)
		}

		response.setStatus(HttpStatus.CREATED.value())
		response.addHeader("Location", toplevelUrl + Routes.api.attachment(a))

		Mav(new JSONView(Map(
			"success" -> true,
			"status" -> "ok",
			"attachment" -> jsonFileAttachmentObject(a)
		)))
	}

	// Catch the situation where a filename hasn't been provided
	@RequestMapping(method = Array(POST), params = Array("!filename"), headers = Array("!X-Filename"))
	def createFileNoFilename() = {
		val errors = new BindException(new Object, "request")
		errors.reject("fileattachment.api.nofilename")

		new JSONErrorView(errors)
	}

}