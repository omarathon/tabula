package uk.ac.warwick.tabula.coursework.commands.feedback

import scala.collection.JavaConverters._
import uk.ac.warwick.tabula.data.model.{Feedback, MarkerFeedback, Member, Assignment, Module}
import uk.ac.warwick.tabula.CurrentUser
import uk.ac.warwick.tabula.commands.{Appliable, CommandInternal, ComposableCommand}
import uk.ac.warwick.tabula.services.{ZipServiceComponent, FileAttachmentComponent, FeedbackServiceComponent, AutowiringZipServiceComponent, AutowiringFileAttachmentServiceComponent, AutowiringFeedbackServiceComponent}
import uk.ac.warwick.tabula.data.AutowiringSavedFormValueDaoComponent
import org.springframework.validation.Errors
import uk.ac.warwick.tabula.data.model.MarkingState.{MarkingCompleted, Rejected}
import uk.ac.warwick.tabula.helpers.StringUtils._
import uk.ac.warwick.tabula.coursework.commands.assignments.FinaliseFeedbackCommand

object OnlineModerationCommand {
	def apply(module: Module, assignment: Assignment, student: Member, currentUser: CurrentUser) =
		new OnlineModerationCommand(module, assignment, student, currentUser)
			with ComposableCommand[MarkerFeedback]
			with OnlineFeedbackFormPermissions
			with MarkerFeedbackStateCopy
			with CopyFromFormFields
			with WriteToFormFields
			with AutowiringFeedbackServiceComponent
			with AutowiringFileAttachmentServiceComponent
			with AutowiringZipServiceComponent
			with AutowiringSavedFormValueDaoComponent
			with OnlineFeedbackFormDescription[MarkerFeedback] {
				override lazy val eventName = "OnlineMarkerFeedback"
			}
}

abstract class OnlineModerationCommand(module: Module, assignment: Assignment, student: Member, currentUser: CurrentUser)
	extends AbstractOnlineFeedbackFormCommand(module, assignment, student, currentUser)
	with CommandInternal[MarkerFeedback] with Appliable[MarkerFeedback] with ModerationState{

	self: FeedbackServiceComponent with FileAttachmentComponent with ZipServiceComponent with MarkerFeedbackStateCopy =>

	def markerFeedback = assignment.getMarkerFeedback(student.universityId, currentUser.apparentUser)

	copyState(markerFeedback, copyModerationFieldsFrom)

	def applyInternal(): MarkerFeedback = {

		// find the parent feedback or make a new one
		val parentFeedback = assignment.feedbacks.asScala.find(_.universityId == student.universityId).getOrElse({
			val newFeedback = new Feedback
			newFeedback.assignment = assignment
			newFeedback.uploaderId = currentUser.apparentId
			newFeedback.universityId = student.universityId
			newFeedback.released = false
			newFeedback
		})

		val firstMarkerFeedback = parentFeedback.retrieveFirstMarkerFeedback
		val secondMarkerFeedback = parentFeedback.retrieveSecondMarkerFeedback
		val markerFeedback = Seq(firstMarkerFeedback, secondMarkerFeedback)

		// if the second-marker feedback is already rejected then do nothing - UI should prevent this
		if(secondMarkerFeedback.state != Rejected){
			if (approved) {
				val finaliseFeedbackCommand = new FinaliseFeedbackCommand(assignment, Seq(firstMarkerFeedback).asJava)
				finaliseFeedbackCommand.apply()

				secondMarkerFeedback.state = MarkingCompleted
			} else {
				markerFeedback.foreach(_.state = Rejected)
				copyModerationFieldsTo(secondMarkerFeedback)
			}

			feedbackService.saveOrUpdate(parentFeedback)
			markerFeedback.foreach(feedbackService.save)
		}
		firstMarkerFeedback
	}

	def copyModerationFieldsFrom(markerFeedback: MarkerFeedback) {

		copyFrom(markerFeedback)

		if(markerFeedback.rejectionComments.hasText) {
			rejectionComments = markerFeedback.rejectionComments
		}

		approved = markerFeedback.state != Rejected
	}

	def copyModerationFieldsTo(markerFeedback: MarkerFeedback) {

		copyTo(markerFeedback)

		if(rejectionComments.hasText) {
			markerFeedback.rejectionComments = rejectionComments
		}
	}

	override def validate(errors: Errors){
		super.validate(errors)

		if (!Option(approved).isDefined)
			errors.rejectValue("approved", "markers.moderation.approved.notDefined")
		else if(!approved && !rejectionComments.hasText)
			errors.rejectValue("rejectionComments", "markers.moderation.rejectionComments.empty")
	}

}

trait ModerationState {
	var approved: Boolean = true
	var rejectionComments: String = _
}
