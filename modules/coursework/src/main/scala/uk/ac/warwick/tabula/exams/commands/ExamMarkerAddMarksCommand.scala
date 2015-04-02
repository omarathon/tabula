package uk.ac.warwick.tabula.exams.commands

import uk.ac.warwick.tabula.CurrentUser
import uk.ac.warwick.tabula.commands._
import uk.ac.warwick.tabula.coursework.services.docconversion.AutowiringMarksExtractorComponent
import uk.ac.warwick.tabula.data.HibernateHelpers
import uk.ac.warwick.tabula.data.model._
import uk.ac.warwick.tabula.data.model.notifications.coursework.FeedbackChangeNotification
import uk.ac.warwick.tabula.permissions.Permissions
import uk.ac.warwick.tabula.services._
import uk.ac.warwick.tabula.system.permissions.{PermissionsChecking, PermissionsCheckingMethods, RequiresPermissionsChecking}

import uk.ac.warwick.tabula.coursework.commands.assignments._

object ExamMarkerAddMarksCommand {
	def apply(module: Module, assessment: Assessment, submitter: CurrentUser, gradeGenerator: GeneratesGradesFromMarks) =
		new AdminAddMarksCommandInternal(module, assessment, submitter, gradeGenerator)
			with AutowiringFeedbackServiceComponent
			with AutowiringUserLookupComponent
			with AutowiringMarksExtractorComponent
			with ComposableCommand[Seq[Feedback]]
			with MarkerAddMarksDescription
			with MarkerAddMarksPermissions
			with AdminAddMarksCommandValidation
			with AdminAddMarksNotifications
			with AdminAddMarksCommandState
			with PostExtractValidation
			with AddMarksCommandBindListener
}

trait MarkerAddMarksDescription extends Describable[Seq[Feedback]] {

	self: AdminAddMarksCommandState =>

	override lazy val eventName = "MarkerAddMarks"

	override def describe(d: Description) {
		assessment match {
			case assignment: Assignment => d.assignment(assignment)
			case exam: Exam => d.exam(exam)
		}

	}
}

trait AdminAddMarksNotifications extends Notifies[Seq[Feedback], Feedback] {
	
	self: AdminAddMarksCommandState =>
	
	def emit(updatedFeedback: Seq[Feedback]) = updatedReleasedFeedback.flatMap { feedback => HibernateHelpers.initialiseAndUnproxy(feedback) match {
		case assignmentFeedback: AssignmentFeedback =>
			Option(Notification.init(new FeedbackChangeNotification, submitter.apparentUser, assignmentFeedback, assignmentFeedback.assignment))
		case _ =>
			None
	}}
}

trait MarkerAddMarksPermissions extends RequiresPermissionsChecking with PermissionsCheckingMethods {

	self: AdminAddMarksCommandState =>

	override def permissionsCheck(p: PermissionsChecking) {
		p.mustBeLinked(assessment, module)
		p.PermissionCheck(Permissions.Feedback.Create, assessment)
	}

}