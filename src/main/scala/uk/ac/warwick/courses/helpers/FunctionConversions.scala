package uk.ac.warwick.courses.helpers

import org.springframework.jdbc.core.ConnectionCallback
import java.sql.Connection

/**
 * A collection of implicit conversions from Scala functions to Java callback interfaces.
 * Import this object's members to activate the implicit conversions.
 * 
 * These aren't really related so it's a bit lazy to bung them all in one class - but it's
 * not doing much harm.
 */
object FunctionConversions {
	implicit def ToConnectionCallback[T](fn: Connection=>T) = new ConnectionCallback[T] {
		override def doInConnection(connection:Connection) = fn(connection)
	}
}