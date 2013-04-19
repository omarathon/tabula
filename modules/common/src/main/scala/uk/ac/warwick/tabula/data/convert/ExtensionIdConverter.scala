package uk.ac.warwick.tabula.data.convert
import uk.ac.warwick.spring.Wire
import uk.ac.warwick.tabula.data.model.forms.Extension
import uk.ac.warwick.tabula.services.AssignmentService
import uk.ac.warwick.tabula.system.TwoWayConverter
import uk.ac.warwick.tabula.services.ExtensionService

class ExtensionIdConverter extends TwoWayConverter[String, Extension] {

	var service = Wire[ExtensionService]

	override def convertRight(id: String) = (Option(id) flatMap { service.getExtensionById(_) }).orNull
	override def convertLeft(extension: Extension) = (Option(extension) map {_.id}).orNull

}