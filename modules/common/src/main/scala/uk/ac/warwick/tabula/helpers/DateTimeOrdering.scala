package uk.ac.warwick.tabula.helpers
import org.joda.time.DateTime
import scala.math

/**
 * import uk.ac.warwick.tabula.helpers.DateTimeOrdering._
 * to tell your code how to sort DateTime objects
 */
object DateTimeOrdering {
	implicit def orderedDateTime(d: DateTime): math.Ordered[DateTime] = new math.Ordered[DateTime] {
		override def compare(d2: DateTime) = d.compareTo(d2)
	}
}