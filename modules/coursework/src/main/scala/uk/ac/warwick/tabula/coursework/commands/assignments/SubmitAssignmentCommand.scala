package uk.ac.warwick.tabula.coursework.commands.assignments

import uk.ac.warwick.tabula.commands._
import uk.ac.warwick.tabula.data.model._

import uk.ac.warwick.tabula.services.AssignmentService
import uk.ac.warwick.tabula.data.Transactions._
import org.joda.time.DateTime
import uk.ac.warwick.tabula.CurrentUser
import org.springframework.validation.Errors
import collection.JavaConversions._
import collection.JavaConverters._
import uk.ac.warwick.tabula.JavaImports._
import java.beans.PropertyEditorSupport
import uk.ac.warwick.util.web.bind.AbstractPropertyEditor
import uk.ac.warwick.tabula.data.model.forms.SubmissionValue
import uk.ac.warwick.tabula.services.ZipService
import uk.ac.warwick.spring.Wire
import uk.ac.warwick.tabula.data.model.MarkingState._
import uk.ac.warwick.tabula.system.BindListener
import uk.ac.warwick.tabula.permissions._
import org.springframework.util.Assert
import org.springframework.validation.BindingResult
import uk.ac.warwick.tabula.services.SubmissionService

class SubmitAssignmentCommand(val module: Module, val assignment: Assignment, val user: CurrentUser) extends Command[Submission] with SelfValidating with BindListener {
	
	mustBeLinked(mandatory(assignment), mandatory(module))
	PermissionCheck(Permissions.Submission.Create, assignment)
	
	var service = Wire[SubmissionService]
	var zipService = Wire[ZipService]

	var fields = buildEmptyFields

	// used as a hint to the view.
	@transient var justSubmitted: Boolean = false

	// just used as a hint to the view.
	@transient var plagiarismDeclaration: Boolean = false

	override def onBind(result:BindingResult) {
		for ((key, field) <- fields) field.onBind(result)
	}

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

	def validate(errors: Errors) {
		if (!assignment.active) {
			errors.reject("assignment.submit.inactive")
		}
		if (!assignment.isOpened()) {
			errors.reject("assignment.submit.notopen")
		}
		if (!assignment.collectSubmissions) {
			errors.reject("assignment.submit.disabled")
		}

		val hasExtension = assignment.isWithinExtension(user.apparentUser.getUserId)

		if (!assignment.allowLateSubmissions && (assignment.isClosed() && !hasExtension)) {
			errors.reject("assignment.submit.closed")
		}
		// HFC-164
		if (assignment.submissions.exists(_.universityId == user.universityId)) {
			if (assignment.allowResubmission) {
				if (assignment.allowLateSubmissions && (assignment.isClosed() && !hasExtension)) {
					errors.reject("assignment.resubmit.closed")
				}
			} else {
				errors.reject("assignment.submit.already")
			}
		}

		if (assignment.displayPlagiarismNotice && !plagiarismDeclaration) {
			errors.rejectValue("plagiarismDeclaration", "assignment.submit.plagiarism")
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

	override def applyInternal() = transactional() {
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
		
		// TAB-413 assert that we have at least one attachment
		Assert.isTrue(
			submission.values.find(value => Option(value.attachments).isDefined && !value.attachments.isEmpty).isDefined, 
			"Submission must have at least one attachment"
		)

		zipService.invalidateSubmissionZip(assignment)
		service.saveSubmission(submission)
		submission
	}

	override def describe(d: Description) = d.assignment(assignment).properties()

	override def describeResult(d: Description, s: Submission) = {
		d.assignment(assignment).properties().property("submission" -> s.id)
		if (s.isNoteworthy)
			d.assignment(assignment).properties().property("submissionIsNoteworthy" -> true)
	}
}