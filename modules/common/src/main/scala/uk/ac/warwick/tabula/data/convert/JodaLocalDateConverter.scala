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

class JodaLocalDateConverter extends TwoWayConverter[String, LocalDate] {
	
	val formatter = DateTimeFormat.forPattern(DateFormats.DatePicker)

	override def convertRight(text: String) = 
		if (text.hasText) try {	LocalDate.parse(text, formatter)	} catch {	case e: IllegalArgumentException => null }
		else null

	override def convertLeft(date: LocalDate) = Option(date) match {
		case Some (date) => formatter print date
		case None => null
	}
}