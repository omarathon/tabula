package uk.ac.warwick.tabula.data.convert

import uk.ac.warwick.tabula.system.TwoWayConverter
import org.joda.time.DateTime
import org.joda.time.format.{DateTimeFormat, DateTimeFormatter}
import uk.ac.warwick.tabula.DateFormats
import uk.ac.warwick.tabula.helpers.StringUtils._

class JodaDateTimeConverter extends TwoWayConverter[String, DateTime] {

	val formatter: DateTimeFormatter = DateTimeFormat.forPattern(DateFormats.DateTimePicker)

	override def convertRight(text: String): DateTime =
		if (text.hasText) try {	DateTime.parse(text, formatter)	} catch {	case e: IllegalArgumentException => null }
		else null

	override def convertLeft(date: DateTime): String = Option(date) match {
		case Some (date) => formatter print date
		case None => null
	}
}