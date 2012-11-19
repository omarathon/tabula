package uk.ac.warwick.courses.data.convert

import uk.ac.warwick.courses.system.TwoWayConverter
import uk.ac.warwick.courses.data.model.MarkScheme
import uk.ac.warwick.courses.data.Daoisms

class MarkSchemeIdConverter extends TwoWayConverter[String, MarkScheme] with Daoisms {

	override def convertLeft(scheme: MarkScheme) = Option(scheme) match {
		case Some(s) => s.id
		case None => null
	}
	
	override def convertRight(id: String) = getById[MarkScheme](id).orNull

}