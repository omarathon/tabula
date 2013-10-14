package uk.ac.warwick.tabula.attendance.commands

import uk.ac.warwick.tabula.commands._
import uk.ac.warwick.tabula.services._
import uk.ac.warwick.tabula.data.model.{StudentMember, Department}
import uk.ac.warwick.tabula.system.permissions.Public
import uk.ac.warwick.tabula.permissions.Permissions
import uk.ac.warwick.tabula.CurrentUser

object HomeCommand {
	def apply(user: CurrentUser) =
		new HomeCommand(user)
		with Command[(Boolean, Map[String, Set[Department]])]
		with AutowiringModuleAndDepartmentServiceComponent
		with AutowiringProfileServiceComponent
		with Public with ReadOnly with Unaudited
}

abstract class HomeCommand(val user: CurrentUser) extends CommandInternal[(Boolean, Map[String, Set[Department]])] with HomeCommandState {
	self: ModuleAndDepartmentServiceComponent with ProfileServiceComponent =>

	override def applyInternal() = {
		Pair(
			profileService.getMemberByUserId(user.apparentId) match {
				case Some(student: StudentMember) =>
					student.mostSignificantCourseDetails match {
						case Some(scd) => true
						case None => false
					}
				case _ => false
			},
			Map(
			"ViewPermissions" -> moduleAndDepartmentService.departmentsWithPermission(user, Permissions.MonitoringPoints.View),
			"ManagePermissions" -> moduleAndDepartmentService.departmentsWithPermission(user, Permissions.MonitoringPoints.Manage)
			)
		)

	}
}

trait HomeCommandState {

	def user: CurrentUser

}
