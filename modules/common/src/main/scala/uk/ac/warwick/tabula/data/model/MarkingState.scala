package uk.ac.warwick.tabula.data.model

import org.hibernate.`type`.StandardBasicTypes
import scala.Array
import java.sql.Types


sealed abstract class MarkingState(val name: String, val ts: Set[MarkingState]){
	lazy val transitionStates = ts
	
	def canTransitionTo(state: MarkingState): Boolean = state == this || transitionStates.contains(state)
	
	override def toString = name
}

// initial state - ready to be distributed to markers
case object ReleasedForMarking extends MarkingState("ReleasedForMarking", Set(DownloadedByMarker, MarkingCompleted))
// has been downloaded by the marker and is being marked
case object DownloadedByMarker extends MarkingState("DownloadedByMarker", Set(MarkingCompleted))
// submission has been marked and feedback has been uploaded
case object MarkingCompleted extends MarkingState("MarkingCompleted", Set())

object MarkingState {
	val values: Set[MarkingState] = Set(ReleasedForMarking, DownloadedByMarker, MarkingCompleted)

	def fromCode(code: String): MarkingState =
		if (code == null) null
		else values.find{_.name == code} match {
			case Some(state) => state
			case None => throw new IllegalArgumentException()
		}
}

class MarkingStateUserType extends AbstractBasicUserType[MarkingState, String]{

	val basicType = StandardBasicTypes.STRING
	override def sqlTypes = Array(Types.VARCHAR)

	val nullValue = null
	val nullObject = null

	override def convertToObject(string: String) = MarkingState.fromCode(string)
	override def convertToValue(state: MarkingState) = state.name
}
