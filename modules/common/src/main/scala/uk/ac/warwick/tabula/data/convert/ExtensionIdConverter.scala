package uk.ac.warwick.tabula.data.convert
import org.springframework.beans.factory.annotation.Autowired

import uk.ac.warwick.tabula.data.model.forms.Extension
import uk.ac.warwick.tabula.services.AssignmentService
import uk.ac.warwick.tabula.system.TwoWayConverter

class ExtensionIdConverter extends TwoWayConverter[String, Extension] {

	@Autowired var service: AssignmentService = _

	override def convertRight(id: String) = Option(id) flatMap { service.getExtensionById(_) } orNull
	override def convertLeft(extension: Extension) = Option(extension) map {_.id} orNull

}