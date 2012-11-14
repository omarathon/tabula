package uk.ac.warwick.tabula.coursework.data.convert

import org.springframework.core.convert.converter.Converter
import org.springframework.beans.factory.annotation.Autowired
import uk.ac.warwick.tabula.coursework.services.ModuleAndDepartmentService
import uk.ac.warwick.tabula.coursework.data.model._
import uk.ac.warwick.tabula.coursework.services.AssignmentService
import org.springframework.format.Formatter
import java.util.Locale

class UpstreamAssignmentIdConverter extends Converter[String, UpstreamAssignment] with Formatter[UpstreamAssignment] {

	@Autowired var service: AssignmentService = _

	// Converter used for binding request
	override def convert(id: String) = service.getUpstreamAssignment(id).orNull

	// Formatter used for generating textual value in template
	override def parse(id: String, locale: Locale): UpstreamAssignment = convert(id)
	override def print(assignment: UpstreamAssignment, locale: Locale): String = assignment.id

}