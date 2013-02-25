package uk.ac.warwick.tabula.data.convert

import org.springframework.core.convert.converter.Converter
import org.springframework.format.Formatter
import org.springframework.beans.factory.annotation.Autowired
import uk.ac.warwick.tabula.services.AssignmentService
import java.util.Locale
import uk.ac.warwick.tabula.data.model.forms.AssessmentGroup


class AssessmentGroupIdConverter extends Converter[String, AssessmentGroup] with Formatter[AssessmentGroup] {

	@Autowired var service: AssignmentService = _

	// Converter used for binding request
	override def convert(id: String) = service.getAssessmentGroup(id).orNull

	// Formatter used for generating textual value in template
	override def parse(id: String, locale: Locale): AssessmentGroup = convert(id)
	override def print(group: AssessmentGroup, locale: Locale): String = group.id

}
