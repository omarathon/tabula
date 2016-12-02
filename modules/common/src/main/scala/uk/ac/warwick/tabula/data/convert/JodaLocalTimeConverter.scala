package uk.ac.warwick.tabula.data.convert

import uk.ac.warwick.tabula.system.TwoWayConverter
import org.joda.time.format.{DateTimeFormat, DateTimeFormatter}
import uk.ac.warwick.tabula.DateFormats
import uk.ac.warwick.tabula.helpers.StringUtils._
import org.joda.time.LocalTime

class JodaLocalTimeConverter extends TwoWayConverter[String, LocalTime] {

	val formatter: DateTimeFormatter = DateTimeFormat.forPattern(DateFormats.TimePicker)

	override def convertRight(text: String): LocalTime =
		if (text.hasText) try {	LocalTime.parse(text, formatter) } catch {	case e: IllegalArgumentException => null }
		else null

	override def convertLeft(time: LocalTime): String = Option(time) match {
		case Some (time) => formatter print time
		case None => null
	}
}