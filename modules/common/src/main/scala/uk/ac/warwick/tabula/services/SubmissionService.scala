package uk.ac.warwick.tabula.services

import uk.ac.warwick.tabula.data.model.{SubmissionState, Submission}
import org.springframework.stereotype.Service
import uk.ac.warwick.tabula.data.Daoisms
import uk.ac.warwick.tabula.helpers.Logging

trait SubmissionService {
	def updateState(submission: Submission, state: SubmissionState)
}

@Service(value = "submissionService")
class SubmissionServiceImpl extends SubmissionService with Daoisms with Logging {

	def updateState(submission: Submission, state: SubmissionState){
		if (submission.state != null && !submission.state.canTransitionTo(state))
			throw new IllegalStateException("Cannot transition from " + submission.state + " to " + state)
		submission.state = state
		session.saveOrUpdate(submission)
	}

}
