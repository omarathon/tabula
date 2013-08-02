package uk.ac.warwick.tabula.coursework.commands.assignments

import scala.collection.JavaConverters._
import uk.ac.warwick.tabula.commands._
import uk.ac.warwick.tabula.data.model.{Module, Assignment}
import uk.ac.warwick.tabula.services._
import uk.ac.warwick.tabula.system.permissions.{PermissionsChecking, RequiresPermissionsChecking}
import uk.ac.warwick.tabula.permissions.Permissions
import uk.ac.warwick.tabula.JavaImports._
import uk.ac.warwick.tabula.AcademicYear
import org.joda.time.DateTime
import uk.ac.warwick.tabula.data.model.forms.WordCountField
import org.springframework.validation.Errors

object CopyAssignmentsCommand {
	def apply(modules: Seq[Module]) =
		new CopyAssignmentsCommand(modules: Seq[Module])
			with ComposableCommand[Seq[Assignment]]
			with CopyAssignmentsPermissions
			with CopyAssignmentsDescription
			with AutowiringAssignmentServiceComponent {
				override lazy val eventName = "CopyAssignments"
			}
}

abstract class CopyAssignmentsCommand(val modules: Seq[Module]) extends CommandInternal[Seq[Assignment]]
	with Appliable[Seq[Assignment]] with CopyAssignmentsState with FindAssignmentFields {

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
		newAssignment.archived = false

		// best guess of new open and close dates. likely to be wrong by up to a few weeks but better than out by years
		val yearOffest = academicYear.startYear - assignment.academicYear.startYear
		newAssignment.openDate = assignment.openDate.plusYears(yearOffest)
		newAssignment.closeDate = assignment.closeDate.plusYears(yearOffest)

		// copy the other fields from the target assignment
		newAssignment.module = assignment.module
		newAssignment.name = assignment.name
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

		newAssignment.addDefaultFields()

		for (field <- findCommentField(assignment); newField <- findCommentField(newAssignment)) newField.value = field.value

		for (field <- findFileField(assignment); newField <- findFileField(newAssignment)) {
			newField.attachmentLimit = field.attachmentLimit
			newField.attachmentTypes = field.attachmentTypes
		}

		val field = findWordCountField(assignment)
		val newField = findWordCountField(newAssignment)
		newField.max = field.max
		newField.min = field.min
		newField.conventions = field.conventions

		newAssignment
	}

}

trait CopyAssignmentsPermissions extends RequiresPermissionsChecking {
	self: CopyAssignmentsState =>
	def permissionsCheck(p: PermissionsChecking) {
		for(module <- modules) {
			if(archive) {
				p.PermissionCheck(Permissions.Assignment.Update, module)
			}
			p.PermissionCheck(Permissions.Assignment.Create, module)
		}
	}
}

trait CopyAssignmentsState {
	val modules: Seq[Module]

	var assignments: JList[Assignment] = JArrayList()
	var archive: JBoolean = false
	var academicYear: AcademicYear = AcademicYear.guessByDate(new DateTime)
}

trait CopyAssignmentsDescription extends Describable[Seq[Assignment]] {
	self: CopyAssignmentsState =>
	def describe(d: Description) = d
		.properties("modules" -> modules.map(_.id))
		.properties("assignments" -> assignments.asScala.map(_.id))
}
