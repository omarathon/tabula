package uk.ac.warwick.courses.commands.markschemes

import uk.ac.warwick.courses.commands._
import uk.ac.warwick.courses.data.model._
import org.springframework.validation.Errors
import scala.reflect.BeanProperty
import uk.ac.warwick.courses.data.Transactions._
import uk.ac.warwick.courses.services.AssignmentService
import uk.ac.warwick.spring.Wire
import uk.ac.warwick.courses.data.MarkSchemeDao

class DeleteMarkSchemeCommand(var markScheme: MarkScheme) extends Command[Unit] with SelfValidating {

	@BeanProperty var department: Department = _
	
	var dao = Wire.auto[MarkSchemeDao]
	
	override def work() {
		transactional() {
			department.markSchemes.remove(markScheme)
		}
	}
	
	def validate(implicit errors: Errors) {
		if (!dao.getAssignmentsUsingMarkScheme(markScheme).isEmpty()) {
			errors.reject("inuse") // FIXME use a real message code
		}
	}
	
	override def describe(d: Description) {
		
	}
	
}