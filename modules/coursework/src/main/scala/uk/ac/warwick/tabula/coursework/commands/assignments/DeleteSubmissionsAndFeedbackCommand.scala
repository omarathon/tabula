package uk.ac.warwick.tabula.coursework.commands.assignments

import scala.collection.JavaConversions.asScalaBuffer
import org.springframework.validation.Errors
import uk.ac.warwick.spring.Wire
import uk.ac.warwick.tabula.commands.Command
import uk.ac.warwick.tabula.commands.Description
import uk.ac.warwick.tabula.commands.SelfValidating
import uk.ac.warwick.tabula.data.FeedbackDao
import uk.ac.warwick.tabula.data.model.Assignment
import uk.ac.warwick.tabula.JavaImports._
import uk.ac.warwick.tabula.services.AssignmentService
import uk.ac.warwick.tabula.services.UserLookupService
import uk.ac.warwick.tabula.services.ZipService
import uk.ac.warwick.userlookup.User
import uk.ac.warwick.tabula.data.model.Module
import uk.ac.warwick.tabula.permissions._
import uk.ac.warwick.tabula.services.FeedbackService
import uk.ac.warwick.tabula.services.SubmissionService

/**
 * Takes a list of student university IDs and deletes either all their submissions, or all their feedback, or both,
 * depending on the value of submissionOrFeedback
 */
class DeleteSubmissionsAndFeedbackCommand(val module: Module, val assignment: Assignment) extends Command[Unit] with SelfValidating {
	
	mustBeLinked(assignment, module)
	PermissionCheck(Permissions.Feedback.Delete, assignment)
	PermissionCheck(Permissions.Submission.Delete, assignment)

	var submissionService = Wire.auto[SubmissionService]
	var feedbackService = Wire.auto[FeedbackService]
	
	var zipService = Wire.auto[ZipService]
	var userLookup = Wire.auto[UserLookupService]
	
    var students: JList[String] = JArrayList()
    var submissionOrFeedback: String = ""
	var confirm: Boolean = false

	var submissionsDeleted = 0
	var feedbacksDeleted = 0
	
	val SubmissionOnly = "submissionOnly"
	val FeedbackOnly = "feedbackOnly"
	val SubmissionAndFeedback = "submissionAndFeedback"
	
	def shouldDeleteSubmissions = ( submissionOrFeedback == SubmissionAndFeedback || submissionOrFeedback == SubmissionOnly )
	def shouldDeleteFeedback = ( submissionOrFeedback == SubmissionAndFeedback || submissionOrFeedback == FeedbackOnly )

	def applyInternal() = {			
		if (shouldDeleteSubmissions) {
			for (uniId <- students; submission <- submissionService.getSubmissionByUniId(assignment, uniId)) {
				submissionService.delete(mandatory(submission))
				submissionsDeleted = submissionsDeleted + 1
			}
			zipService.invalidateSubmissionZip(assignment)
		}

		if (shouldDeleteFeedback) {
			for (uniId <- students; feedback <- feedbackService.getFeedbackByUniId(assignment, uniId)) {
				feedbackService.delete(mandatory(feedback))
				feedbacksDeleted = feedbacksDeleted + 1
			}
			zipService.invalidateFeedbackZip(assignment)
		}
	}

	def prevalidate(errors: Errors) {
		for (uniId <- students; submission <- submissionService.getSubmissionByUniId(assignment, uniId)) {
			if (mandatory(submission).assignment != assignment) errors.reject("submission.bulk.wrongassignment")
		}

		for (uniId <- students; feedback <- feedbackService.getFeedbackByUniId(assignment, uniId)) {
			if (mandatory(feedback).assignment != assignment) errors.reject("feedback.bulk.wrongassignment")
		}
		
		if (!Seq(SubmissionOnly, FeedbackOnly, SubmissionAndFeedback).contains(submissionOrFeedback)) {
			errors.rejectValue("submissionOrFeedback", "invalid")
		}
	}

	def validate(errors: Errors) {
		prevalidate(errors)
		if (!confirm) errors.rejectValue("confirm", "submissionOrFeedback.delete.confirm")
	}
	
	
	def getStudentsAsUsers(): JList[User] = {
		val studentsAsUsers: JList[User] = JArrayList()
		for (student <- students)  {
			val user: User = userLookup.getUserByWarwickUniId(student)
			studentsAsUsers.add(user)
		}
		studentsAsUsers
	}

	override def describe(d: Description) = d
		.assignment(assignment)
		.property("students" -> students)

	override def describeResult(d: Description) = d
		.assignment(assignment)
		.property("submissionsDeleted" -> submissionsDeleted)
		.property("feedbacksDeleted" -> feedbacksDeleted)
}
