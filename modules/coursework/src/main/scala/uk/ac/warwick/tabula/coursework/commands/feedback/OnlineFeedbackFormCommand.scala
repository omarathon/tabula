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

object OnlineFeedbackFormCommand {
	def apply(module: Module, assignment: Assignment, student: Member, currentUser: CurrentUser) =
		new OnlineFeedbackFormCommand(module, assignment, student, currentUser)
			with ComposableCommand[Feedback]
			with OnlineFeedbackPermissions
			with AutowiringFeedbackServiceComponent
			with AutowiringFileAttachmentServiceComponent
			with AutowiringZipServiceComponent
			with AutowiringSavedFormValueDaoComponent
			with OnlineFeedbackFormDescription {
			override lazy val eventName = "OnlineFeedback"
		}
}

abstract class OnlineFeedbackFormCommand(val module: Module, val assignment: Assignment, val student: Member, val currentUser: CurrentUser)
	extends CommandInternal[Feedback]
	with Appliable[Feedback] with OnlineFeedbackState with OnlineFeedbackStudentState with BindListener with SelfValidating {

	self: FeedbackServiceComponent with SavedFormValueDaoComponent with FileAttachmentServiceComponent with ZipServiceComponent =>

	def submission = assignment.findSubmission(student.universityId)
	def feedback = assignment.findFullFeedback(student.universityId)

	def submissionState = {
		submission match {
			case Some(submission) if submission.isAuthorisedLate => "workflow.Submission.authorisedLate"
			case Some(submission) if submission.isLate => "workflow.Submission.late"
			case Some(_) => "workflow.Submission.onTime"
			case None if !assignment.isClosed => "workflow.Submission.unsubmitted.withinDeadline"
			case None => "workflow.Submission.unsubmitted.late"
		}
	}


	feedback match {
		case Some(f) => copyFrom(f)
		case None => {
			fields = {
				val pairs = assignment.feedbackFields.map { field => field.id -> field.blankFormValue }
				Map(pairs: _*).asJava
			}
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
		if (assignment.collectMarks){
			feedback.actualMark = Some(mark.toInt)
			feedback.actualGrade = Some(grade)
		}

		// save attachments
		if (feedback.attachments != null) {
			val filesToKeep =  Option(attachedFiles).getOrElse(JList()).asScala
			val existingFiles = Option(feedback.attachments).getOrElse(JList()).asScala
			val filesToRemove = existingFiles -- filesToKeep
			filesToRemove.foreach(fileAttachmentService.delete(_))
			feedback.attachments = JArrayList[FileAttachment](filesToKeep)
		}
		feedback.addAttachments(file.attached.asScala)
	}

}


trait OnlineFeedbackStudentState {
	val student: Member
	var mark: String = _
	var grade: String = _
	var fields: JMap[String, FormValue] = _
	var file:UploadedFile = new UploadedFile
	var attachedFiles:JList[FileAttachment] = _
}

trait OnlineFeedbackFormDescription extends Describable[Feedback] {

	this: OnlineFeedbackState with OnlineFeedbackStudentState =>

	def describe(d: Description) {
		// TODO
	}
}