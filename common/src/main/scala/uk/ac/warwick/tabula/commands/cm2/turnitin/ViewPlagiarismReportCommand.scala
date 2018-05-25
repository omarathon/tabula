package uk.ac.warwick.tabula.commands.cm2.turnitin

import org.springframework.validation.Errors
import uk.ac.warwick.spring.Wire
import uk.ac.warwick.tabula.CurrentUser
import uk.ac.warwick.tabula.commands._
import uk.ac.warwick.tabula.data.model.notifications.coursework.TurnitinJobSuccessNotification
import uk.ac.warwick.tabula.data.model.{Assignment, FileAttachment}
import uk.ac.warwick.tabula.events.NotificationHandling
import uk.ac.warwick.tabula.helpers.Logging
import uk.ac.warwick.tabula.helpers.StringUtils._
import uk.ac.warwick.tabula.permissions.Permissions
import uk.ac.warwick.tabula.services.turnitinlti.{AutowiringTurnitinLtiServiceComponent, TurnitinLtiServiceComponent}
import uk.ac.warwick.tabula.services.{AutowiringOriginalityReportServiceComponent, OriginalityReportServiceComponent}
import uk.ac.warwick.tabula.services.turnitin.{GotSubmissions, Turnitin}
import uk.ac.warwick.tabula.system.permissions.{PermissionsChecking, PermissionsCheckingMethods, RequiresPermissionsChecking}
import uk.ac.warwick.userlookup.User
import uk.ac.warwick.util.web.Uri

object ViewPlagiarismReportCommand {
	type CommandType = Appliable[Either[Uri, TurnitinReportError]] with ViewPlagiarismReportRequest with SelfValidating

	def apply(assignment: Assignment, attachment: FileAttachment, isInstructor: Boolean): CommandType =
		new ViewPlagiarismReportCommandInternal(assignment, attachment, isInstructor)
			with ComposableCommand[Either[Uri, TurnitinReportError]]
			with ViewPlagiarismReportPermissions
			with ViewPlagiarismReportValidation
			with ReadOnly with Unaudited
			with AutowiringTurnitinApiComponent
			with AutowiringTurnitinLtiServiceComponent
			with AutowiringOriginalityReportServiceComponent

	def apply(assignment: Assignment, attachment: FileAttachment, isInstructor: Boolean, currentUser: CurrentUser): CommandType =
		new ViewPlagiarismReportCommandInternal(assignment, attachment, isInstructor, currentUser)
			with ComposableCommand[Either[Uri, TurnitinReportError]]
			with ViewPlagiarismReportPermissions
			with ViewPlagiarismReportValidation
			with CompletesViewPlagiarismReportNotifications
			with ReadOnly with Unaudited
			with AutowiringTurnitinApiComponent
			with AutowiringTurnitinLtiServiceComponent
			with AutowiringOriginalityReportServiceComponent
}

trait ViewPlagiarismReportState {
	def assignment: Assignment
	def attachment: FileAttachment
	var ltiParams: Map[String, String] = Map()
	var ltiEndpoint: String = _
}

trait ViewPlagiarismReportRequest extends ViewPlagiarismReportState {
	self: ViewPlagiarismReportState =>

	var viewer: User = _
}

class ViewPlagiarismReportCommandInternal(val assignment: Assignment, val attachment: FileAttachment, val isInstructor: Boolean)
	extends CommandInternal[Either[Uri, TurnitinReportError]] with ViewPlagiarismReportRequest with Logging {
	self: TurnitinApiComponent with TurnitinLtiServiceComponent =>

	def this(assignment: Assignment, attachment: FileAttachment, isInstructor: Boolean, user: CurrentUser) {
		this(assignment, attachment)

		viewer = user.apparentUser
	}

	override def applyInternal(): Either[Uri, TurnitinReportError with Product with Serializable] = {

		if (attachment.originalityReport.turnitinId.hasText) {
			//LTI
			ltiEndpoint = turnitinLtiService.getOriginalityReportEndpoint(attachment)

			ltiParams = turnitinLtiService.getOriginalityReportParams(
				endpoint = ltiEndpoint,
				assignment = assignment,
				attachment = attachment,
				userId = viewer.getUserId,
				email = {
					if(isInstructor) viewer.getEmail
					else viewer.getUserId + "@TurnitinLti.warwick.ac.uk"
				},
				firstName = viewer.getFirstName,
				lastName = viewer.getLastName,
				isInstructor = isInstructor
			)
			Left(Uri.parse(ltiEndpoint))
		} else if (!isInstructor) {
			// only doing LTI for student view atm
			Right(TurnitinReportError.NoSessionError)
		} else {

			debug("Getting document viewer URL for FileAttachment %s", attachment.id)

			api.login(viewer.getEmail, viewer.getFirstName, viewer.getLastName) match {
				case Some(session) =>

					val classId = Turnitin.classIdFor(assignment, api.classPrefix)
					val className = Turnitin.classNameFor(assignment)
					val assignmentId = Turnitin.assignmentIdFor(assignment)
					val assignmentName = Turnitin.assignmentNameFor(assignment)
					session.listSubmissions(classId, className, assignmentId, assignmentName) match {
						case GotSubmissions(list) =>
							val matchingObject = list.find { _.title == attachment.id }
							val objectId = matchingObject.map { _.objectId }
							objectId match {
								case Some(id) =>
									debug("Found objectID %s for FileAttachment %s", id, attachment.id)
									val link = session.getDocumentViewerLink(id)
									debug("Redirecting to %s for FileAttachment %s", link, attachment.id)
									Left(link)
								case None =>
									Right(TurnitinReportError.NoObjectError)
							}
						case what => Right(TurnitinReportError.ApiError(what.message))
					}
				case None => Right(TurnitinReportError.NoSessionError)
			}
		}
	}

}

trait ViewPlagiarismReportValidation extends SelfValidating {
	self: ViewPlagiarismReportState with ViewPlagiarismReportRequest =>

	override def validate(errors: Errors): Unit = {
		if (viewer == null || !viewer.isFoundUser) errors.rejectValue("viewer", "NotEmpty")

		if (attachment.originalityReport == null) {
			errors.reject("fileattachment.originalityReport.empty")
		}
	}
}

trait ViewPlagiarismReportPermissions extends RequiresPermissionsChecking with PermissionsCheckingMethods {
	self: ViewPlagiarismReportState =>

	override def permissionsCheck(p: PermissionsChecking) {
		p.PermissionCheck(Permissions.Submission.ViewPlagiarismStatus, assignment)
	}
}

trait CompletesViewPlagiarismReportNotifications extends CompletesNotifications[Either[Uri, TurnitinReportError]] {
	self: ViewPlagiarismReportRequest with NotificationHandling with OriginalityReportServiceComponent =>

	override def notificationsToComplete(commandResult: Either[Uri, TurnitinReportError]): CompletesNotificationsResult = {
		commandResult match {
			case Left(_) =>
				originalityReportService.getOriginalityReportByFileId(attachment.id).map(report =>
					CompletesNotificationsResult(
						notificationService.findActionRequiredNotificationsByEntityAndType[TurnitinJobSuccessNotification](report),
						viewer
					)
				).getOrElse(EmptyCompletesNotificationsResult)
			case Right(_) =>
				EmptyCompletesNotificationsResult
		}
	}

}

sealed abstract class TurnitinReportError(val code: String)
trait TurnitinReportErrorWithMessage {
	self: TurnitinReportError =>

	def message: String
}

object TurnitinReportError {
	case object NoObjectError extends TurnitinReportError("no-object")
	case object NoSessionError extends TurnitinReportError("no-session")
	case class ApiError(message: String) extends TurnitinReportError("api-error") with TurnitinReportErrorWithMessage
}

trait AutowiringTurnitinApiComponent  extends TurnitinApiComponent {
	var api: Turnitin = Wire[Turnitin]
}

trait TurnitinApiComponent {
	def api: Turnitin
}