package uk.ac.warwick.tabula.coursework.commands.feedback
import uk.ac.warwick.spring.Wire
import uk.ac.warwick.tabula.actions.Participate
import uk.ac.warwick.tabula.commands._
import uk.ac.warwick.tabula.data.model.Assignment
import uk.ac.warwick.tabula.data.model.Module
import uk.ac.warwick.tabula.services.AuditEventIndexService


class ListFeedbackCommand(val module: Module, val assignment: Assignment) extends Command[Seq[String]] with ReadOnly with Unaudited {
	mustBeLinked(assignment, module)
	PermissionsCheck(Participate(module))
	
	var auditIndexService = Wire.auto[AuditEventIndexService]
	
	override def applyInternal() = {
		auditIndexService.whoDownloadedFeedback(assignment)
	}

	override def describe(d: Description) =	d.assignment(assignment)
}