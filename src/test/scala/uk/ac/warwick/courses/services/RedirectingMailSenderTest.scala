package uk.ac.warwick.courses.services
import uk.ac.warwick.courses.TestBase
import uk.ac.warwick.util.mail.AsynchronousWarwickMailSender
import org.springframework.mail.SimpleMailMessage
import org.junit.Test
import uk.ac.warwick.courses.Mockito
import uk.ac.warwick.util.mail.WarwickMailSender


class RedirectingMailSenderTest extends TestBase with Mockito {
	@Test def barelyTest {
	  val delegate = mock[WarwickMailSender]
	   
	  val text = """
		CONGRATS
		
		You have won a bike. Pick it up from the basement.
		
		(fools)
	  		"""
		
	  val sender = new RedirectingMailSender(delegate)
	  
	  val message = new SimpleMailMessage
	  message.setTo(Array("ron@example.com", "jim@example.net"))
	  message.setFrom("no-reply@example.com")
	  message.setText(text)
	  sender.send(message)
	  
	  // This passes because the sender changes the message object.
	  // It should probably make a copy.
	  there was one (delegate).send(message)
	}
}