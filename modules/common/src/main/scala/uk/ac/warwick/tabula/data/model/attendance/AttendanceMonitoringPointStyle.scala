package uk.ac.warwick.tabula.data.model.attendance

import uk.ac.warwick.tabula.data.model.AbstractStringUserType

sealed abstract class AttendanceMonitoringPointStyle(val dbValue: String, val description: String)

object AttendanceMonitoringPointStyle {
	case object Week extends AttendanceMonitoringPointStyle("week", "Week")
	case object Date extends AttendanceMonitoringPointStyle("date", "Date")

	def fromCode(code: String) = code match {
		case Week.dbValue => Week
		case Date.dbValue => Date
		case null => null
		case _ => throw new IllegalArgumentException()
	}
}

class AttendanceMonitoringPointStyleUserType extends AbstractStringUserType[AttendanceMonitoringPointStyle] {

	override def convertToObject(string: String) = AttendanceMonitoringPointStyle.fromCode(string)

	override def convertToValue(state: AttendanceMonitoringPointStyle) = state.dbValue

}