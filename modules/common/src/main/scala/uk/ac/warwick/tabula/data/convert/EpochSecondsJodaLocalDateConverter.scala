package uk.ac.warwick.tabula.data.convert

import org.joda.time.{DateTime, LocalDate}
import uk.ac.warwick.tabula.helpers.StringUtils._
import uk.ac.warwick.tabula.system.TwoWayConverter

class EpochSecondsJodaLocalDateConverter extends TwoWayConverter[String, LocalDate] {

	override def convertRight(value: String) =
		if (value.hasText) try { new DateTime(value.toLong * 1000).toLocalDate} catch {	case e: NumberFormatException => null }
		else null

	override def convertLeft(date: LocalDate) = Option(date) match {
		case Some (d) => date.toDateTimeAtStartOfDay.getMillis.toString
		case None => null
	}
}