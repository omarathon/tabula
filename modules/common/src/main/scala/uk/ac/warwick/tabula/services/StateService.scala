package uk.ac.warwick.tabula.services

import uk.ac.warwick.tabula.data.model.{MarkerFeedback, MarkingState, Submission}
import org.springframework.stereotype.Service
import uk.ac.warwick.tabula.data.{HasSession, Daoisms}
import uk.ac.warwick.tabula.helpers.Logging

trait StateService {
	def updateState(markerFeedback: MarkerFeedback, state: MarkingState)
}

@Service(value = "stateService")
class StateServiceImpl extends ComposableStateServiceImpl with Daoisms


class ComposableStateServiceImpl extends StateService {
  this:HasSession =>

  def updateState(markerFeedback: MarkerFeedback, state: MarkingState){
    if (markerFeedback.state != null && !markerFeedback.state.canTransitionTo(state))
      throw new IllegalStateException("Cannot transition from " + markerFeedback.state + " to " + state)
    markerFeedback.state = state
    session.saveOrUpdate(markerFeedback)
  }
}
