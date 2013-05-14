package uk.ac.warwick.tabula.profiles.commands

import uk.ac.warwick.tabula.Mockito
import uk.ac.warwick.tabula.TestBase
import uk.ac.warwick.tabula.data.model.Member
import uk.ac.warwick.tabula.helpers.Promises
import uk.ac.warwick.util.mail.WarwickMailSender
import uk.ac.warwick.util.mail.AsynchronousWarwickMailSender
import javax.mail.internet.MimeMessage
import javax.mail.Session
import java.util.Properties

class TutorChangeNotifierCommandTest extends TestBase with Mockito with Promises {

	@Test def messageForTest {
		val actualOldTutor = mock[Member]
		val oldTutor: Option[Member] = Some(actualOldTutor)

		val newTutor = mock[Member]
		newTutor.email = "foo@bar.com"

		val student = mock[Member]


		val cmd = new TutorChangeNotifierCommand(student, oldTutor, promise { newTutor })

		// get a sufficient mailSender
		val mailSender = mock[WarwickMailSender]
		val session = Session.getDefaultInstance(new Properties)
		val mimeMessage = new MimeMessage(session)
		mailSender.createMimeMessage() returns mimeMessage
		cmd.mailSender = mailSender


		cmd.replyAddress = "reply@to.me"
		cmd.fromAddress = "from@to.me"
			
		// FIXME not testing anything?

/*		val message = cmd.messageFor("/WEB-INF/freemarker/emails/new_tutor_notification.ftl",
			"new@tutor.com",
//			newTutor.email,
			student,
			oldTutor,
			newTutor,
			"www.warwick.ac.uk")

		message.getFrom() should be("from@to.me")
		message.getReplyTo() should be("reply@to.me")
		message.getAllRecipients().head should be("foo@bar.com")
		message.getSubject should be("Personal tutor change")*/
	}
}
