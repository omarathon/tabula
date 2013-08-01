package uk.ac.warwick.tabula.coursework.commands.assignments

import scala.collection.JavaConverters._
import uk.ac.warwick.tabula.commands._
import uk.ac.warwick.tabula.data.model.Assignment
import uk.ac.warwick.tabula.services._
import uk.ac.warwick.tabula.system.permissions.{PermissionsChecking, RequiresPermissionsChecking}
import uk.ac.warwick.tabula.permissions.Permissions
import uk.ac.warwick.tabula.JavaImports._
import uk.ac.warwick.tabula.AcademicYear
import org.joda.time.DateTime

object CopyAssignmentsCommand {
	def apply() =
		new CopyAssignmentsCommand
			with ComposableCommand[Seq[Assignment]]
			with CopyAssignmentsPermissions
			with CopyAssignmentsDescription
			with AutowiringAssignmentServiceComponent {
				override lazy val eventName = "CopyAssignments"
			}
}

abstract class CopyAssignmentsCommand extends CommandInternal[Seq[Assignment]]
	with Appliable[Seq[Assignment]] with CopyAssignmentsState with FindAssignmentFields
{

	self: AssignmentServiceComponent =>

	def applyInternal(): Seq[Assignment] = {

		val scalaAssignments = assignments.asScala

		if (archive) {
			for (assignment <- scalaAssignments.filterNot(_.archived)) {
				assignment.archived = true
				assignmentService.save(assignment)
			}
		}

		scalaAssignments.map { assignment =>
			val newAssignment = copy(assignment)
			assignmentService.save(newAssignment)
			newAssignment
		}
	}

	def copy(assignment: Assignment) : Assignment = {
		val newAssignment = new Assignment()
		newAssignment.academicYear = academicYear

		// copy the other fields from the target assignment
		newAssignment.module = assignment.module
		newAssignment.name = assignment.name
		newAssignment.openDate = assignment.openDate
		newAssignment.closeDate = assignment.closeDate
		newAssignment.openEnded = assignment.openEnded
		newAssignment.collectMarks = assignment.collectMarks
		newAssignment.collectSubmissions = assignment.collectSubmissions
		newAssignment.restrictSubmissions = assignment.restrictSubmissions
		newAssignment.allowLateSubmissions = assignment.allowLateSubmissions
		newAssignment.allowResubmission = assignment.allowResubmission
		newAssignment.displayPlagiarismNotice = assignment.displayPlagiarismNotice
		newAssignment.allowExtensions = assignment.allowExtensions
		newAssignment.allowExtensionRequests = assignment.allowExtensionRequests
		newAssignment.summative = assignment.summative
		newAssignment.feedbackTemplate = assignment.feedbackTemplate
		newAssignment.markingWorkflow = assignment.markingWorkflow

		for (field <- findCommentField(assignment)) { newAssignment.addField(field) }
		for (file <- findFileField(assignment)) { newAssignment.addField(file) }
		newAssignment.addField(findWordCountField(assignment))
		newAssignment
	}

}

trait CopyAssignmentsPermissions extends RequiresPermissionsChecking {
	self: CopyAssignmentsState =>
	def permissionsCheck(p: PermissionsChecking) {
		for(assignment <- assignments.asScala) {
			if(archive) {
				p.PermissionCheck(Permissions.Assignment.Update, assignment)
			}
			p.PermissionCheck(Permissions.Assignment.Create, assignment.module)
		}
	}
}

trait CopyAssignmentsState {
	var assignments: JList[Assignment] = JArrayList()
	var archive: JBoolean = false
	var academicYear: AcademicYear = AcademicYear.guessByDate(new DateTime)
}

trait CopyAssignmentsDescription extends Describable[Seq[Assignment]] {

	self: CopyAssignmentsState =>

	def describe(d: Description) = d.properties("assignmentIds" -> assignments.asScala.map(_.id))
}
