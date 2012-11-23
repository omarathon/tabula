package uk.ac.warwick.tabula.data.convert

import org.springframework.core.convert.converter.Converter
import org.springframework.beans.factory.annotation.Autowired
import uk.ac.warwick.tabula.data.model.Department
import uk.ac.warwick.tabula.services.ModuleAndDepartmentService
import uk.ac.warwick.tabula.data.model.Module
import uk.ac.warwick.tabula.services.AssignmentService
import uk.ac.warwick.tabula.data.model.Assignment
import org.springframework.format.Formatter
import java.util.Locale

class AssignmentIdConverter extends Converter[String, Assignment] with Formatter[Assignment] {

	@Autowired var service: AssignmentService = _

	override def convert(id: String) = service.getAssignmentById(id).orNull
	
	override def print(assignment: Assignment, l:Locale) = assignment.id
	override def parse(id: String, l:Locale) = service.getAssignmentById(id).getOrElse(throw new IllegalArgumentException)

}