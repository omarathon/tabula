package uk.ac.warwick.tabula.home.commands

import uk.ac.warwick.tabula.Mockito
import uk.ac.warwick.tabula.TestBase
import uk.ac.warwick.util.mail.WarwickMailSender
import org.springframework.validation.BindException
import javax.mail.internet.MimeMessage
import javax.mail.Session
import java.util.Properties
import javax.mail.internet.MimeMultipart
import javax.mail.Message.RecipientType

class AppCommentsCommandTest extends TestBase with Mockito {
	
	val mailSender = mock[WarwickMailSender]
	
	val session = Session.getDefaultInstance(new Properties)
	val mimeMessage = new MimeMessage(session)	   
	mailSender.createMimeMessage() returns mimeMessage
	
	val adminEmail = "stabula@warwick.ac.uk"
	
	private def newCommand() = {
		val cmd = new AppCommentCommand(currentUser)
		cmd.mailSender = mailSender
		cmd.freemarker = newFreemarkerConfiguration
		cmd.adminMailAddress = adminEmail
		
		cmd
	}
	
	@Test def populateFromNoUser {
		val cmd = newCommand()
		cmd.usercode should be (null)
		cmd.name should be (null)
		cmd.email should be (null)
	}
	
	@Test def populateWithUser = withUser("cuscav") {
		currentUser.apparentUser.setFullName("Billy Bob")
		currentUser.apparentUser.setEmail("billybob@warwick.ac.uk")
		
		val cmd = newCommand()
		cmd.usercode should not be ('empty)
		cmd.name should not be ('empty)
		cmd.email should not be ('empty)
	}
	
	@Test def validatePasses {
		val cmd = newCommand()
		cmd.message = "I'm coming for you"
			
		val errors = new BindException(cmd, "command")
		cmd.validate(errors)
		
		errors.hasErrors should be (false)
	}
	
	@Test def validateNoMessage {
		val cmd = newCommand()
		cmd.message = "   "
			
		val errors = new BindException(cmd, "command")
		cmd.validate(errors)
		
		errors.getErrorCount should be (1)
		errors.getFieldError.getField should be ("message")
		errors.getFieldError.getCode should be ("NotEmpty")
	}
	
	@Test def sendWithNothing {
		// Only message is required, so this should work even if the user doesn't fill anything out
		val cmd = newCommand()
		cmd.message = "I'm coming for you"
			
		cmd.applyInternal
		there was one(mailSender).send(mimeMessage)
		
		mimeMessage.getRecipients(RecipientType.TO).map {_.toString} should be (Array(adminEmail))
		mimeMessage.getFrom().map {_.toString} should be (Array(adminEmail))
		mimeMessage.getSubject() should be ("Tabula feedback")
		
		// Check properties have been set
		val text = mimeMessage.getContent match {
			case string: String => string
			case multipart: MimeMultipart => multipart.getBodyPart(0).getContent.toString
		}
		
		text should include ("I'm coming for you")
		text should include ("Name: Not provided")
	}
	
	@Test def sendFullyPopulated = withUser("cuscav") {
		currentUser.apparentUser.setFullName("Billy Bob")
		currentUser.apparentUser.setEmail("billybob@warwick.ac.uk")
		
		val cmd = newCommand()
		cmd.message = "I'm coming for you"
		cmd.currentPage = "http://stabula.warwick.ac.uk/my/page"
		cmd.browser = "Chrome"
		cmd.ipAddress = "137.205.194.132"
		cmd.os = "Window"
		cmd.resolution = "New years"
			
		cmd.applyInternal
		there was one(mailSender).send(mimeMessage)
		
		mimeMessage.getRecipients(RecipientType.TO).map {_.toString} should be (Array(adminEmail))
		mimeMessage.getFrom().map {_.toString} should be (Array(adminEmail))
		mimeMessage.getSubject() should be ("Tabula feedback")
		
		// Check properties have been set
		val text = mimeMessage.getContent match {
			case string: String => string
			case multipart: MimeMultipart => multipart.getBodyPart(0).getContent.toString
		}
		
		text should include ("I'm coming for you")
		text should include ("Name: Billy Bob")
		text should include ("Email: billybob@warwick.ac.uk")
		text should include ("Usercode: cuscav")
		text should include ("Current page: http://stabula.warwick.ac.uk/my/page")
		text should include ("Browser: Chrome")
		text should include ("OS: Window")
		text should include ("Screen resolution: New years")
		text should include ("IP address: 137.205.194.132")
	}

}