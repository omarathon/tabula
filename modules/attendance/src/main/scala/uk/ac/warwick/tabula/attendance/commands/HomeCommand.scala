package uk.ac.warwick.tabula.attendance.commands

import uk.ac.warwick.tabula.commands._
import uk.ac.warwick.tabula.services._
import uk.ac.warwick.tabula.data.model.{RuntimeMember, StudentRelationshipType, StudentMember, Department}
import uk.ac.warwick.tabula.system.permissions.Public
import uk.ac.warwick.tabula.permissions.Permissions
import uk.ac.warwick.tabula.CurrentUser
import scala.collection.JavaConverters._

case class HomeInformation(
	hasProfile: Boolean,
	viewPermissions: Set[Department],
	managePermissions: Set[Department],
	allRelationshipTypes: Seq[StudentRelationshipType],
	relationshipTypesMap: Map[StudentRelationshipType, Boolean]
)

object HomeCommand {
	def apply(user: CurrentUser) =
		new HomeCommand(user)
		with Command[HomeInformation]
		with AutowiringModuleAndDepartmentServiceComponent
		with AutowiringProfileServiceComponent
		with AutowiringRelationshipServiceComponent
		with Public with ReadOnly with Unaudited
}

abstract class HomeCommand(val user: CurrentUser) extends CommandInternal[HomeInformation] with HomeCommandState {
	self: ModuleAndDepartmentServiceComponent with ProfileServiceComponent with RelationshipServiceComponent =>

	override def applyInternal() = {
		val optionalCurrentMember = profileService.getMemberByUserId(user.apparentId, disableFilter = true)
		val currentMember = optionalCurrentMember getOrElse new RuntimeMember(user)
		val hasProfile = currentMember match {
			case student: StudentMember =>
				student.mostSignificantCourseDetails match {
					case Some(scd) => true
					case None => false
				}
			case _ => false
		}

		val viewDepartments = moduleAndDepartmentService.departmentsWithPermission(user, Permissions.MonitoringPoints.View)
		val manageDepartments = moduleAndDepartmentService.departmentsWithPermission(user, Permissions.MonitoringPoints.Manage)
		
		val viewRoutes = moduleAndDepartmentService.routesWithPermission(user, Permissions.MonitoringPoints.View)
		val manageRoutes = moduleAndDepartmentService.routesWithPermission(user, Permissions.MonitoringPoints.Manage)
		
		def withSubDepartments(d: Department) = (Set(d) ++ d.children.asScala.toSet).filter(_.routes.asScala.size > 0)
		
		val allViewDepartments = (viewDepartments ++ viewRoutes.map { _.department }).map(withSubDepartments).flatten
		val allManageDepartments = (manageDepartments ++ manageRoutes.map { _.department }).map(withSubDepartments).flatten
		
		// TODO we might want to distinguish between a "Manage department" and a "Manage route" like we do in coursework with modules
		// These return Sets so no need to distinct the result

		val allRelationshipTypes = relationshipService.allStudentRelationshipTypes
		val downwardRelationships = relationshipService.listAllStudentRelationshipsWithMember(currentMember)
		val relationshipTypesMap = allRelationshipTypes.map { t =>
			(t, downwardRelationships.exists(_.relationshipType == t))
		}.toMap
		
		HomeInformation(
			hasProfile,
			allViewDepartments,
			allManageDepartments,
			allRelationshipTypes,
			relationshipTypesMap
		)
	}
}

trait HomeCommandState {
	def user: CurrentUser
}
