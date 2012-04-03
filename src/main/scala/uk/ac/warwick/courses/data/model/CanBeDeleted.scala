package uk.ac.warwick.courses.data.model

import uk.ac.warwick.courses.JavaImports._
import scala.reflect.BeanProperty

/**
 * Trait marking a Hibernate entity that has a deleted flag.
 * 
 * This is confusable with the Deleteable trait for the Delete() action. 
 */
trait CanBeDeleted {
	@BeanProperty var deleted:JBoolean = false
	
	def markDeleted() = {
		deleted = true
	}
}