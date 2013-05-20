package uk.ac.warwick.tabula.data.model.groups

import uk.ac.warwick.tabula.data.model.AbstractBasicUserType
import org.hibernate.`type`.StandardBasicTypes
import java.sql.Types

case class WeekRange(val minWeek: Int, val maxWeek: Int) {
	if (maxWeek < minWeek) throw new IllegalArgumentException("maxWeek must be >= minWeek")
	
	def isSingleWeek = maxWeek == minWeek
	
	override def toString =
		if (!isSingleWeek) "%d-%d" format (minWeek, maxWeek)
		else minWeek.toString
}

object WeekRange {
	def apply(singleWeek: Int): WeekRange = WeekRange(singleWeek, singleWeek)
	def fromString(rep: String) = rep.split('-') match {
		case Array(singleWeek) => WeekRange(singleWeek.toInt)
		case Array(min, max) => WeekRange(min.toInt, max.toInt)
		case _ => throw new IllegalArgumentException("Couldn't convert string representation %s to WeekRange" format (rep))
	}
	
	implicit val defaultOrdering = Ordering.by[WeekRange, Int] ( _.minWeek )
	
	object NumberingSystem {
		val Term = "term"
		val Cumulative = "cumulative"
		val Academic = "academic"
			
		val Default = Term
	}
}

class WeekRangeListUserType extends AbstractBasicUserType[Seq[WeekRange], String] {

	val separator = ","
	val basicType = StandardBasicTypes.STRING
	override def sqlTypes = Array(Types.VARCHAR)

	val nullValue = null
	val nullObject = Nil

	override def convertToObject(string: String) = string.split(separator) map { rep => WeekRange.fromString(rep) }
	override def convertToValue(list: Seq[WeekRange]) = if (list.isEmpty) null else list.map { _.toString }.mkString(separator)

}