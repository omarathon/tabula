package uk.ac.warwick.courses.data

import org.hibernate.criterion._
import collection.JavaConverters._

/** Nice wrapper for a Criteria object. You usually won't create
  * this explicitly - the Daoisms trait adds a newCriteria method
  * to Session which will return one of these.
  */
class ScalaCriteria[T](c: org.hibernate.Criteria) {

	def add(criterion: Criterion) = chainable { c.add(criterion) }
	def addOrder(order: Order) = chainable { c.addOrder(order) }
	def setMaxResults(i: Int) = chainable { c.setMaxResults(i) }
	def setFirstResult(i: Int) = chainable { c.setFirstResult(i) }

	// Helper to neaten up the above chainable methods
	@inline private def chainable(fn: => Unit) = { fn; this }

	/** Returns a typed Seq of the results. */
	def seq: Seq[T] = list.asScala

	/** Returns a typed list of the results.*/
	def list: java.util.List[T] = c.list().asInstanceOf[java.util.List[T]]
	
	/** Return an untyped list of the results, in case you've
	  * set the projection for the Criteria to return something else. */
	def untypedList: java.util.List[_] = c.list()
	
	/** Return Some(result), or None if no row matched. */
	def uniqueResult: Option[T] = Option(c.uniqueResult().asInstanceOf[T])
}