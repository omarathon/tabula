package uk.ac.warwick.tabula.data.model

import org.hibernate.`type`.StandardBasicTypes
import scala.Array
import java.sql.Types


sealed abstract class SubmissionState(val name: String, val ts: Set[SubmissionState]){
	lazy val transitionStates = ts
	def canTransitionTo(state: SubmissionState): Boolean = transitionStates.contains(state)
	override def toString = name
}
// initial state - received but has not yet been released for marking
case object Received extends SubmissionState("Received", Set(ReleasedForMarking, MarkingCompleted))
// ready to be distributed to markers
case object ReleasedForMarking extends SubmissionState("ReleasedForMarking", Set(DownloadedByMarker))
// has been downloaded by the marker and is being marked
case object DownloadedByMarker extends SubmissionState("DownloadedByMarker", Set(MarkingCompleted))
// submission has been marked and feedback has been uploaded
case object MarkingCompleted extends SubmissionState("MarkingCompleted", Set())

object SubmissionState {
	val values: Set[SubmissionState] = Set(Received, ReleasedForMarking, DownloadedByMarker, MarkingCompleted)

	def fromCode(code: String): SubmissionState =
		if (code == null) null
		else values.find{_.name == code} match {
			case Some(state) => state
			case None => throw new IllegalArgumentException()
		}
}

class SubmissionStateUserType extends AbstractBasicUserType[SubmissionState, String]{

	val basicType = StandardBasicTypes.STRING
	override def sqlTypes = Array(Types.VARCHAR)

	val nullValue = null
	val nullObject = null

	override def convertToObject(string: String) = SubmissionState.fromCode(string)
	override def convertToValue(state: SubmissionState) = state.name
}
