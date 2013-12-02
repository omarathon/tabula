package uk.ac.warwick.tabula.data

import org.hibernate.criterion._
import collection.JavaConverters._
import uk.ac.warwick.tabula.JavaImports._
import org.hibernate.transform.DistinctRootEntityResultTransformer
import collection.mutable
import org.hibernate.transform.ResultTransformer
import org.hibernate.Criteria

/**
 * Nice wrapper for a Criteria object. You usually won't create
 * this explicitly - the Daoisms trait adds a newCriteria method
 * to Session which will return one of these.
 */
class ScalaCriteria[A](c: org.hibernate.Criteria) {

	private val aliases: mutable.Map[String, String] = mutable.Map()

	def add(criterion: Criterion) = chainable { c.add(criterion) }
	def add(restriction: ScalaRestriction) = chainable {
		restriction.aliases.foreach { case (property, alias) => createAlias(property, alias) }
		c.add(restriction.underlying)
	}
	def addOrder(order: Order) = chainable { c.addOrder(order) }
	def addOrder(order: ScalaOrder) = chainable {
		order.aliases.foreach { case (property, alias) => createAlias(property, alias) }
		c.addOrder(order.underlying)
	}
	def setMaxResults(i: Int) = chainable { c.setMaxResults(i) }
	def setFirstResult(i: Int) = chainable { c.setFirstResult(i) }
	def createAlias(property: String, alias: String) = chainable {
		aliases.put(property, alias) match {
			case None => c.createAlias(property, alias)
			case Some(existing) if existing == alias => // duplicate
			case Some(other) => throw new IllegalArgumentException("Tried to alias %s to %s, but it is already aliased to %s!".format(property, alias, other))
		}
	}
	def distinct = chainable { c.setResultTransformer(DistinctRootEntityResultTransformer.INSTANCE) }

	// Helper to neaten up the above chainable methods - returns this instead of plain Criteria
	@inline private def chainable(fn: => Unit) = { fn; this }

	def project[B](projection: Projection) = {
		c.setProjection(projection)
		new ProjectedScalaCriteria[A, B](this)
	}

	/** Returns a typed Seq of the results. */
	def seq: Seq[A] = list.asScala

	/** Returns a typed list of the results. */
	def list: JList[A] = listOf[A]

	def scroll() = c.scroll()

	/**
	 * Return an untyped list of the results, in case you've
	 * set the projection for the Criteria to return something else.
	 */
	def untypedList: JList[_] = c.list()

	/**
	 * Returns a Java List of the given type. If the result of the query
	 * doesn't match the requested type you will get a runtime error.
	 */
	def listOf[A]: JList[A] = untypedList.asInstanceOf[JList[A]]

	/** Return Some(result), or None if no row matched. */
	def uniqueResult: Option[A] = uniqueResultOf[A]

	def uniqueResultOf[A]: Option[A] = Option(c.uniqueResult().asInstanceOf[A])

	def setResultTransformer[B](resultTransformer: ResultTransformer) = {
		c.setResultTransformer(resultTransformer)
		new ProjectedScalaCriteria[A, B] (this)
	}
}

class ProjectedScalaCriteria[A, B](c: ScalaCriteria[A]) {
	/** Returns a typed Seq of the results. */
	def seq: Seq[B] = c.listOf[B].asScala

	def uniqueResult: Option[B] = c.uniqueResultOf[B]

	def setResultTransformer(resultTransformer: ResultTransformer) = c.setResultTransformer(resultTransformer)
}


