package uk.ac.warwick.tabula.profiles.commands

import uk.ac.warwick.tabula.commands.Command
import uk.ac.warwick.tabula.services.ModuleAndDepartmentServiceComponent
import uk.ac.warwick.tabula.services.AutowiringModuleAndDepartmentServiceComponent
import uk.ac.warwick.tabula.services.RelationshipServiceComponent
import uk.ac.warwick.tabula.services.SmallGroupServiceComponent
import uk.ac.warwick.tabula.FeaturesComponent
import uk.ac.warwick.tabula.CurrentUser
import uk.ac.warwick.tabula.commands.CommandInternal
import uk.ac.warwick.tabula.data.model.Member
import uk.ac.warwick.tabula.AutowiringFeaturesComponent
import uk.ac.warwick.tabula.services.AutowiringSmallGroupServiceComponent
import uk.ac.warwick.tabula.services.AutowiringRelationshipServiceComponent
import uk.ac.warwick.tabula.commands.Unaudited
import uk.ac.warwick.tabula.commands.ReadOnly
import uk.ac.warwick.tabula.system.permissions.Public
import uk.ac.warwick.tabula.permissions.Permissions
import uk.ac.warwick.tabula.data.model.Department
import uk.ac.warwick.tabula.data.model.groups.SmallGroup
import uk.ac.warwick.tabula.data.model.StudentRelationshipType
import uk.ac.warwick.tabula.commands.TaskBenchmarking

case class ProfilesHomeInformation(
	smallGroups: Seq[SmallGroup] = Nil,
	relationshipTypesMap: Map[StudentRelationshipType, Boolean] = Map(),
	adminDepartments: Set[Department] = Set()
)

object ProfilesHomeCommand {
	def apply(user: CurrentUser, currentMember: Option[Member]) =
		new ProfilesHomeCommand(user, currentMember) 
			with Command[ProfilesHomeInformation] 
			with AutowiringFeaturesComponent 
			with AutowiringSmallGroupServiceComponent 
			with AutowiringRelationshipServiceComponent 
			with AutowiringModuleAndDepartmentServiceComponent 
			with Public with ReadOnly with Unaudited
		
}

abstract class ProfilesHomeCommand(val user: CurrentUser, val currentMember: Option[Member]) extends CommandInternal[ProfilesHomeInformation] with TaskBenchmarking {
	self: FeaturesComponent with SmallGroupServiceComponent with RelationshipServiceComponent with ModuleAndDepartmentServiceComponent =>

	override def applyInternal() = {
		if (user.isStaff) {
			val smallGroups =
				if (features.smallGroupTeachingTutorView) benchmarkTask("Find all small groups with user as tutor") { 
					smallGroupService.findSmallGroupsByTutor(user.apparentUser) 
				}
				else Nil
				
			// Get all the relationship types that the current member is an agent of
			val downwardRelationshipTypes = currentMember.map { m => 
				benchmarkTask("Get all relationship types with member") { relationshipService.listAllStudentRelationshipTypesWithMember(m) } 
			}.getOrElse(Nil)
				
			// Get all the enabled relationship types for a department
			// Filtered by department visibility in view
			val allRelationshipTypes = benchmarkTask("Get all relationship types") { relationshipService.allStudentRelationshipTypes }
			
			// A map from each type to a boolean for whether the current member has downward relationships of that type
			val relationshipTypesMap = benchmarkTask("Map relationship types to existing ones") { allRelationshipTypes.map { t =>
				(t, downwardRelationshipTypes.exists(_ == t))
			}.toMap }
			
			val adminDepartments = benchmarkTask("Get all departments with permissions to manage profiles") { 
				moduleAndDepartmentService.departmentsWithPermission(user, Permissions.Department.ManageProfiles)
			}
			
			ProfilesHomeInformation(
				smallGroups = smallGroups,
				relationshipTypesMap = relationshipTypesMap,
				adminDepartments = adminDepartments
			)
		} else ProfilesHomeInformation()
	}
}