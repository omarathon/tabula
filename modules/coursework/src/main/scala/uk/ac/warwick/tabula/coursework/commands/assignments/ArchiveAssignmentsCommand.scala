package uk.ac.warwick.tabula.coursework.commands.assignments

import scala.collection.JavaConverters._
import uk.ac.warwick.tabula.data.model.{Assignment, Module}
import uk.ac.warwick.tabula.commands._
import uk.ac.warwick.tabula.services.{AssignmentServiceComponent, AutowiringAssignmentServiceComponent}
import uk.ac.warwick.tabula.system.permissions.{PermissionsChecking, RequiresPermissionsChecking}
import uk.ac.warwick.tabula.permissions.Permissions
import uk.ac.warwick.tabula.JavaImports._

object ArchiveAssignmentsCommand {
	def apply(modules: Seq[Module]) =
		new ArchiveAssignmentsCommand(modules: Seq[Module])
			with ComposableCommand[Seq[Assignment]]
			with ArchiveAssignmentsPermissions
			with ArchiveAssignmentsDescription
			with AutowiringAssignmentServiceComponent {
			override lazy val eventName = "ArchiveAssignments"
		}
}

abstract class ArchiveAssignmentsCommand(val modules: Seq[Module]) extends CommandInternal[Seq[Assignment]]
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
		for(module <- modules) {
			p.PermissionCheck(Permissions.Assignment.Update, module)
		}
	}
}

trait ArchiveAssignmentsState {
	val modules: Seq[Module]
	var assignments: JList[Assignment] = JArrayList()
}

trait ArchiveAssignmentsDescription extends Describable[Seq[Assignment]] {
	self: ArchiveAssignmentsState =>
	def describe(d: Description) = d
		.properties("modules" -> modules.map(_.id))
		.properties("assignments" -> assignments.asScala.map(_.id))
}