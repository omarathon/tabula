package uk.ac.warwick.tabula.data.model

import org.hibernate.`type`.StandardBasicTypes
import scala.Array
import java.sql.Types


sealed abstract class SubmissionState(val name: String){
	def canTransitionTo: Set[SubmissionState]
	override def toString = name
}
// initial state - received but has not yet been released for marking
case object Received extends SubmissionState("Received"){
	def canTransitionTo = Set(ReleasedForMarking)
}
// ready to be distributed to markers
case object ReleasedForMarking extends SubmissionState("ReleasedForMarking"){
	def canTransitionTo = Set(DownloadedByMarker)
}
// has been downloaded by the marker and is being marked
case object DownloadedByMarker extends SubmissionState("DownloadedByMarker"){
	def canTransitionTo = Set(MarkingCompleted)
}
// submission has been marked and feedback has been uploaded
case object MarkingCompleted extends SubmissionState("MarkingCompleted"){
	def canTransitionTo = Set()
}

object SubmissionState {
	def fromCode(code: String) = code match {
		case "Received" => Received
		case "ReleasedForMarking" => ReleasedForMarking
		case "DownloadedByMarker" => DownloadedByMarker
		case "MarkingCompleted" => MarkingCompleted
		case null => null
		case _ => throw new IllegalArgumentException()
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