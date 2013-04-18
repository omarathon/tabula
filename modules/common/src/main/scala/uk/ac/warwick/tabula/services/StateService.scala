package uk.ac.warwick.tabula.services

import uk.ac.warwick.tabula.data.model.{MarkerFeedback, MarkingState, Submission}
import org.springframework.stereotype.Service
import uk.ac.warwick.tabula.data.Daoisms
import uk.ac.warwick.tabula.helpers.Logging
import uk.ac.warwick.tabula.data.Transactions._

trait StateService {
	def updateState(markerFeedback: MarkerFeedback, state: MarkingState)
}

@Service(value = "stateService")
class StateServiceImpl extends StateService with Daoisms with Logging {

	def updateState(markerFeedback: MarkerFeedback, state: MarkingState) = transactional() {
		if (markerFeedback.state != null && !markerFeedback.state.canTransitionTo(state))
			throw new IllegalStateException("Cannot transition from " + markerFeedback.state + " to " + state)
		markerFeedback.state = state
		session.saveOrUpdate(markerFeedback)
	}

}
