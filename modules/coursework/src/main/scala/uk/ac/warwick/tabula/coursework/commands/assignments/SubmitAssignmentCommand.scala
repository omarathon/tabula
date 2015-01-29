package uk.ac.warwick.tabula.coursework.commands.assignments

import org.apache.commons.collections.Factory
import org.apache.commons.collections.map.LazyMap
import org.joda.time.DateTime
import org.springframework.util.Assert
import org.springframework.validation.{BindingResult, Errors}
import uk.ac.warwick.spring.Wire
import uk.ac.warwick.tabula.CurrentUser
import uk.ac.warwick.tabula.commands._
import uk.ac.warwick.tabula.data.Transactions._
import uk.ac.warwick.tabula.data.model._
import uk.ac.warwick.tabula.data.model.forms.{FormValue, SavedFormValue}
import uk.ac.warwick.tabula.data.model.notifications.coursework.{SubmissionDueGeneralNotification, SubmissionDueWithExtensionNotification, SubmissionReceiptNotification, SubmissionReceivedNotification}
import uk.ac.warwick.tabula.permissions._
import uk.ac.warwick.tabula.services.attendancemonitoring.AttendanceMonitoringCourseworkSubmissionService
import uk.ac.warwick.tabula.services.{MonitoringPointProfileTermAssignmentService, SubmissionService, ZipService}
import uk.ac.warwick.tabula.system.BindListener

import scala.collection.JavaConverters._

class SubmitAssignmentCommand(
	val module: Module,
	val assignment: Assignment,
	val user: CurrentUser
) extends Command[Submission] with SelfValidating with BindListener
	with Notifies[Submission, Submission] with CompletesNotifications[Submission] {

	mustBeLinked(mandatory(assignment), mandatory(module))
	PermissionCheck(Permissions.Submission.Create, assignment)

	var service = Wire.auto[SubmissionService]
	var zipService = Wire.auto[ZipService]
	var monitoringPointProfileTermAssignmentService = Wire.auto[MonitoringPointProfileTermAssignmentService]
	var attendanceMonitoringCourseworkSubmissionService = Wire.auto[AttendanceMonitoringCourseworkSubmissionService]

	var fields = buildEmptyFields

	var plagiarismDeclaration: Boolean = false

	// used as a hint to the view.
	var justSubmitted: Boolean = false

	override def onBind(result:BindingResult) {
		for ((key, field) <- fields.asScala) field.onBind(result)
	}

	/**
	 * Goes through the assignment's fields building a set of empty FormValue
	 * objects that can be attached to the form and used for binding form values.
	 * The key is the form field's ID, so binding should be impervious to field reordering,
	 * though it will fail if a field is removed between a user loading a submission form
	 * and submitting it.
	 */
	private def buildEmptyFields: JMap[String, FormValue] = {
		val fields = JHashMap(assignment.submissionFields.map { field => field.id -> field.blankFormValue }.toMap)
		
		LazyMap.decorate(fields, new Factory {
			def create() = new FormValue {
				val field = null
				def persist(value: SavedFormValue) {}
			}
		}).asInstanceOf[JMap[String, FormValue]]
	}

	def validate(errors: Errors) {
		if (!assignment.active) {
			errors.reject("assignment.submit.inactive")
		}
		if (!assignment.isOpened) {
			errors.reject("assignment.submit.notopen")
		}
		if (!assignment.collectSubmissions) {
			errors.reject("assignment.submit.disabled")
		}

		val hasExtension = assignment.isWithinExtension(user.apparentUser)

		if (!assignment.allowLateSubmissions && (assignment.isClosed && !hasExtension)) {
			errors.reject("assignment.submit.closed")
		}
		// HFC-164
		if (assignment.submissions.asScala.exists(_.universityId == user.universityId)) {
			if (assignment.allowResubmission) {
				if (assignment.allowLateSubmissions && (assignment.isClosed && !hasExtension)) {
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
		assignment.submissionFields.foreach { field =>
			errors.pushNestedPath("fields[%s]".format(field.id))
			fields.asScala.get(field.id).map { field.validate(_, errors) }
			errors.popNestedPath()
		}

	}

	override def applyInternal() = transactional() {
		assignment.submissions.asScala.find(_.isForUser(user.apparentUser)).map { existingSubmission =>
			if (assignment.resubmittable(user.apparentUser)) {
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

		submission.values = fields.asScala.map {
			case (_, submissionValue) =>
				val value = new SavedFormValue()
				value.name = submissionValue.field.name
				value.submission = submission
				submissionValue.persist(value)
				value
		}.toSet[SavedFormValue].asJava

		// TAB-413 assert that we have at least one attachment
		Assert.isTrue(
			submission.values.asScala.exists(value => Option(value.attachments).isDefined && !value.attachments.isEmpty),
			"Submission must have at least one attachment"
		)

		zipService.invalidateSubmissionZip(assignment)
		service.saveSubmission(submission)
		monitoringPointProfileTermAssignmentService.updateCheckpointsForSubmission(submission)
		if (features.attendanceMonitoringAcademicYear2014)
			attendanceMonitoringCourseworkSubmissionService.updateCheckpoints(submission)
		submission
	}

	override def describe(d: Description) =	{
		d.assignment(assignment)
		
		assignment.submissions.asScala.find(_.universityId == user.universityId).map { existingSubmission =>
			d.properties(
				"existingSubmission" -> existingSubmission.id,
				"existingAttachments" -> existingSubmission.allAttachments.map { _.id }
			)
		}
	}

	override def describeResult(d: Description, s: Submission) = {
		d.assignment(assignment).properties("submission" -> s.id).fileAttachments(s.allAttachments)
		if (s.isNoteworthy)
			d.properties("submissionIsNoteworthy" -> true)
	}

	def emit(submission: Submission) = {
		Seq(
			Notification.init(new SubmissionReceiptNotification, user.apparentUser, Seq(submission), assignment),
			Notification.init(new SubmissionReceivedNotification, user.apparentUser, Seq(submission), assignment)
		)
	}

	def notificationsToComplete(commandResult: Submission): CompletesNotificationsResult = {
		CompletesNotificationsResult(
			notificationService.findActionRequiredNotificationsByEntityAndType[SubmissionDueGeneralNotification](assignment) ++
				assignment.findExtension(user.universityId).map(
					notificationService.findActionRequiredNotificationsByEntityAndType[SubmissionDueWithExtensionNotification]
				).getOrElse(Seq()),
			user.apparentUser
		)
	}
}