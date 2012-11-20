package uk.ac.warwick.tabula.coursework.commands.markschemes

import uk.ac.warwick.tabula.commands._
import uk.ac.warwick.tabula.data.model._
import org.springframework.validation.Errors
import scala.reflect.BeanProperty
import uk.ac.warwick.tabula.data.Transactions._
import uk.ac.warwick.tabula.services.AssignmentService
import uk.ac.warwick.spring.Wire
import uk.ac.warwick.tabula.data.MarkSchemeDao

class DeleteMarkSchemeCommand(var markScheme: MarkScheme) extends Command[Unit] with SelfValidating {

	@BeanProperty var department: Department = _
	
	var dao = Wire.auto[MarkSchemeDao]
	
	override def applyInternal() {
		transactional() {
			department.markSchemes.remove(markScheme)
		}
	}
	
	def validate(errors: Errors) {
		if (!dao.getAssignmentsUsingMarkScheme(markScheme).isEmpty()) {
			errors.reject("inuse") // FIXME use a real message code
		}
	}
	
	override def describe(d: Description) {
		
	}
	
}