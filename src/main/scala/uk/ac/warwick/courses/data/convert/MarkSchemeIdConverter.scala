package uk.ac.warwick.courses.data.convert

import uk.ac.warwick.courses.system.TwoWayConverter
import uk.ac.warwick.courses.data.model.MarkScheme
import uk.ac.warwick.courses.data.Daoisms

class MarkSchemeIdConverter extends TwoWayConverter[String, MarkScheme] with Daoisms {

	override def convertLeft(scheme: MarkScheme) = scheme.id
	
	override def convertRight(id: String) = getById[MarkScheme](id) getOrElse { throw new IllegalArgumentException }

}