package uk.ac.warwick.tabula.data.model.attendance

import org.hibernate.`type`.StandardBasicTypes
import java.sql.Types
import uk.ac.warwick.tabula.data.model.AbstractBasicUserType

sealed abstract class MonitoringPointType(val dbValue: String, val description: String)

object MonitoringPointType {
	case object Meeting extends MonitoringPointType("meeting", "Meeting")
	case object SmallGroup extends MonitoringPointType("smallGroup", "Small Group")
	case object AssignmentSubmission extends MonitoringPointType("assignmentSubmission", "Assignment Submission")

	def fromCode(code: String) = code match {
		case Meeting.dbValue => Meeting
		case SmallGroup.dbValue => SmallGroup
		case AssignmentSubmission.dbValue => AssignmentSubmission
		case null => null
		case _ => throw new IllegalArgumentException()
	}
}

class MonitoringPointTypeUserType extends AbstractBasicUserType[MonitoringPointType, String] {

	val basicType = StandardBasicTypes.STRING
	override def sqlTypes = Array(Types.VARCHAR)

	val nullValue = null
	val nullObject = null

	override def convertToObject(string: String) = MonitoringPointType.fromCode(string)

	override def convertToValue(state: MonitoringPointType) = state.dbValue

}