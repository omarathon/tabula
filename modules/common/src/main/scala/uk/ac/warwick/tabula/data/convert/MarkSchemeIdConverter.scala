package uk.ac.warwick.tabula.data.convert

import uk.ac.warwick.tabula.system.TwoWayConverter
import uk.ac.warwick.tabula.data.model.MarkScheme
import uk.ac.warwick.tabula.data.Daoisms

class MarkSchemeIdConverter extends TwoWayConverter[String, MarkScheme] with Daoisms {

	override def convertLeft(scheme: MarkScheme) = scheme.id
	
	override def convertRight(id: String) = getById[MarkScheme](id) getOrElse { throw new IllegalArgumentException }

}