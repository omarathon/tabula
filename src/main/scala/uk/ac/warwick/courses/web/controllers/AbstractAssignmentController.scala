package uk.ac.warwick.courses.web.controllers

import org.springframework.stereotype.Controller
import uk.ac.warwick.courses.actions.View
import uk.ac.warwick.courses.data.model.Assignment
import org.springframework.beans.factory.annotation.Autowired
import uk.ac.warwick.courses.data.FeedbackDao
import uk.ac.warwick.courses.data.model.Feedback
import uk.ac.warwick.courses.CurrentUser


abstract class AbstractAssignmentController extends BaseController {
	@Autowired var feedbackDao:FeedbackDao =_
	
	def checkCanGetFeedback(assignment:Assignment, user:CurrentUser): Option[Feedback] = {
		val feedback = (if (assignment.resultsPublished) 
							  feedbackDao.getFeedbackByUniId(assignment, user.universityId)
						   else None)
		
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
			case None => mustBeAbleTo(View(assignment))
		}
		feedback
	}
}