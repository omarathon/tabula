package uk.ac.warwick.tabula.data.convert
import org.springframework.beans.factory.annotation.Autowired

import org.springframework.core.convert.converter.Converter

import uk.ac.warwick.tabula.data.model.forms.Extension
import uk.ac.warwick.tabula.services.AssignmentService

class ExtensionIdConverter extends Converter[String, Extension] {

	@Autowired var service: AssignmentService = _

	override def convert(id: String) = service.getExtensionById(id).orNull

}