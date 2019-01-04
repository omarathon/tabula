package uk.ac.warwick.tabula.data.model

/**
 * Entity which implements toEntityReference, to allow it to be
 * referenced by a notification.
 */
trait ToEntityReference {
	type Entity >: Null <: ToEntityReference
	final def toEntityReference: EntityReference[Entity] = EntityReference(this.asInstanceOf[Entity])
}
