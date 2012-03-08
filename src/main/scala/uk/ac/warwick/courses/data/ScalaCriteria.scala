package uk.ac.warwick.courses.data

import org.hibernate.criterion.Criterion
import org.hibernate.criterion.Order

class ScalaCriteria[T](c:org.hibernate.Criteria) {
	def add(criterion:Criterion) = {
		c.add(criterion)
		this
	}
	def addOrder(order:Order) = {
		c.addOrder(order)
		this
	}
	def setMaxResults(i:Int) = {
		c.setMaxResults(i)
		this
	}
	def setFirstResult(i:Int) = {
		c.setFirstResult(i)
		this
	}
	def list: java.util.List[T] = c.list().asInstanceOf[java.util.List[T]]
	def uniqueResult:Option[T] = Option(c.uniqueResult().asInstanceOf[T])
}