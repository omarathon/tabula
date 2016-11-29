package uk.ac.warwick.tabula.data.convert
import uk.ac.warwick.tabula.system.TwoWayConverter
import org.joda.time.{DateTime, LocalDate}
import org.joda.time.format.{DateTimeFormat, DateTimeFormatter}
import uk.ac.warwick.tabula.DateFormats
import uk.ac.warwick.tabula.helpers.StringUtils._

class JodaLocalDateConverter extends TwoWayConverter[String, LocalDate] {

	val formatter: DateTimeFormatter = DateTimeFormat.forPattern(DateFormats.DatePicker)

	override def convertRight(text: String): LocalDate =
		if (text.hasText && text.forall(_.isDigit))
			try { new DateTime(text.toLong * 1000).toLocalDate } catch { case e: NumberFormatException => null }
		else if (text.hasText)
			try {	LocalDate.parse(text, formatter) } catch { case e: IllegalArgumentException => null }
		else null

	override def convertLeft(date: LocalDate): String = Option(date) match {
		case Some (d) => formatter print d
		case None => null
	}
}