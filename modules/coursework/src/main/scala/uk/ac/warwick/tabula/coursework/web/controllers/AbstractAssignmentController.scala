package uk.ac.warwick.tabula.coursework.web.controllers

import org.springframework.stereotype.Controller
import uk.ac.warwick.tabula.coursework.actions.View
import uk.ac.warwick.tabula.coursework.data.model.Assignment
import org.springframework.beans.factory.annotation.Autowired
import uk.ac.warwick.tabula.coursework.data.FeedbackDao
import uk.ac.warwick.tabula.coursework.data.model.Feedback
import uk.ac.warwick.tabula.CurrentUser
import uk.ac.warwick.tabula.coursework.services.AssignmentService

abstract class AbstractAssignmentController extends BaseController {
	@Autowired var feedbackDao: FeedbackDao = _
	@Autowired var assignmentService: AssignmentService = _

	def checkCanGetFeedback(assignment: Assignment, user: CurrentUser): Option[Feedback] = {
		notDeleted(assignment)
		val feedback = feedbackDao.getFeedbackByUniId(assignment, user.universityId).filter(_.released)

		/*
		 * When feedback has been released and we have some for that user,
		 * we should allow them to view. Otherwise, restrict to those who can
		 * view assignment (those in the defined members group).
		 * 
		 * The check for being able to view feedback is not really necessary given that
		 * we've just explicitly obtained the feedback for the current user.
		 */
		feedback match {
			case Some(feedback) => mustBeAbleTo(View(feedback))
			case None => //mustBeAbleTo(View(assignment))
		}
		feedback
	}
}