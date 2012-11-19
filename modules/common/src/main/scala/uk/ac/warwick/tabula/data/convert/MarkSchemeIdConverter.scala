package uk.ac.warwick.tabula.data.convert

import uk.ac.warwick.tabula.system.TwoWayConverter
import uk.ac.warwick.tabula.data.model.MarkScheme
import uk.ac.warwick.tabula.data.Daoisms

class MarkSchemeIdConverter extends TwoWayConverter[String, MarkScheme] with Daoisms {

	override def convertLeft(scheme: MarkScheme) = Option(scheme) match {
		case Some(s) => s.id
		case None => null
	}
	
	override def convertRight(id: String) = getById[MarkScheme](id).orNull

}