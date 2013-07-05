package uk.ac.warwick.tabula.coursework.commands.assignments


import uk.ac.warwick.tabula.data.Transactions._
import uk.ac.warwick.tabula.data.model.Assignment
import uk.ac.warwick.tabula.commands.Description
import org.springframework.validation.Errors
import uk.ac.warwick.tabula.permissions._
import uk.ac.warwick.tabula.data.model.Module


class EditAssignmentCommand(module: Module = null, val assignment: Assignment = null)
	extends ModifyAssignmentCommand(module) {
	
	mustBeLinked(assignment, module)
	PermissionCheck(Permissions.Assignment.Update, assignment)

	this.copyFrom(assignment)

	def canUpdateMarkingWorkflow = {
		Option(assignment.markingWorkflow) match {
			// if students can choose the marker and submissions exist then the markingWorkflow cannot be updated
			case Some(scheme) if scheme.studentsChooseMarker => (assignment.submissions.size() == 0)
			case Some(scheme) => true
			case None => true
		}
	}
	
	override def validate(errors: Errors) {
		super.validate(errors)
		
		if (academicYear != assignment.academicYear) {
			errors.rejectValue("academicYear", "academicYear.immutable")
		}
	}

	override def contextSpecificValidation(errors:Errors){
		val workflowChanged = assignment.markingWorkflow != markingWorkflow
		if (!canUpdateMarkingWorkflow && workflowChanged){
			errors.rejectValue("markingWorkflow", "markingWorkflow.cannotChange")
		}
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
