package uk.ac.warwick.tabula.coursework.commands.assignments

import scala.collection.JavaConverters._
import uk.ac.warwick.tabula.data.model.{Assignment, Module}
import uk.ac.warwick.tabula.commands._
import uk.ac.warwick.tabula.services.{AssignmentServiceComponent, AutowiringAssignmentServiceComponent}
import uk.ac.warwick.tabula.system.permissions.{PermissionsChecking, RequiresPermissionsChecking}
import uk.ac.warwick.tabula.permissions.Permissions
import uk.ac.warwick.tabula.JavaImports._
import uk.ac.warwick.tabula.data.model.Department

object ArchiveAssignmentsCommand {
	def apply(department: Department, modules: Seq[Module]) =
		new ArchiveAssignmentsCommand(department, modules)
			with ComposableCommand[Seq[Assignment]]
			with ArchiveAssignmentsPermissions
			with ArchiveAssignmentsDescription
			with AutowiringAssignmentServiceComponent {
			override lazy val eventName = "ArchiveAssignments"
		}
}

abstract class ArchiveAssignmentsCommand(val department: Department, val modules: Seq[Module]) extends CommandInternal[Seq[Assignment]]
with Appliable[Seq[Assignment]] with ArchiveAssignmentsState {

	self: AssignmentServiceComponent =>

	def applyInternal(): Seq[Assignment] = {
		val unarchivedAssignments = assignments.asScala.filterNot(_.archived)

		for (assignment <- unarchivedAssignments) {
			assignment.archived = true
			assignmentService.save(assignment)
		}
		unarchivedAssignments
	}

}

trait ArchiveAssignmentsPermissions extends RequiresPermissionsChecking {
	self: ArchiveAssignmentsState =>
	def permissionsCheck(p: PermissionsChecking) {
		if (modules.isEmpty) p.PermissionCheck(Permissions.Assignment.Archive, p.mandatory(department))
		else for (module <- modules) {
			p.mustBeLinked(p.mandatory(module), p.mandatory(department))
			p.PermissionCheck(Permissions.Assignment.Archive, module)
		}
	}
}

trait ArchiveAssignmentsState {
	val department: Department
	val modules: Seq[Module]
	var assignments: JList[Assignment] = JArrayList()
}

trait ArchiveAssignmentsDescription extends Describable[Seq[Assignment]] {
	self: ArchiveAssignmentsState =>
	def describe(d: Description) = d
		.properties("modules" -> modules.map(_.id))
		.properties("assignments" -> assignments.asScala.map(_.id))
}