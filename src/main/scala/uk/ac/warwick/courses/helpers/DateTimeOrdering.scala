package uk.ac.warwick.courses.helpers
import org.joda.time.DateTime
import scala.math.{Ordered => SMOrdered}

trait DateTimeOrdering {
	implicit def orderedDateTime(d:DateTime): SMOrdered[DateTime] = new SMOrdered[DateTime] {
		override def compare(d2:DateTime) = d.compareTo(d2)
	}
}