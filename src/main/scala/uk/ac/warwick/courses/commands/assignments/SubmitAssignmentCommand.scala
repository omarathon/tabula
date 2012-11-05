package uk.ac.warwick.courses.commands.assignments

import uk.ac.warwick.courses.commands._
import uk.ac.warwick.courses.data.model._
import org.springframework.beans.factory.annotation.Autowired
import uk.ac.warwick.courses.services.AssignmentService
import uk.ac.warwick.courses.data.Transactions._
import org.joda.time.DateTime
import uk.ac.warwick.courses.CurrentUser
import org.springframework.validation.Errors
import collection.JavaConversions._
import collection.JavaConverters._
import uk.ac.warwick.courses.JavaImports._
import scala.reflect.BeanProperty
import java.beans.PropertyEditorSupport
import uk.ac.warwick.util.web.bind.AbstractPropertyEditor
import uk.ac.warwick.courses.data.model.forms.SubmissionValue
import org.springframework.beans.factory.annotation.Configurable
import uk.ac.warwick.courses.services.ZipService

@Configurable
class SubmitAssignmentCommand(val assignment: Assignment, val user: CurrentUser) extends Command[Submission] with SelfValidating {

	@Autowired var service: AssignmentService = _
	@Autowired var zipService: ZipService = _

	@BeanProperty var fields = buildEmptyFields

	// not important to command - only used to bind to request.
	@transient @BeanProperty var module: Module = _

	// used as a hint to the view.
	@transient @BeanProperty var justSubmitted: Boolean = false

	// just used as a hint to the view.
	@transient @BeanProperty var plagiarismDeclaration: Boolean = false

	def onBind: Unit = for ((key, field) <- fields) field.onBind

	/**
	 * Goes through the assignment's fields building a set of empty SubmissionValue
	 * objects that can be attached to the form and used for binding form values.
	 * The key is the form field's ID, so binding should be impervious to field reordering,
	 * though it will fail if a field is removed between a user loading a submission form
	 * and submitting it.
	 */
	private def buildEmptyFields: JMap[String, SubmissionValue] = {
		val pairs = assignment.fields.map { field => field.id -> field.blankSubmissionValue.asInstanceOf[SubmissionValue] }
		Map(pairs: _*)
	}

	def validate(implicit errors: Errors) {
		if (!assignment.active) {
			reject("assignment.submit.inactive")
		}
		if (!assignment.isOpened()) {
			reject("assignment.submit.notopen")
		}
		if (!assignment.collectSubmissions) {
			reject("assignment.submit.disabled")
		}

		val hasExtension = assignment.isWithinExtension(user.apparentUser.getUserId)

		if (!assignment.allowLateSubmissions && (assignment.isClosed() && !hasExtension)) {
			reject("assignment.submit.closed")
		}
		// HFC-164
		if (assignment.submissions.exists(_.universityId == user.universityId)) {
			if (assignment.allowResubmission) {
				if (assignment.allowLateSubmissions && (assignment.isClosed() && !hasExtension)) {
					reject("assignment.resubmit.closed")
				}
			} else {
				reject("assignment.submit.already")
			}
		}

		if (assignment.displayPlagiarismNotice && !plagiarismDeclaration) {
			rejectValue("plagiarismDeclaration", "assignment.submit.plagiarism")
		}

		// TODO for multiple attachments, check filenames are unique 	

		// Individually validate all the custom fields
		// If a submitted ID is not found in assignment, it's ignored.
		assignment.fields.foreach { field =>
			errors.pushNestedPath("fields[%s]".format(field.id))
			fields.asScala.get(field.id).map { field.validate(_, errors) }
			errors.popNestedPath()
		}

	}

	override def work = transactional() {
		assignment.submissions.find(_.universityId == user.universityId).map { existingSubmission =>
			if (assignment.resubmittable(user.apparentId)) {
				service.delete(existingSubmission)
			} else { // Validation should prevent ever reaching here.
				throw new IllegalArgumentException("Submission already exists and can't overwrite it")
			}
		}

		val submission = new Submission
		submission.assignment = assignment
		submission.submitted = true
		submission.submittedDate = new DateTime
		submission.userId = user.apparentUser.getUserId
		submission.universityId = user.apparentUser.getWarwickId

		submission.values = fields.map {
			case (_, submissionValue) =>
				val value = new SavedSubmissionValue()
				value.name = submissionValue.field.name
				value.submission = submission
				submissionValue.persist(value)
				value
		}.toSet[SavedSubmissionValue]

		zipService.invalidateSubmissionZip(assignment)
		service.saveSubmission(submission)
		submission
	}

	override def describe(d: Description) = d.assignment(assignment).properties()

}