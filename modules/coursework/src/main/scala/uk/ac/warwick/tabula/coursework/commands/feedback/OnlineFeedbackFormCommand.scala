package uk.ac.warwick.tabula.coursework.commands.feedback

import collection.JavaConverters._
import uk.ac.warwick.tabula.data.model._
import uk.ac.warwick.tabula.commands._
import uk.ac.warwick.tabula.services._
import uk.ac.warwick.tabula.JavaImports._
import uk.ac.warwick.tabula.data.model.forms.{SavedFormValue, FormValue}
import org.springframework.validation.{Errors, BindingResult}
import uk.ac.warwick.tabula.system.BindListener
import uk.ac.warwick.tabula.CurrentUser
import scala.Some
import uk.ac.warwick.tabula.data.{AutowiringSavedFormValueDaoComponent, SavedFormValueDaoComponent}
import uk.ac.warwick.tabula.helpers.StringUtils._
import uk.ac.warwick.tabula.data.model.MarkingState.InProgress

abstract class AbstractOnlineFeedbackFormCommand(val module: Module, val assignment: Assignment, val student: Member, val currentUser: CurrentUser)
	extends OnlineFeedbackState with OnlineFeedbackStudentState with BindListener with SelfValidating {

	def submission = assignment.findSubmission(student.universityId)

	def submissionState = {
		submission match {
			case Some(submission) if submission.isAuthorisedLate => "workflow.Submission.authorisedLate"
			case Some(submission) if submission.isLate => "workflow.Submission.late"
			case Some(_) => "workflow.Submission.onTime"
			case None if !assignment.isClosed => "workflow.Submission.unsubmitted.withinDeadline"
			case None => "workflow.Submission.unsubmitted.late"
		}
	}

	override def onBind(result:BindingResult) {
		for ((key, field) <- fields.asScala) {
			field.onBind(result)
		}
		file.onBind(result)
	}

	override def validate(errors: Errors) {

		// Individually validate all the custom fields
		assignment.feedbackFields.foreach { field =>
			errors.pushNestedPath("fields[%s]".format(field.id))
			fields.asScala.get(field.id).map { field.validate(_, errors) }
			errors.popNestedPath()
		}

		if (mark.hasText) {
			try {
				val asInt = mark.toInt
				if (asInt < 0 || asInt > 100) {
					errors.rejectValue("mark", "actualMark.range")
				}
			} catch {
				case _ @ (_: NumberFormatException | _: IllegalArgumentException) => {
					errors.rejectValue("mark", "actualMark.format")
				}
			}
		}
	}
}


object OnlineFeedbackFormCommand {
	def apply(module: Module, assignment: Assignment, student: Member, currentUser: CurrentUser) =
		new OnlineFeedbackFormCommand(module, assignment, student, currentUser)
			with ComposableCommand[Feedback]
			with OnlineFeedbackPermissions
			with AutowiringFeedbackServiceComponent
			with AutowiringFileAttachmentServiceComponent
			with AutowiringZipServiceComponent
			with AutowiringSavedFormValueDaoComponent
			with OnlineFeedbackFormDescription[Feedback] {
			override lazy val eventName = "OnlineFeedback"
		}
}

abstract class OnlineFeedbackFormCommand(module: Module, assignment: Assignment, student: Member, currentUser: CurrentUser)
	extends AbstractOnlineFeedbackFormCommand(module, assignment, student, currentUser)
	with CommandInternal[Feedback] with Appliable[Feedback] {

	self: FeedbackServiceComponent with SavedFormValueDaoComponent with FileAttachmentServiceComponent with ZipServiceComponent =>

	def feedback = assignment.findFullFeedback(student.universityId)

	feedback match {
		case Some(f) => copyFrom(f)
		case None => {
			fields = {
				val pairs = assignment.feedbackFields.map { field => field.id -> field.blankFormValue }
				Map(pairs: _*).asJava
			}
		}
	}

	def applyInternal(): Feedback = {

		val feedback = assignment.findFeedback(student.universityId).getOrElse({
			val newFeedback = new Feedback
			newFeedback.assignment = assignment
			newFeedback.uploaderId = currentUser.apparentId
			newFeedback.universityId = student.universityId
			newFeedback.released = false
			newFeedback
		})

		copyTo(feedback)

		// if we are updating existing feedback then invalidate any cached feedback zips
		if(feedback.id != null) {
			zipService.invalidateIndividualFeedbackZip(feedback)
			zipService.invalidateFeedbackZip(assignment)
		}

		feedbackService.save(feedback)
		feedback
	}

	def copyFrom(feedback: Feedback) {
		// get custom field values
		fields = {
			val pairs = assignment.feedbackFields.map { field =>
				val currentValue = feedback.customFormValues.asScala.find(_.name == field.name)
				val formValue = currentValue match {
					case Some(initialValue) => field.populatedFormValue(initialValue)
					case None => field.blankFormValue
				}
				field.id -> formValue
			}
			Map(pairs: _*).asJava
		}

		// mark and grade
		if (assignment.collectMarks){
			mark = feedback.actualMark match {
				case Some(m) => m.toString
				case None => ""
			}
			grade = feedback.actualGrade.getOrElse("")
		}

		// get attachments
		attachedFiles = feedback.attachments
	}

	def copyTo(feedback: Feedback) {
		// save custom fields
		feedback.customFormValues = fields.asScala.map {
			case (_, formValue) => {

				def newValue = {
					val newValue = new SavedFormValue()
					newValue.name = formValue.field.name
					newValue.feedback = feedback
					newValue
				}

				// Don't send brand new feedback to the DAO or we'll get a TransientObjectException
				val savedFormValue = if (feedback.id == null) {
					newValue
				} else {
					savedFormValueDao.get(formValue.field, feedback).getOrElse(newValue)
				}

				formValue.persist(savedFormValue)
				savedFormValue
			}
		}.toSet[SavedFormValue].asJava

		// save mark and grade
		if (assignment.collectMarks) {
			Option(mark).foreach(mark => feedback.actualMark = Some(mark.toInt))
			feedback.actualGrade = Some(grade)
		}

		// save attachments
		if (feedback.attachments != null) {
			val filesToKeep =  Option(attachedFiles).getOrElse(JList()).asScala
			val existingFiles = Option(feedback.attachments).getOrElse(JList()).asScala
			val filesToRemove = existingFiles -- filesToKeep
			filesToRemove.foreach(fileAttachmentService.delete)
			feedback.attachments = JArrayList[FileAttachment](filesToKeep)
		}
		feedback.addAttachments(file.attached.asScala)
	}

}

object OnlineMarkerFeedbackFormCommand {
	def apply(module: Module, assignment: Assignment, student: Member, currentUser: CurrentUser) =
		new OnlineMarkerFeedbackFormCommand(module, assignment, student, currentUser)
			with ComposableCommand[MarkerFeedback]
			with OnlineFeedbackPermissions
			with AutowiringFeedbackServiceComponent
			with AutowiringFileAttachmentServiceComponent
			with AutowiringZipServiceComponent
			with AutowiringSavedFormValueDaoComponent
			with OnlineFeedbackFormDescription[MarkerFeedback] {
			override lazy val eventName = "OnlineMarkerFeedback"
		}
}

abstract class OnlineMarkerFeedbackFormCommand(module: Module, assignment: Assignment, student: Member, currentUser: CurrentUser)
	extends AbstractOnlineFeedbackFormCommand(module, assignment, student, currentUser)
	with CommandInternal[MarkerFeedback] with Appliable[MarkerFeedback] {

	self: FeedbackServiceComponent with SavedFormValueDaoComponent with FileAttachmentServiceComponent with ZipServiceComponent =>

	def markerFeedback = assignment.getMarkerFeedback(student.universityId, currentUser.apparentUser)

	markerFeedback match {
		case Some(f) => copyFrom(f)
		case None => {
			fields = {
				val pairs = assignment.feedbackFields.map { field => field.id -> field.blankFormValue }
				Map(pairs: _*).asJava
			}
		}
	}

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

		val firstMarker = assignment.isFirstMarker(currentUser.apparentUser)

		// see if marker feedback already exists - if not create one
		val markerFeedback:MarkerFeedback = firstMarker match {
			case true => parentFeedback.retrieveFirstMarkerFeedback
			case false => parentFeedback.retrieveSecondMarkerFeedback
		}

		copyTo(markerFeedback)
		markerFeedback.state = InProgress

		feedbackService.save(parentFeedback)
		feedbackService.save(markerFeedback)
		markerFeedback
	}

	def copyFrom(markerFeedback: MarkerFeedback) {
		// get custom field values
		fields = {
			val pairs = assignment.feedbackFields.map { field =>
				val currentValue = markerFeedback.customFormValues.asScala.find(_.name == field.name)
				val formValue = currentValue match {
					case Some(initialValue) => field.populatedFormValue(initialValue)
					case None => field.blankFormValue
				}
				field.id -> formValue
			}
			Map(pairs: _*).asJava
		}

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
		// save custom fields
		markerFeedback.customFormValues = fields.asScala.map {
			case (_, formValue) => {

				def newValue = {
					val newValue = new SavedFormValue()
					newValue.name = formValue.field.name
					newValue.markerFeedback = markerFeedback
					newValue
				}

				// Don't send brand new feedback to the DAO or we'll get a TransientObjectException
				val savedFormValue = if (markerFeedback.id == null) {
					newValue
				} else {
					savedFormValueDao.get(formValue.field, markerFeedback).getOrElse(newValue)
				}

				formValue.persist(savedFormValue)
				savedFormValue
			}
		}.toSet[SavedFormValue].asJava

		// save mark and grade
		if (assignment.collectMarks){
			markerFeedback.mark = Some(mark.toInt)
			markerFeedback.grade = Some(grade)
		}

		// save attachments
		if (markerFeedback.attachments != null) {
			val filesToKeep =  Option(attachedFiles).getOrElse(JList()).asScala
			val existingFiles = Option(markerFeedback.attachments).getOrElse(JList()).asScala
			val filesToRemove = existingFiles -- filesToKeep
			filesToRemove.foreach(fileAttachmentService.delete)
			markerFeedback.attachments = JArrayList[FileAttachment](filesToKeep)
		}
		markerFeedback.addAttachments(file.attached.asScala)
	}

}

trait OnlineFeedbackStudentState {
	val student: Member
	val assignment: Assignment
	var mark: String = _
	var grade: String = _
	var fields: JMap[String, FormValue] = _
	var file:UploadedFile = new UploadedFile
	var attachedFiles:JList[FileAttachment] = _
}

trait OnlineFeedbackFormDescription[A] extends Describable[A] {

	this: OnlineFeedbackState with OnlineFeedbackStudentState =>

	def describe(d: Description) {
		d.studentIds(Seq(student.universityId))
		d.assignment(assignment)
	}
}