package uk.ac.warwick.tabula.coursework.commands.assignments


import uk.ac.warwick.tabula.data.Transactions._
import uk.ac.warwick.tabula.data.model.Assignment
import uk.ac.warwick.tabula.commands.Description
import org.springframework.validation.Errors


class EditAssignmentCommand(val assignment: Assignment = null) extends ModifyAssignmentCommand {

	this.copyFrom(assignment)

	def module = assignment.module

	def checkMarkScheme() {
		Option(assignment.markScheme) match {
			// if students can choose the marker and submissions exist then the markScheme cannot be updated
			case Some(scheme) if scheme.studentsChooseMarker => canUpdateMarkScheme = (assignment.submissions.size() == 0)
			case Some(scheme) =>
			case None =>
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
