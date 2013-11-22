package uk.ac.warwick.tabula.services

import uk.ac.warwick.tabula.data.model.{MarkerFeedback, MarkingState}
import org.springframework.stereotype.Service
import uk.ac.warwick.tabula.data.{SessionComponent, Daoisms}

trait StateService {
	def updateState(markerFeedback: MarkerFeedback, state: MarkingState)
}

@Service(value = "stateService")
class StateServiceImpl extends ComposableStateServiceImpl with Daoisms

class ComposableStateServiceImpl extends StateService {
	this:SessionComponent =>

	def updateState(markerFeedback: MarkerFeedback, state: MarkingState) {
		if (markerFeedback.state != null && !markerFeedback.state.canTransitionTo(state))
			throw new IllegalStateException(
				s"Cannot transition from ${markerFeedback.state} to $state. " +
				s"Valid transition states are ${markerFeedback.state.transitionStates}"
			)
		markerFeedback.state = state
		session.saveOrUpdate(markerFeedback)
	}
}
