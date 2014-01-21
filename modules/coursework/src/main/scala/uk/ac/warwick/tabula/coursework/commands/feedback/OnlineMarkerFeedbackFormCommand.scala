package uk.ac.warwick.tabula.coursework.commands.feedback

import collection.JavaConverters._
import uk.ac.warwick.tabula.data.model.{FileAttachment, Feedback, MarkerFeedback, Assignment, Module}
import uk.ac.warwick.tabula.CurrentUser
import uk.ac.warwick.tabula.commands.{Appliable, CommandInternal, ComposableCommand}
import uk.ac.warwick.tabula.services.{ZipServiceComponent, FileAttachmentServiceComponent, FeedbackServiceComponent, AutowiringZipServiceComponent, AutowiringFileAttachmentServiceComponent, AutowiringFeedbackServiceComponent}
import uk.ac.warwick.tabula.data.AutowiringSavedFormValueDaoComponent
import uk.ac.warwick.tabula.data.model.MarkingState.InProgress
import uk.ac.warwick.tabula.JavaImports._
import uk.ac.warwick.tabula.helpers.StringUtils._
import uk.ac.warwick.userlookup.User

object OnlineMarkerFeedbackFormCommand {
	def apply(module: Module, assignment: Assignment, student: User, currentUser: CurrentUser) =
		new OnlineMarkerFeedbackFormCommand(module, assignment, student, currentUser)
			with ComposableCommand[MarkerFeedback]
			with MarkerFeedbackStateCopy
			with OnlineFeedbackFormPermissions
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

abstract class OnlineMarkerFeedbackFormCommand(module: Module, assignment: Assignment, student: User, currentUser: CurrentUser)
	extends AbstractOnlineFeedbackFormCommand(module, assignment, student, currentUser)
	with CommandInternal[MarkerFeedback] with Appliable[MarkerFeedback] {

	self: FeedbackServiceComponent with ZipServiceComponent 	with MarkerFeedbackStateCopy =>

	def markerFeedback = assignment.getMarkerFeedback(student.getWarwickId, currentUser.apparentUser)

	copyState(markerFeedback)

	def applyInternal(): MarkerFeedback = {

		// find the parent feedback or make a new one
		val parentFeedback = assignment.feedbacks.asScala.find(_.universityId == student.getWarwickId).getOrElse({
			val newFeedback = new Feedback
			newFeedback.assignment = assignment
			newFeedback.uploaderId = currentUser.apparentId
			newFeedback.universityId = student.getWarwickId
			newFeedback.released = false
			newFeedback
		})

		val firstMarker = assignment.isFirstMarker(currentUser.apparentUser)

		// see if marker feedback already exists - if not create one
		val markerFeedback:MarkerFeedback = firstMarker match {
			case true => parentFeedback.retrieveFirstMarkerFeedback
			case false => parentFeedback.retrieveSecondMarkerFeedback
		}

		copyTo(markerFeedback)
		markerFeedback.state = InProgress

		feedbackService.saveOrUpdate(parentFeedback)
		feedbackService.save(markerFeedback)
		markerFeedback
	}

}

trait MarkerFeedbackStateCopy {

	self: OnlineFeedbackState with OnlineFeedbackStudentState with CopyFromFormFields with WriteToFormFields
		with FileAttachmentServiceComponent =>

	/*
		If there is a marker feedback then use the specified copy function to copy it's state to the form object
		if not then set up blank field values
	*/
	def copyState(markerFeedback: Option[MarkerFeedback], copyFunction: MarkerFeedback => Unit): Unit = markerFeedback match {
		case Some(f) => copyFunction(f)
		case None => {
			fields = {
				val pairs = assignment.feedbackFields.map { field => field.id -> field.blankFormValue }
				Map(pairs: _*).asJava
			}
		}
	}

	// when we dont specify a copy function use the one in this trait
	def copyState(markerFeedback: Option[MarkerFeedback]): Unit = copyState(markerFeedback, copyFrom)

	def copyFrom(markerFeedback: MarkerFeedback) {

		copyFormFields(markerFeedback)

		// mark and grade
		if (assignment.collectMarks){
			mark = markerFeedback.mark match {
				case Some(m) => m.toString
				case None => ""
			}
			grade = markerFeedback.grade.getOrElse("")
		}

		// get attachments
		attachedFiles = markerFeedback.attachments
	}

	def copyTo(markerFeedback: MarkerFeedback) {

		saveFormFields(markerFeedback)

		// save mark and grade
		if (assignment.collectMarks) {
			if (mark.hasText) markerFeedback.mark = Some(mark.toInt)
			if (grade.hasText) markerFeedback.grade = Some(grade)
		}

		// save attachments
		if (markerFeedback.attachments != null) {
			val filesToKeep =  Option(attachedFiles).getOrElse(JList()).asScala
			val existingFiles = Option(markerFeedback.attachments).getOrElse(JList()).asScala
			val filesToRemove = existingFiles -- filesToKeep
			fileAttachmentService.deleteAttachments(filesToRemove)
			markerFeedback.attachments = JArrayList[FileAttachment](filesToKeep)
		}
		markerFeedback.addAttachments(file.attached.asScala)
	}
}
