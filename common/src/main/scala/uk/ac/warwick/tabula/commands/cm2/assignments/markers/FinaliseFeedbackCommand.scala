package uk.ac.warwick.tabula.commands.cm2.assignments.markers

import org.joda.time.DateTime
import uk.ac.warwick.tabula.commands._
import uk.ac.warwick.tabula.data.{AutowiringFileDaoComponent, FileDaoComponent}
import uk.ac.warwick.tabula.data.model.forms.SavedFormValue
import uk.ac.warwick.tabula.data.model._
import uk.ac.warwick.tabula.data.model.notifications.cm2.CM2FinaliseFeedbackNotification
import uk.ac.warwick.tabula.permissions.Permissions
import uk.ac.warwick.tabula.services.{AutowiringZipServiceComponent, ZipServiceComponent}
import uk.ac.warwick.tabula.system.permissions.{PermissionsChecking, PermissionsCheckingMethods, RequiresPermissionsChecking}
import uk.ac.warwick.userlookup.User

import scala.collection.JavaConversions._

/**
 * Copies the appropriate MarkerFeedback item to its parent Feedback ready for processing by administrators
 */
object FinaliseFeedbackCommand {
	def apply(assignment: Assignment, markerFeedback: Seq[MarkerFeedback], user: User) =
		new FinaliseFeedbackCommandInternal(assignment, markerFeedback, user)
			with ComposableCommand[Seq[Feedback]]
			with FinaliseFeedbackPermissions
			with FinaliseFeedbackDescription
			with FinaliseFeedbackNotifier
			with AutowiringZipServiceComponent
			with AutowiringFileDaoComponent
}

trait FinaliseFeedbackCommandState {
	def assignment: Assignment
	def markerFeedback: Seq[MarkerFeedback]
}

abstract class FinaliseFeedbackCommandInternal(val assignment: Assignment, val markerFeedback: Seq[MarkerFeedback], val user: User)
	extends CommandInternal[Seq[Feedback]] with FinaliseFeedbackCommandState with UserAware {

	this: FileDaoComponent with ZipServiceComponent =>

	override def applyInternal(): Seq[Feedback] = {
		markerFeedback.map(copyToFeedback)
	}
S
	private def copyToFeedback(markerFeedback: MarkerFeedback): Feedback = {
		val parent = markerFeedback.feedback

		parent.clearCustomFormValues()

		// save custom fields
		parent.customFormValues.addAll(markerFeedback.customFormValues.map { formValue =>
			val newValue = new SavedFormValue()
			newValue.name = formValue.name
			newValue.feedback = formValue.markerFeedback.feedback
			newValue.value = formValue.value
			newValue
		}.toSet[SavedFormValue])

		parent.actualGrade = markerFeedback.grade
		parent.actualMark = markerFeedback.mark

		parent.updatedDate = DateTime.now

		// erase any existing attachments - these will be replaced
		parent.clearAttachments()

		markerFeedback.attachments.foreach(parent.addAttachment)
		zipService.invalidateIndividualFeedbackZip(parent)
		parent
	}
}

trait FinaliseFeedbackPermissions extends RequiresPermissionsChecking with PermissionsCheckingMethods {
	self: FinaliseFeedbackCommandState =>

	override def permissionsCheck(p: PermissionsChecking) {
		p.PermissionCheck(Permissions.AssignmentMarkerFeedback.Manage, mandatory(assignment))
	}
}

trait FinaliseFeedbackDescription extends Describable[Seq[Feedback]] {
	self: FinaliseFeedbackCommandState =>

	override def describe(d: Description) {
		d.assignment(assignment)
		d.property("updatedFeedback" -> markerFeedback.size)
	}

}

trait FinaliseFeedbackNotifier extends Notifies[Seq[Feedback], Seq[Feedback]] {
	self: FinaliseFeedbackCommandState with UserAware =>

	override def emit(feedbacks: Seq[Feedback]): Seq[Notification[Feedback, Assignment]] = {
		Seq(Notification.init(new CM2FinaliseFeedbackNotification, user, feedbacks.filterNot { _.checkedReleased }, assignment))
	}
}

trait FinaliseFeedbackComponent {
	def finaliseFeedback(assignment: Assignment, markerFeedback: Seq[MarkerFeedback])
}

trait FinaliseFeedbackComponentImpl extends FinaliseFeedbackComponent {

	val marker: User

	def finaliseFeedback(assignment: Assignment, markerFeedback: Seq[MarkerFeedback]) {
		val finaliseFeedbackCommand = FinaliseFeedbackCommand(assignment, markerFeedback, marker)
		finaliseFeedbackCommand.apply()
	}
}