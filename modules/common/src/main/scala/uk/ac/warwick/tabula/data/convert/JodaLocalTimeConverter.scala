package uk.ac.warwick.tabula.data.convert

import scala.reflect.BeanProperty
import org.springframework.beans.factory.annotation.Autowired
import uk.ac.warwick.tabula.data.FileDao
import uk.ac.warwick.tabula.data.model.FileAttachment
import uk.ac.warwick.tabula.system.TwoWayConverter
import org.joda.time.LocalDate
import org.joda.time.format.DateTimeFormat
import uk.ac.warwick.tabula.DateFormats
import uk.ac.warwick.tabula.helpers.StringUtils._
import org.joda.time.LocalTime

class JodaLocalTimeConverter extends TwoWayConverter[String, LocalTime] {
	
	val formatter = DateTimeFormat.forPattern(DateFormats.TimePicker)

	override def convertRight(text: String) = 
		if (text.hasText) try {	LocalTime.parse(text, formatter) } catch {	case e: IllegalArgumentException => null }
		else null

	override def convertLeft(time: LocalTime) = Option(time) match {
		case Some (time) => formatter print time
		case None => null
	}
}