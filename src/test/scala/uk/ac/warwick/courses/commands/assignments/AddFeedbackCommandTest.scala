package uk.ac.warwick.courses.commands.assignments
import org.springframework.transaction.annotation.Transactional
import org.springframework.beans.factory.annotation.Autowired
import uk.ac.warwick.courses.AppContextTestBase
import uk.ac.warwick.courses.data.ModuleDao
import org.junit.Test
import uk.ac.warwick.courses.data.model.Assignment
import org.springframework.web.multipart.MultipartFile
import org.springframework.mock.web.MockMultipartFile
import collection.JavaConversions._
import org.springframework.validation.BindException
import uk.ac.warwick.userlookup.UserLookupInterface
import uk.ac.warwick.userlookup.UserLookup
import uk.ac.warwick.userlookup.UserLookupBackend
import uk.ac.warwick.courses.services.SwappableUserLookupService
import uk.ac.warwick.courses.services.UserLookupService
import org.junit.Before
import uk.ac.warwick.userlookup.User
import org.specs.mock.ClassMocker
import uk.ac.warwick.courses.LenientUserLookup

class AddFeedbackCommandTest extends AppContextTestBase with ClassMocker with LenientUserLookup {
	
	@Autowired var modules:ModuleDao =_
	
	@Transactional
	@Test def add {
		

		withUser("abc") {
			val feedbackDocument = resourceAsBytes("feedback.docx")
			
			val assignment = new Assignment
			session.save(assignment)
			val command = new AddFeedbackCommand(assignment, currentUser)
			command.uniNumber = "1234567"
			command.file.upload = List(new MockMultipartFile("feedback.docx", feedbackDocument))
			command.onBind
			val feedbacks = command.apply
			feedbacks.size should be (1)
			feedbacks(0).attachments.get(0).length should be (Some(feedbackDocument.length))
		}
		
	}
	
	@Transactional
	@Test def addZip {
		withUser("abc") {
			val feedbackZip = resourceAsBytes("feedback1.zip")
			
			val assignment = new Assignment
			session.save(assignment)
			val command = new AddFeedbackCommand(assignment, currentUser)
			command.archive = new MockMultipartFile("archive", "feedback.zip", "application/unknown", feedbackZip)
			val errors = new BindException(command,"command")
			command.preExtractValidation(errors)
			command.onBind
			command.postExtractValidation(errors)
			errors.hasErrors should be (false)
			
			command.items.size should be(2)
			command.unrecognisedFiles.size should be(1)
			
			val attached0123456 = command.items.find { _.uniNumber == "0123456" }.get.file.attached
			attached0123456.find { _.name == "feedback.txt" }.get.length should be (Some(975))
		}
	}
}