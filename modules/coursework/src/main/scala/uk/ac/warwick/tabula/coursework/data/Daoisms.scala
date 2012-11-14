package uk.ac.warwick.tabula.coursework.data

import org.hibernate.SessionFactory
import scala.reflect.Manifest
import org.springframework.beans.factory.annotation.Autowired
import scala.annotation.target.field
import javax.sql.DataSource
import org.hibernate.Session
import uk.ac.warwick.tabula.coursework.data.model.CanBeDeleted
import uk.ac.warwick.tabula.data.ScalaCriteria
import uk.ac.warwick.tabula.data.ScalaQuery
import javax.annotation.Resource
import org.springframework.transaction.support.TransactionTemplate
import org.springframework.transaction.TransactionStatus
import org.springframework.transaction.support.TransactionCallback
import org.springframework.transaction.PlatformTransactionManager
import uk.ac.warwick.spring.Wire

/**
 * A trait for DAO classes to mix in to get useful things
 * like the current session.
 *
 * It's only really for Hibernate access to the default
 * session factory. If you want to do JDBC stuff or use a
 * different data source you'll need to look elsewhere.
 */
trait Daoisms {
	import uk.ac.warwick.tabula.data.Transactions._

	import org.hibernate.criterion.Restrictions._
	def is = org.hibernate.criterion.Restrictions.eq _

	var dataSource = Wire[DataSource]("dataSource")
	var sessionFactory = Wire.auto[SessionFactory]

	protected def session = sessionFactory.getCurrentSession

	def isFilterEnabled(name: String) = session.getEnabledFilter(name) != null

	/**
	 * Do some work in a new session. Only needed outside of a request,
	 * since we already have sessions there. When you know there's already
	 * a session, you can access it through the `session` getter (within
	 * the callback of this method, it should work too).
	 */
	protected def inSession(fn: (Session) => Unit) {
		val sess = sessionFactory.openSession()
		try fn(sess) finally sess.close()
	}

	class NiceQueryCreator(session: Session) {
		def newCriteria[T](implicit m: Manifest[T]) = new ScalaCriteria[T](session.createCriteria(m.erasure))
		def newQuery[T](sql: String)(implicit m: Manifest[T]) = new ScalaQuery[T](session.createQuery(sql))
	}

	/**
	 * Adds a method to Session which returns a wrapped Criteria or Query that works
	 * better with Scala's generics support.
	 */
	implicit def niceCriteriaCreator(session: Session) = new NiceQueryCreator(session)

	/**
	 * Returns Some(obj) if it matches the expected type, otherwise None.
	 * Useful for converting the value from .uniqueResult into a typed Option.
	 *
	 * An implicit Manifest object is supplied by the Scala compiler, which
	 * holds detailed information about the type D which is otherwise missing
	 * from the JVM bytecode.
	 */
	def option[D](obj: Any)(implicit m: Manifest[D]): Option[D] = obj match {
		case a: Any if m.erasure.isInstance(a) => Some(a.asInstanceOf[D])
		case _ => None
	}

	/**
	 * type-safe session.get. returns an Option object, which will match None if
	 * null is returned.
	 *
	 * For CanBeDeleted entities, it also checks if the entity is deleted and
	 * the notDeleted filter is enabled, in which case it also returns None.
	 */
	protected def getById[D](id: String)(implicit m: Manifest[D]): Option[D] =
		session.get(m.erasure.getName(), id) match {
			case entity: CanBeDeleted if entity.deleted && isFilterEnabled("notDeleted") => None
			case entity: Any if m.erasure.isInstance(entity) => Some(entity.asInstanceOf[D])
			case _ => None
		}

}
