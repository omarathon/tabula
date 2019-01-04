package uk.ac.warwick.tabula.data

import org.hibernate.ScrollableResults
import org.hibernate.transform.DistinctRootEntityResultTransformer
import uk.ac.warwick.tabula.JavaImports._

import scala.collection.JavaConverters._

/**
 * Nice wrapper for a Query object. You usually won't create
 * this explicitly - the Daoisms trait adds a newQuery method
 * to Session which will return one of these.
 */
class ScalaQuery[A](c: org.hibernate.query.Query[A]) {

	def distinct: ScalaQuery[A] = chainable { c.setResultTransformer(DistinctRootEntityResultTransformer.INSTANCE) }

	def setString(name: String, value: String): ScalaQuery[A] = chainable { c.setString(name, value) }
	def setEntity(name: String, entity: Any): ScalaQuery[A] = chainable { c.setEntity(name, entity) }
	def setParameter(name: String, value: Any): ScalaQuery[A] = chainable { c.setParameter(name, value) }
	def setParameterList(name: String, list: Seq[_]): ScalaQuery[A] = chainable { c.setParameterList(name, list.toList.asJava) }
	def setMaxResults(maxResults: Int): ScalaQuery[A] = chainable { c.setMaxResults(maxResults) }
	def setFirstResult(firstResult: Int): ScalaQuery[A] = chainable { c.setFirstResult(firstResult) }

	// Helper to neaten up the above chainable methods - returns this instead of plain Query
	@inline private def chainable(fn: => Unit): ScalaQuery[A] = { fn; this }

	/** Returns a typed Seq of the results. */
	def seq: Seq[A] = list.asScala

	/** Returns a typed list of the results.*/
	def list: JList[A] = c.list()

	def scroll(): ScrollableResults = c.scroll()

	/**
	 * Return an untyped list of the results, in case you've
	 * set the projection for the query to return something else.
	 */
	def untypedList: JList[_] = c.list()

	/** Return Some(result), or None if no row matched. */
	def uniqueResult: Option[A] = Option(c.uniqueResult())
}

class ScalaUpdateQuery(c: org.hibernate.query.Query[_]) {
	def setString(name: String, value: String): ScalaUpdateQuery = chainable { c.setString(name, value) }
	def setEntity(name: String, entity: Any): ScalaUpdateQuery = chainable { c.setEntity(name, entity) }
	def setParameter(name: String, value: Any): ScalaUpdateQuery = chainable { c.setParameter(name, value) }
	def setParameterList(name: String, list: Seq[_]): ScalaUpdateQuery = chainable { c.setParameterList(name, list.toList.asJava) }

	// Helper to neaten up the above chainable methods - returns this instead of plain Query
	@inline private def chainable(fn: => Unit): ScalaUpdateQuery = { fn; this }

	def run(): Int = c.executeUpdate()
	def executeUpdate(): Int = c.executeUpdate()
}