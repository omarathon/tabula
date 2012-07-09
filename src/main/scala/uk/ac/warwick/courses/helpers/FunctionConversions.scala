package uk.ac.warwick.courses.helpers

import org.springframework.jdbc.core.ConnectionCallback
import java.sql.Connection
import org.springframework.orm.hibernate3.HibernateCallback
import org.hibernate.Session

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
	
	implicit def ToHibernateCallback[T](fn: Session=>T) = new HibernateCallback[T] {
		override def doInHibernate(session:Session) = fn(session)
	}
	
}