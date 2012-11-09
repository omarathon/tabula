package uk.ac.warwick.courses.data

import org.hibernate.criterion._
import collection.JavaConverters._

/**
 * Nice wrapper for a Query object. You usually won't create
 * this explicitly - the Daoisms trait adds a newQuery method
 * to Session which will return one of these.
 */
class ScalaQuery[T](c: org.hibernate.Query) {
	
	def setString(name: String, value: String) = chainable { c.setString(name, value) }
	def setEntity(name: String, entity: Any) = chainable { c.setEntity(name, entity) }
	// TODO add other methods on demand
	
	// Helper to neaten up the above chainable methods - returns this instead of plain Query
    @inline private def chainable(fn: => Unit) = { fn; this }

    /** Returns a typed Seq of the results. */
    def seq: Seq[T] = list.asScala

    /** Returns a typed list of the results.*/
    def list: java.util.List[T] = c.list().asInstanceOf[java.util.List[T]]

    def scroll() = c.scroll()

    /**
     * Return an untyped list of the results, in case you've
     * set the projection for the query to return something else.
     */
    def untypedList: java.util.List[_] = c.list()

    /** Return Some(result), or None if no row matched. */
    def uniqueResult: Option[T] = Option(c.uniqueResult().asInstanceOf[T])
}