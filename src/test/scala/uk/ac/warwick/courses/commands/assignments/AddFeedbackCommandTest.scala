package uk.ac.warwick.courses.commands.assignments
import org.springframework.transaction.annotation.Transactional
import org.springframework.beans.factory.annotation.Autowired
import uk.ac.warwick.courses.AppContextTestBase
import uk.ac.warwick.courses.data.ModuleDao
import org.junit.Test
import uk.ac.warwick.courses.data.model.Assignment
import org.springframework.web.multipart.MultipartFile
import org.springframework.mock.web.MockMultipartFile

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
			command.file.upload = new MockMultipartFile("feedback.docx", feedbackDocument)
			command.onBind
			val feedback = command.apply
			feedback.attachments.get(0).data.length should be (feedbackDocument.length)
		}
		
	}
}