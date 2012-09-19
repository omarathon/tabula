package uk.ac.warwick.courses.helpers

import scala.reflect.BeanProperty

/**
 * Extends Spring's Ordered interface for beans, with the only
 * meaningful implementation of it.
 */
trait Ordered extends org.springframework.core.Ordered {
	var order: Int = _
	override def getOrder = order
	def setOrder(o: Int) { order = o }
}