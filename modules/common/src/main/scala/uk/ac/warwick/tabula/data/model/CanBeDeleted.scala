package uk.ac.warwick.tabula.data.model

import uk.ac.warwick.tabula.JavaImports._
import scala.beans.BeanProperty

/**
 * Trait marking a Hibernate entity that has a deleted flag.
 *
 * This is confusable with the Deleteable trait for the Delete() action.
 */
trait CanBeDeleted {
	var deleted: JBoolean = false

	def markDeleted() = {
		deleted = true
	}
}