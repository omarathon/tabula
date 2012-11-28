package uk.ac.warwick.tabula.coursework.commands.assignments

import scala.collection.JavaConversions.asScalaBuffer
import scala.reflect.BeanProperty

import org.springframework.validation.Errors

import uk.ac.warwick.spring.Wire
import uk.ac.warwick.tabula.commands.Command
import uk.ac.warwick.tabula.commands.Description
import uk.ac.warwick.tabula.commands.SelfValidating
import uk.ac.warwick.tabula.data.FeedbackDao
import uk.ac.warwick.tabula.data.model.Assignment
import uk.ac.warwick.tabula.helpers.ArrayList
import uk.ac.warwick.tabula.services.AssignmentService
import uk.ac.warwick.tabula.services.UserLookupService
import uk.ac.warwick.tabula.services.ZipService
import uk.ac.warwick.userlookup.User

/**
 * Takes a list of student university IDs and deletes either all their submissions, or all their feedback, or both,
 * depending on the value of submissionOrFeedback
 */
class DeleteSubmissionsAndFeedbackCommand(val assignment: Assignment) extends Command[Unit] with SelfValidating {

	var assignmentService = Wire.auto[AssignmentService]
	var zipService = Wire.auto[ZipService]
	var feedbackDao = Wire.auto[FeedbackDao]
	var userLookup = Wire.auto[UserLookupService]
	
    @BeanProperty var students: JList[String] = ArrayList()
    @BeanProperty var submissionOrFeedback: String = ""
	@BeanProperty var confirm: Boolean = false

	var submissionsDeleted = 0
	var feedbacksDeleted = 0
	
	val SubmissionOnly = "submissionOnly"
	val FeedbackOnly = "feedbackOnly"
	val SubmissionAndFeedback = "submissionAndFeedback"
	
	def shouldDeleteSubmissions = ( submissionOrFeedback == SubmissionAndFeedback || submissionOrFeedback == SubmissionOnly )
	def shouldDeleteFeedback = ( submissionOrFeedback == SubmissionAndFeedback || submissionOrFeedback == FeedbackOnly )

	def applyInternal() = {			
		if (shouldDeleteSubmissions) {
			for (uniId <- students; submission <- assignmentService.getSubmissionByUniId(assignment, uniId)) {
				assignmentService.delete(submission)
				submissionsDeleted = submissionsDeleted + 1
			}
			zipService.invalidateSubmissionZip(assignment)
		}

		if (shouldDeleteFeedback) {
			for (uniId <- students; feedback <- feedbackDao.getFeedbackByUniId(assignment, uniId)) {
				feedbackDao.delete(feedback)
				feedbacksDeleted = feedbacksDeleted + 1
			}
			zipService.invalidateFeedbackZip(assignment)
		}
	}

	def prevalidate(errors: Errors) {
		for (uniId <- students; submission <- assignmentService.getSubmissionByUniId(assignment, uniId)) {
			if (submission.assignment != assignment) errors.reject("submission.bulk.wrongassignment")
		}

		for (uniId <- students; feedback <- feedbackDao.getFeedbackByUniId(assignment, uniId)) {
			if (feedback.assignment != assignment) errors.reject("feedback.bulk.wrongassignment")
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
		val studentsAsUsers: JList[User] = ArrayList()
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
