package uk.ac.warwick.tabula.scheduling.commands.turnitin

import uk.ac.warwick.tabula.{Fixtures, TestBase, Mockito}
import uk.ac.warwick.tabula.data.model.FileAttachment
import uk.ac.warwick.tabula.data.model.forms.SavedFormValue
import org.springframework.validation.BindException
import org.joda.time.DateTime

class DownloadFileByTokenCommandTest extends TestBase with Mockito {

	trait Fixture {
		val attachment = new FileAttachment
		val attachmentId = "attachment"
		attachment.id = attachmentId

		val submission = Fixtures.submissionWithId("0000001", id = "submission")

		val sv = new SavedFormValue
		sv.submission = submission

		attachment.submissionValue = sv

		val token = attachment.generateToken()

		val command = DownloadFileByTokenCommand(submission, attachment, token)

		val errors = new BindException(command, "command")
	}


	@Test
	def validToken() { new Fixture {
		command.validate(errors)
		errors.hasErrors should be {false}
	}}

	@Test
	def invalidUsedToken() { new Fixture {
		token.dateUsed = new DateTime()
		command.validate(errors)
		errors.hasErrors should be {true}
	}}

	@Test
	def invalidExpiredToken() { new Fixture {
		token.expires = new DateTime().minusMinutes(1)
		command.validate(errors)
		errors.hasErrors should be {true}
	}}

	@Test
	def invalidAccessToken() { new Fixture {
		token.fileAttachmentId = "wrong id"
		command.validate(errors)
		errors.hasErrors should be {true}
	}}

}