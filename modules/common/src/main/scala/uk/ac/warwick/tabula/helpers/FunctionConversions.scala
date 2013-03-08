package uk.ac.warwick.tabula.helpers

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
trait FunctionConversions {

	implicit def ToConnectionCallback[A](fn: Connection => A) = new ConnectionCallback[A] {
		override def doInConnection(connection: Connection) = fn(connection)
	}

	implicit def ToHibernateCallback[A](fn: Session => A) = new HibernateCallback[A] {
		override def doInHibernate(session: Session) = fn(session)
	}

}

object FunctionConversions extends FunctionConversions