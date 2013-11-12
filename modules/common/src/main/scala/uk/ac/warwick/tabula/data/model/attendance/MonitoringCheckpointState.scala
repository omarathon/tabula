package uk.ac.warwick.tabula.data.model.attendance

import org.hibernate.`type`.StandardBasicTypes
import java.sql.Types
import uk.ac.warwick.tabula.data.model.AbstractBasicUserType

sealed abstract class MonitoringCheckpointState(val dbValue: String, val description: String)

object MonitoringCheckpointState {
	case object Attended extends MonitoringCheckpointState("attended", "Attended")
	case object MissedAuthorised extends MonitoringCheckpointState("authorised", "Missed (authorised)")
	case object MissedUnauthorised extends MonitoringCheckpointState("unauthorised", "Missed (unauthorised)")

	def fromCode(code: String) = code match {
	  	case Attended.dbValue => Attended
	  	case MissedAuthorised.dbValue => MissedAuthorised
	  	case MissedUnauthorised.dbValue => MissedUnauthorised
	  	case null => null
	  	case _ => throw new IllegalArgumentException()
	}

	val values: Seq[MonitoringCheckpointState] = Seq(MissedUnauthorised, MissedAuthorised, Attended)
}

class MonitoringCheckpointStateUserType extends AbstractBasicUserType[MonitoringCheckpointState, String] {

	val basicType = StandardBasicTypes.STRING
	override def sqlTypes = Array(Types.VARCHAR)

	val nullValue = null
	val nullObject = null

	override def convertToObject(string: String) = MonitoringCheckpointState.fromCode(string)
	
	override def convertToValue(state: MonitoringCheckpointState) = state.dbValue

}