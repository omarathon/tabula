package uk.ac.warwick.courses.services
import uk.ac.warwick.courses.TestBase
import uk.ac.warwick.util.mail.AsynchronousWarwickMailSender
import org.springframework.mail.SimpleMailMessage
import org.junit.Test


class RedirectingMailSenderTest extends TestBase {
	@Test def barelyTest {
	  val sender = new RedirectingMailSender(null)
	  
	  val message = new SimpleMailMessage
	  message.setTo(Array("ron@example.com", "jim@example.net"))
	  message.setFrom("no-reply@example.com")
	  message.setCc(Array("overlord@example.com"))
	  message.setText("""
		CONGRATS
		
		You have won a bike. Pick it up from the basement.
		
		(fools)
	  		""")
	  sender.send(message)
	}
}