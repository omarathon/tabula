package uk.ac.warwick.tabula.data.convert


import uk.ac.warwick.tabula.data.model.MeetingFormat
import uk.ac.warwick.tabula.system.TwoWayConverter

class MeetingFormatConverter extends TwoWayConverter[String, MeetingFormat] {

	override def convertRight(description: String) = MeetingFormat.fromDescription(description)
	override def convertLeft(format: MeetingFormat) = (Option(format) map { _.description }).orNull
}