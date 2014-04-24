package uk.ac.warwick.tabula.coursework.commands.assignments

import uk.ac.warwick.tabula.commands.{Describable, ComposableCommand, CommandInternal, Description}
import uk.ac.warwick.tabula.data.model.{SecondMarkersMap, FirstMarkersMap, UserGroup, Module, Assignment}
import uk.ac.warwick.tabula.services.{AutowiringAssignmentServiceComponent, AssignmentServiceComponent}
import uk.ac.warwick.tabula.permissions.Permissions
import uk.ac.warwick.tabula.system.permissions.{PermissionsChecking, PermissionsCheckingMethods, RequiresPermissionsChecking}
import uk.ac.warwick.tabula.JavaImports._
import scala.collection.JavaConverters._
import uk.ac.warwick.tabula.data.{AutowiringUserGroupDaoComponent, UserGroupDaoComponent}


object AssignMarkersCommand {
	def apply(module: Module, assignment: Assignment) =
		new AssignMarkersCommand(module, assignment)
		with ComposableCommand[Assignment]
		with AssignMarkersPermission
		with AssignMarkersDescription
		with AssignMarkersCommandState
		with AutowiringAssignmentServiceComponent
		with AutowiringUserGroupDaoComponent
}

class AssignMarkersCommand(val module: Module, val assignment: Assignment) extends CommandInternal[Assignment] {

	self: AssignmentServiceComponent with UserGroupDaoComponent =>

	var firstMarkerMapping : JMap[String, JList[String]] = assignment.markingWorkflow.firstMarkers.members.map({ marker =>
		val list : JList[String] = JArrayList()
		(marker, list)
	}).toMap.asJava

	var secondMarkerMapping : JMap[String, JList[String]] = assignment.markingWorkflow.secondMarkers.members.map({ marker =>
		val list : JList[String] = JArrayList()
		(marker, list)
	}).toMap.asJava

	def applyInternal() = {

		assignment.firstMarkers.clear()
		assignment.firstMarkers.addAll(firstMarkerMapping.asScala.map { case (markerId, studentIds) =>
			val group = UserGroup.ofUsercodes
			group.includedUserIds = studentIds.asScala
			userGroupDao.saveOrUpdate(group)
			FirstMarkersMap(assignment, markerId, group)
		}.toSeq.asJava)

		assignment.secondMarkers.clear()
		assignment.secondMarkers.addAll(secondMarkerMapping.asScala.map { case (markerId, studentIds) =>
			val group = UserGroup.ofUsercodes
			group.includedUserIds = studentIds.asScala
			userGroupDao.saveOrUpdate(group)
			SecondMarkersMap(assignment, markerId, group)
		}.toSeq.asJava)

		assignmentService.save(assignment)
		assignment

	}
}

trait AssignMarkersPermission extends RequiresPermissionsChecking with PermissionsCheckingMethods {

	self: AssignMarkersCommandState =>

	override def permissionsCheck(p: PermissionsChecking) {
		p.PermissionCheck(Permissions.Assignment.Update, assignment)
	}

}

trait AssignMarkersDescription extends Describable[Assignment] {

	self: AssignMarkersCommandState =>

	override lazy val eventName = "AssignMarkers"

	override def describe(d: Description) {
		d.assignment(assignment)
	}

}

trait AssignMarkersCommandState {
	def module: Module
	def assignment: Assignment
}
