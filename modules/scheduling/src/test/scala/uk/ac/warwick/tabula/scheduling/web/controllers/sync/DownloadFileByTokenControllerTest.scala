package uk.ac.warwick.tabula.scheduling.web.controllers.sync

import uk.ac.warwick.tabula.{ItemNotFoundException, Fixtures, FeaturesImpl, TestBase}
import org.scalatest.mock.MockitoSugar
import org.springframework.mock.web.MockHttpServletRequest
import org.springframework.mock.web.MockHttpServletResponse
import uk.ac.warwick.tabula.data.model.{FileAttachmentToken, FileAttachment}
import uk.ac.warwick.tabula.services.fileserver.FileServer
import java.io.ByteArrayInputStream
import org.apache.http.HttpStatus
import org.springframework.util.FileCopyUtils
import java.io.FileOutputStream
import uk.ac.warwick.tabula.scheduling.web.controllers.turnitin.DownloadFileByTokenController
import uk.ac.warwick.tabula.scheduling.commands.turnitin.DownloadFileByTokenCommand
import uk.ac.warwick.tabula.data.model.forms.SavedFormValue
import uk.ac.warwick.tabula.events.EventHandling
import org.joda.time.DateTime

class DownloadFileByTokenControllerTest extends TestBase with MockitoSugar {
	
	val controller = new DownloadFileByTokenController
	controller.fileServer = new FileServer
	controller.fileServer.features = new FeaturesImpl
	controller.fileServer.features.xSendfile = false

	val attachment = new FileAttachment
	val attachmentId = "123"

	val file = createTemporaryFile
	FileCopyUtils.copy(new ByteArrayInputStream("some file content".getBytes), new FileOutputStream(file))
	attachment.file = file
	attachment.id = attachmentId

	val submission1 = Fixtures.submissionWithId("0000001", id = attachmentId)
	val submission2 = Fixtures.submissionWithId("0000002", id = "456")

	val sv = new SavedFormValue
	sv.name = "upload"
	sv.submission = submission1

	attachment.submissionValue = sv

	EventHandling.enabled = false

	implicit val request = new MockHttpServletRequest
	implicit val response = new MockHttpServletResponse

	@Test
	def validToken() {

		val token = new FileAttachmentToken
		token.init(attachment)

		val command = new DownloadFileByTokenCommand(submission1, attachment, token)

		controller.serve(command)
		
		response.getStatus should be (HttpStatus.SC_OK)
		response.getContentAsString should be ("some file content")
	}

	@Test
	def invalidUsedToken() {

		val token = new FileAttachmentToken
		token.init(attachment)
		token.used = true

		val command = new DownloadFileByTokenCommand(submission1, attachment, token)

		controller.serve(command)

		response.getStatus should be (HttpStatus.SC_BAD_REQUEST)
	}

	@Test
	def invalidExpiredToken() {

		val token = new FileAttachmentToken
		token.init(attachment)
		token.expires = new DateTime().minusMinutes(1)

		val command = new DownloadFileByTokenCommand(submission1, attachment, token)

		controller.serve(command)

		response.getStatus should be (HttpStatus.SC_BAD_REQUEST)
	}

	@Test
	def invalidAccessToken() {

		val token = new FileAttachmentToken
		token.init(attachment)
		token.fileAttachmentId = "wrong id"

		val command = new DownloadFileByTokenCommand(submission1, attachment, token)

		controller.serve(command)

		response.getStatus should be (HttpStatus.SC_BAD_REQUEST)
	}

	@Test(expected = classOf[ItemNotFoundException])
	def submissionAndAttachmentNotLinked() {

		val token = new FileAttachmentToken
		token.init(attachment)

		val command = new DownloadFileByTokenCommand(submission2, attachment, token)

		controller.serve(command)

	}
}