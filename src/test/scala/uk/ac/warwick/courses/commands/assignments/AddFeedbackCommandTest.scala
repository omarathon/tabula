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

class AddFeedbackCommandTest extends AppContextTestBase {
	
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
			val feedback = command.apply
			feedback.attachments.get(0).data.length should be (feedbackDocument.length)
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
			command.onBind
			command.validation(errors)
			println(errors)
			errors.hasErrors should be (false)
			
			command.items.size should be(2)
			command.unrecognisedFiles.size should be(1)
		}
	}
}