package uk.ac.warwick.tabula.attendance.commands

import uk.ac.warwick.tabula.commands._
import uk.ac.warwick.tabula.services._
import uk.ac.warwick.tabula.data.model.{RuntimeMember, StudentRelationshipType, StudentMember, Department}
import uk.ac.warwick.tabula.system.permissions.Public
import uk.ac.warwick.tabula.permissions.Permissions
import uk.ac.warwick.tabula.CurrentUser

object HomeCommand {
	def apply(user: CurrentUser) =
		new HomeCommand(user)
		with Command[Unit]
		with AutowiringModuleAndDepartmentServiceComponent
		with AutowiringProfileServiceComponent
		with AutowiringRelationshipServiceComponent
		with Public with ReadOnly with Unaudited
}

abstract class HomeCommand(val user: CurrentUser) extends CommandInternal[Unit] with HomeCommandState {
	self: ModuleAndDepartmentServiceComponent with ProfileServiceComponent with RelationshipServiceComponent =>

	override def applyInternal() = {
		val optionalCurrentMember = profileService.getMemberByUserId(user.apparentId, disableFilter = true)
		val currentMember = optionalCurrentMember getOrElse new RuntimeMember(user)
		hasProfile = currentMember match {
			case student: StudentMember =>
				student.mostSignificantCourseDetails match {
					case Some(scd) => true
					case None => false
				}
			case _ => false
		}
		viewPermissions = moduleAndDepartmentService.departmentsWithPermission(user, Permissions.MonitoringPoints.View)
		managePermissions = moduleAndDepartmentService.departmentsWithPermission(user, Permissions.MonitoringPoints.Manage)
		allRelationshipTypes = relationshipService.allStudentRelationshipTypes
		val downwardRelationships = relationshipService.listAllStudentRelationshipsWithMember(currentMember)
		relationshipTypesMap = allRelationshipTypes.map { t =>
			(t, downwardRelationships.exists(_.relationshipType == t))
		}.toMap

	}
}

trait HomeCommandState {

	def user: CurrentUser

	var hasProfile: Boolean = _
	var viewPermissions: Set[Department] = _
	var managePermissions: Set[Department] = _
	var allRelationshipTypes: Seq[StudentRelationshipType] = _
	var relationshipTypesMap: Map[StudentRelationshipType, Boolean] = _

}
