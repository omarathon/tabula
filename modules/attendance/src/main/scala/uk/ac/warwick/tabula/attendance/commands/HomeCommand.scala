package uk.ac.warwick.tabula.attendance.commands

import uk.ac.warwick.tabula.commands._
import uk.ac.warwick.tabula.services._
import uk.ac.warwick.tabula.data.model.Department
import uk.ac.warwick.tabula.system.permissions.Public
import uk.ac.warwick.tabula.permissions.Permissions
import uk.ac.warwick.tabula.CurrentUser

object HomeCommand {
	def apply(user: CurrentUser) =
		new HomeCommand(user)
		with Command[Map[String, Set[Department]]]
		with AutowiringModuleAndDepartmentServiceComponent
		with Public with ReadOnly with Unaudited
}

abstract class HomeCommand(val user: CurrentUser) extends CommandInternal[Map[String, Set[Department]]] with HomeCommandState {
	self: ModuleAndDepartmentServiceComponent =>

	override def applyInternal() = {
		Map(
			"View" -> moduleAndDepartmentService.departmentsWithPermission(user, Permissions.MonitoringPoints.View),
			"Manage" -> moduleAndDepartmentService.departmentsWithPermission(user, Permissions.MonitoringPoints.Manage)
		)
	}
}

trait HomeCommandState {

	def user: CurrentUser

}
