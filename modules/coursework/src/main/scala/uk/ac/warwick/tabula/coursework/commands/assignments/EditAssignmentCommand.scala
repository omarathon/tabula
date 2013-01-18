package uk.ac.warwick.tabula.coursework.commands.assignments


import uk.ac.warwick.tabula.data.Transactions._
import uk.ac.warwick.tabula.data.model.Assignment
import uk.ac.warwick.tabula.commands.Description
import org.springframework.validation.Errors
import uk.ac.warwick.tabula.actions.Participate
import uk.ac.warwick.tabula.data.model.Module


class EditAssignmentCommand(val module: Module = null, val assignment: Assignment = null) extends ModifyAssignmentCommand {
	
	mustBeLinked(assignment, module)
	PermissionsCheck(Participate(module))

	this.copyFrom(assignment)

	def canUpdateMarkScheme = {
		Option(assignment.markScheme) match {
			// if students can choose the marker and submissions exist then the markScheme cannot be updated
			case Some(scheme) if scheme.studentsChooseMarker => (assignment.submissions.size() == 0)
			case Some(scheme) => true
			case None => true
		}
	}

	override def contextSpecificValidation(errors:Errors){
		if (!canUpdateMarkScheme && (markScheme == null || assignment.markScheme != markScheme))
			errors.rejectValue("markScheme", "markScheme.cannotChange")
	}

	override def applyInternal(): Assignment = transactional() {
		copyTo(assignment)
		service.save(assignment)
		assignment
	}

	override def describe(d: Description) { d.assignment(assignment).properties(
		"name" -> name,
		"openDate" -> openDate,
		"closeDate" -> closeDate)
	}

}
