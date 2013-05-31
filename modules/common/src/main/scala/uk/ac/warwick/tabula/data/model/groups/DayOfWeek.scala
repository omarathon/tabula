package uk.ac.warwick.tabula.data.model.groups

import org.joda.time.DateTimeConstants
import java.sql.Types
import org.hibernate.`type`.StandardBasicTypes
import uk.ac.warwick.tabula.data.model.AbstractBasicUserType
import uk.ac.warwick.tabula.JavaImports._

sealed abstract class DayOfWeek(val jodaDayOfWeek: Int) {
	def name = toString()
	def shortName = name.substring(0, 3)
	
	// For Spring, the silly bum
	def getName = name
	def getAsInt = jodaDayOfWeek
}

object DayOfWeek {
	case object Monday extends DayOfWeek(DateTimeConstants.MONDAY)
	case object Tuesday extends DayOfWeek(DateTimeConstants.TUESDAY)
	case object Wednesday extends DayOfWeek(DateTimeConstants.WEDNESDAY)
	case object Thursday extends DayOfWeek(DateTimeConstants.THURSDAY)
	case object Friday extends DayOfWeek(DateTimeConstants.FRIDAY)
	case object Saturday extends DayOfWeek(DateTimeConstants.SATURDAY)
	case object Sunday extends DayOfWeek(DateTimeConstants.SUNDAY)

	// lame manual collection. Keep in sync with the case objects above
	val members = Seq(Monday, Tuesday, Wednesday, Thursday, Friday, Saturday, Sunday)
	
	def unapply(i: Int): Option[DayOfWeek] = i match {
		case DateTimeConstants.MONDAY => Some(Monday)
		case DateTimeConstants.TUESDAY => Some(Tuesday)
		case DateTimeConstants.WEDNESDAY => Some(Wednesday)
		case DateTimeConstants.THURSDAY => Some(Thursday)
		case DateTimeConstants.FRIDAY => Some(Friday)
		case DateTimeConstants.SATURDAY => Some(Saturday)
		case DateTimeConstants.SUNDAY => Some(Sunday)
		case _ => None
	}
	
	def apply(i: Int): DayOfWeek = i match {
		case DayOfWeek(d) => d
		case _ => throw new IllegalArgumentException("Invalid value for day of week: %d" format (i))
	}
}

class DayOfWeekUserType extends AbstractBasicUserType[DayOfWeek, JInteger] {

	val basicType = StandardBasicTypes.INTEGER
	override def sqlTypes = Array(Types.INTEGER)

	val nullValue = null
	val nullObject = null

	override def convertToObject(int: JInteger) = DayOfWeek(int)
	override def convertToValue(day: DayOfWeek) = day.jodaDayOfWeek

}