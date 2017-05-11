package uk.ac.warwick.tabula.commands.cm2.feedback

import org.joda.time.DateTime
import org.springframework.validation.{BindingResult, Errors}
import uk.ac.warwick.tabula.CurrentUser
import uk.ac.warwick.tabula.JavaImports.{JList, JMap}
import uk.ac.warwick.tabula.commands.{Describable, Description, SelfValidating, UploadedFile}
import uk.ac.warwick.tabula.data.SavedFormValueDaoComponent
import uk.ac.warwick.tabula.data.model._
import uk.ac.warwick.tabula.data.model.forms.{Extension, FormValue, SavedFormValue, StringFormValue}
import uk.ac.warwick.tabula.services.{GeneratesGradesFromMarks, ProfileServiceComponent}
import uk.ac.warwick.tabula.system.BindListener
import uk.ac.warwick.userlookup.User

import collection.JavaConverters._
import uk.ac.warwick.tabula.helpers.StringUtils._
import uk.ac.warwick.tabula.permissions.Permissions
import uk.ac.warwick.tabula.system.permissions.{PermissionsChecking, RequiresPermissionsChecking}

import scala.util.Try

//FIXME - break out into cake ingredients
trait OnlineFeedbackCommand extends BindListener with SelfValidating {

	this: ProfileServiceComponent with OnlineFeedbackState =>

	override def onBind(result:BindingResult) {
		if (fields != null) {
			for ((key, field) <- fields.asScala) {
				field.onBind(result)
			}
		}
		file.onBind(result)
	}

	override def validate(errors: Errors) {
		if(!hasContent) {
			errors.reject("feedback.empty")
		}

		fieldValidation(errors)
	}

	private def fieldValidation(errors:Errors) {
		// Individually validate all the custom fields
		if(fields != null){
			assignment.feedbackFields.foreach { field =>
				errors.pushNestedPath("fields[%s]".format(field.id))
				fields.asScala.get(field.id).foreach(field.validate(_, errors))
				errors.popNestedPath()
			}
		}

		if (mark.hasText) {
			try {
				val asInt = mark.toInt
				if (asInt < 0 || asInt > 100) {
					errors.rejectValue("mark", "actualMark.range")
				}
			} catch {
				case _ @ (_: NumberFormatException | _: IllegalArgumentException) =>
					errors.rejectValue("mark", "actualMark.format")
			}
		}

		// validate grade is department setting is true
		if (!errors.hasErrors && grade.hasText && module.adminDepartment.assignmentGradeValidation) {
			val validGrades = Try(mark.toInt).toOption.toSeq.flatMap { m => gradeGenerator.applyForMarks(Map(student.getWarwickId -> m))(student.getWarwickId) }
			if (validGrades.nonEmpty && !validGrades.exists(_.grade == grade)) {
				errors.rejectValue("grade", "actualGrade.invalidSITS", Array(validGrades.map(_.grade).mkString(", ")), "")
			}
		}
	}

}

trait CopyFromFormFields {

	self: OnlineFeedbackState with SavedFormValueDaoComponent =>

	def copyFormFields(markerFeedback: MarkerFeedback){
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
	}

}

trait WriteToFormFields {

	self: OnlineFeedbackState with SavedFormValueDaoComponent =>

	def saveFormFields(markerFeedback: MarkerFeedback) {
		// save custom fields
		markerFeedback.customFormValues = fields.asScala.map {
			case (_, formValue) =>

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
		}.toSet[SavedFormValue].asJava
	}

}

trait OnlineFeedbackPermissions extends RequiresPermissionsChecking {

	self: OnlineFeedbackState =>

	def permissionsCheck(p: PermissionsChecking) {
		p.mustBeLinked(assignment, module)
		p.PermissionCheck(Permissions.AssignmentMarkerFeedback.Manage, assignment)
		if(submitter.apparentUser != marker) {
			p.PermissionCheck(Permissions.Assignment.MarkOnBehalf, assignment)
		}
	}
}

trait OnlineFeedbackDescription[A] extends Describable[A] {

	this: OnlineFeedbackState =>

	def describe(d: Description) {
		d.studentIds(Option(student.getWarwickId).toSeq)
		d.studentUsercodes(student.getUserId)
		d.assignment(assignment)
	}
}

trait OnlineFeedbackState {
	def module: Module
	def assignment: Assignment
	def student: User
	def marker: User
	def gradeGenerator: GeneratesGradesFromMarks
	def submitter: CurrentUser

	var mark: String = _
	var grade: String = _
	var fields: JMap[String, FormValue] = _
	var file:UploadedFile = new UploadedFile
	var attachedFiles:JList[FileAttachment] = _

	private def fieldHasValue = fields.asScala.exists{ case (_, value: StringFormValue) => value.value.hasText }
	private def hasFile = Option(attachedFiles).exists(!_.isEmpty) || Option(file).exists(!_.attachedOrEmpty.isEmpty)
	def hasContent: Boolean = mark.hasText || grade.hasText || hasFile || fieldHasValue
}

trait SubmissionState {

	self: ProfileServiceComponent =>

	def assignment: Assignment
	def submission: Option[Submission]
	def student: User

	def submissionState: String = {
		submission match {
			case Some(s) if s.isAuthorisedLate => "workflow.Submission.authorisedLate"
			case Some(s) if s.isLate => "workflow.Submission.late"
			case Some(_) => "workflow.Submission.onTime"
			case None if !assignment.isClosed => "workflow.Submission.unsubmitted.withinDeadline"
			case None if assignment.extensions.asScala.exists(e => e.usercode == student.getUserId && e.expiryDate.exists(_.isBeforeNow))
			=> "workflow.Submission.unsubmitted.withinExtension"
			case None => "workflow.Submission.unsubmitted.late"
		}
	}

	def disability: Option[Disability] = submission.filter(_.useDisability).flatMap(_ =>
		profileService
			.getMemberByUniversityId(student.getWarwickId)
			.collect{case s:StudentMember => s}
			.flatMap(_.disability)
	)

}

trait ExtensionState {
	def assignment: Assignment
	def extension: Option[Extension]

	def extensionState: String = extension match {
		case Some(e) if e.rejected || e.revoked => "workflow.Extension.requestDenied"
		case Some(e) if e.approved => "workflow.Extension.granted"
		case Some(e) if !e.isManual => "workflow.Extension.requested"
		case _ => "workflow.Extension.none"
	}

	def extensionDate: Option[DateTime] = extension.flatMap(e => e.expiryDate.orElse(e.requestedExpiryDate))
}