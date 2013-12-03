package uk.ac.warwick.tabula.coursework.commands.feedback

import org.mockito.Mockito._
import uk.ac.warwick.tabula.services._
import uk.ac.warwick.tabula.{CurrentUser, Mockito, TestBase}
import uk.ac.warwick.tabula.data.{SavedFormValueDao, SavedFormValueDaoComponent}
import uk.ac.warwick.tabula.data.model.{StudentMember, Module, Assignment, Feedback}
import uk.ac.warwick.userlookup.User
import collection.JavaConversions._
import uk.ac.warwick.tabula.JavaImports._
import uk.ac.warwick.tabula.data.model.forms.{SavedFormValue, StringFormValue}
import org.springframework.validation.BindException

class OnlineFeedbackFormCommandTest extends TestBase with Mockito {

	trait Fixture {

		val assignment = new Assignment
		assignment.collectMarks = true
		assignment.addDefaultFeedbackFields()
		val module = new Module

		val student = new User("student")
		val marker = new User("marker")
		val currentUser = new CurrentUser(marker, marker)

		val feedbackField = assignment.feedbackFields.find(_.name == Assignment.defaultFeedbackTextFieldName).get
		feedbackField.id = Assignment.defaultFeedbackTextFieldName
		val feedbackValue = new StringFormValue(feedbackField)
		val heronRamble = "You would have got a first if you didn't mention Herons so much. I hate herons"
		feedbackValue.value = heronRamble

		val savedFormValue = new SavedFormValue()
		savedFormValue.id = Assignment.defaultFeedbackTextFieldName
		savedFormValue.name = Assignment.defaultFeedbackTextFieldName
		savedFormValue.value = heronRamble

		val command = new OnlineFeedbackFormCommand(module, assignment, student, currentUser)
			with OnlineFeedbackFormCommandTestSupport
	}

	@Test
	def copyTo() {
		new Fixture {

			command.mark = "67"
			command.grade = "2:1"
			command.fields = JMap(Assignment.defaultFeedbackTextFieldName -> feedbackValue)

			val feedback = command.apply()

			feedback.actualMark should be(Some(67))
			feedback.actualGrade should be(Some("2:1"))
			feedback.customFormValues.size should be(1)
		}
	}

	@Test
	def copyToWithBlankMarkAndGrade(){
		new Fixture {

			command.mark = ""
			command.grade = ""
			command.fields = JMap(Assignment.defaultFeedbackTextFieldName -> feedbackValue)

			val feedback = command.apply()

			feedback.actualMark should be(None)
			feedback.actualGrade should be(None)
			feedback.customFormValues.size should be(1)
		}

	}

	@Test
	def invalidateZipsWhenUpdatingFeedback() {
		new Fixture {
			val existingFeedback = new Feedback("student")
			existingFeedback.id = "existingFeedback"
			assignment.feedbacks = Seq(existingFeedback)

			when(command.savedFormValueDao.get(feedbackField, existingFeedback)) thenReturn None

			val feedback = command.apply()

			there was one(command.zipService).invalidateIndividualFeedbackZip(existingFeedback)
			there was one(command.zipService).invalidateFeedbackZip(assignment)
		}
	}

	@Test
	def fieldsCopiedWhenUpdatingFeedback() {
		new Fixture {
			val existingFeedback = new Feedback("student")
			existingFeedback.actualGrade = Some("2:2")
			existingFeedback.actualMark = Some(55)
			existingFeedback.id = "existingFeedback"
			savedFormValue.feedback = existingFeedback
			existingFeedback.customFormValues = JSet(savedFormValue)

			assignment.feedbacks = Seq(existingFeedback)

			val command2 = new OnlineFeedbackFormCommand(module, assignment, student, currentUser)
				with OnlineFeedbackFormCommandTestSupport
			command2.mark should be("55")
			command2.grade should be("2:2")
			command2.fields.size should be (1)
			val field = command2.fields.get(Assignment.defaultFeedbackTextFieldName).asInstanceOf[StringFormValue]
			field.value should be(heronRamble)
		}
	}

	@Test
	def validation() {
		new Fixture {
			var errors = new BindException(command, "command")
			command.mark = "heron"
			errors.hasErrors should be (false)
			command.validate(errors)
			errors.hasErrors should be (true)

			errors = new BindException(command, "command")
			errors.hasErrors should be (false)
			command.mark = "101"
			command.validate(errors)
			errors.hasErrors should be (true)

			errors = new BindException(command, "command")
			errors.hasErrors should be (false)
			command.mark = "-5"
			command.validate(errors)
			errors.hasErrors should be (true)

			errors = new BindException(command, "command")
			errors.hasErrors should be (false)
			command.mark = "77"
			command.validate(errors)
			errors.hasErrors should be (false)

			errors = new BindException(command, "command")
			errors.hasErrors should be (false)
			command.mark = "77.2"
			command.validate(errors)
			errors.hasErrors should be (true)

		}
	}
}

trait OnlineFeedbackFormCommandTestSupport extends FileAttachmentComponent with FeedbackServiceComponent
	with ZipServiceComponent with SavedFormValueDaoComponent with Mockito {

	this : OnlineFeedbackFormCommand =>

	val fileAttachmentService = mock[FileAttachmentService]
	val feedbackService = mock[FeedbackService]
	val zipService = mock[ZipService]
	val savedFormValueDao = mock[SavedFormValueDao]

	def apply(): Feedback = this.applyInternal()
}