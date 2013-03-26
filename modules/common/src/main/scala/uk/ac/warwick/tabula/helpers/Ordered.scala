package uk.ac.warwick.tabula.helpers

import scala.beans.BeanProperty

/**
 * Extends Spring's Ordered interface for beans, with the only
 * meaningful implementation of it.
 */
trait Ordered extends org.springframework.core.Ordered {
	var order: Int = _
	override def getOrder = order
	def setOrder(o: Int) { order = o }
}