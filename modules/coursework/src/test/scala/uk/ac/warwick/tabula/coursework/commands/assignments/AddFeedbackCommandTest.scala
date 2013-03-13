package uk.ac.warwick.tabula.coursework.commands.assignments

import uk.ac.warwick.tabula.TestBase
import uk.ac.warwick.tabula.Mockito
import uk.ac.warwick.tabula.Fixtures
import org.springframework.validation.BindException
import uk.ac.warwick.tabula.commands.UploadedFile
import uk.ac.warwick.tabula.MockUserLookup
import uk.ac.warwick.userlookup.User
import uk.ac.warwick.tabula.data.model.FileAttachment
import org.springframework.mock.web.MockMultipartFile
import uk.ac.warwick.tabula.data.FileDao

class AddFeedbackCommandTest extends TestBase with Mockito {
	
	val module = Fixtures.module("cs118")
	val assignment = Fixtures.assignment("my assignment")
	assignment.module = module
	
	val userLookup = new MockUserLookup
	val user = new User("student")
	user.setFoundUser(true)
	user.setWarwickId("1010101")
	userLookup.users += ("student" -> user)
	
	@Test def duplicateFileNames = withUser("cuscav") {
		val cmd = new AddFeedbackCommand(module, assignment, currentUser)
		cmd.userLookup = userLookup
		
		cmd.uniNumber = "1010101"
			
		val file = new UploadedFile
		val a = new FileAttachment
		a.name = "file.txt"
		a.uploadedDataLength = 300
		file.attached.add(a)
		
		val item = new FeedbackItem("1010101")
		item.file = file
		cmd.items.add(item)
		
		// Add an existing feedback with the same name
		val feedback = Fixtures.feedback("1010101")
		feedback.addAttachment(a)
		assignment.feedbacks.add(feedback)
		
		item.submissionExists should be (false)
		
		val errors = new BindException(cmd, "command")
		cmd.postExtractValidation(errors)
		
		errors.hasErrors should be (false)
		
		item.submissionExists should be (true)
		item.duplicateFileNames should be (Set("file.txt"))
	}

}