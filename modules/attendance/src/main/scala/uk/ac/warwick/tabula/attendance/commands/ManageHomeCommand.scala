package uk.ac.warwick.tabula.attendance.commands

import uk.ac.warwick.tabula.commands._
import uk.ac.warwick.tabula.services._
import uk.ac.warwick.tabula.data.model.Department
import uk.ac.warwick.tabula.system.permissions.Public
import uk.ac.warwick.tabula.permissions.Permissions
import uk.ac.warwick.tabula.CurrentUser

object ManageHomeCommand {
	def apply(user: CurrentUser) =
		new ManageHomeCommand(user)
		with Command[Set[Department]]
		with AutowiringModuleAndDepartmentServiceComponent
		with Public with ReadOnly with Unaudited
}


abstract class ManageHomeCommand(val user: CurrentUser) extends CommandInternal[Set[Department]] with ManageHomeState {
	self: ModuleAndDepartmentServiceComponent =>

	override def applyInternal() = {
		moduleAndDepartmentService.departmentsWithPermission(user, Permissions.MonitoringPoints.Manage)
	}
}

trait ManageHomeState {

	def user: CurrentUser

}
