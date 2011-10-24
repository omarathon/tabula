package uk.ac.warwick.courses.helpers

import org.springframework.core.{Ordered => OrderedBean}
import scala.reflect.BeanProperty

/**
 * Extends Spring's Ordered interface for beans, with the only
 * meaningful implementation of it.
 */
trait Ordered extends OrderedBean {
	@BeanProperty var order:Int = _
}