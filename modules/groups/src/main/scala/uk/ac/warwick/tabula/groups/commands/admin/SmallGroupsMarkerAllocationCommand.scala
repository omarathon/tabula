package uk.ac.warwick.tabula.groups.commands.admin

import uk.ac.warwick.tabula.helpers.Logging
import uk.ac.warwick.tabula.permissions.Permissions
import uk.ac.warwick.tabula.data.model.Assignment
import uk.ac.warwick.tabula.services.{AssignmentMembershipServiceComponent, AutowiringAssignmentMembershipServiceComponent, AutowiringSmallGroupServiceComponent, SmallGroupServiceComponent}
import uk.ac.warwick.userlookup.User
import scala.collection.JavaConverters._
import uk.ac.warwick.tabula.commands._
import uk.ac.warwick.tabula.data.model.groups.SmallGroupSet
import uk.ac.warwick.tabula.system.permissions.{PermissionsChecking, PermissionsCheckingMethods, RequiresPermissionsChecking}

object SmallGroupsMarkerAllocationCommand {
	def apply(assignment: Assignment) =
		new SmallGroupsMarkerAllocationCommandInternal(assignment)
			with ComposableCommand[Seq[SetAllocation]]
			with SmallGroupsMarkerAllocationCommandPermissions
			with AutowiringSmallGroupServiceComponent
			with AutowiringAssignmentMembershipServiceComponent
			with Unaudited
}

case class SetAllocation(set: SmallGroupSet, firstMarkerGroups: Seq[GroupAllocation], secondMarkerGroups: Seq[GroupAllocation])
case class GroupAllocation(name: String, tutors: Seq[User], students: Seq[User])

class SmallGroupsMarkerAllocationCommandInternal(val assignment: Assignment)
	extends CommandInternal[Seq[SetAllocation]]	with SmallGroupsMarkerAllocationCommandState with Logging {

	self : SmallGroupServiceComponent with AssignmentMembershipServiceComponent =>

	val module = assignment.module
	val academicYear = assignment.academicYear

	def applyInternal() = {
		val sets = smallGroupService.getSmallGroupSets(module, academicYear)
		val validStudents = assignmentMembershipService.determineMembershipUsers(assignment)

		val setAllocations = sets.map(set => {
			def getGroupAllocations(markers: Seq[User]) = set.groups.asScala.map(group => {
				val validMarkers = group.events
					.flatMap(_.tutors.users)
					.filter(markers.contains)

				val students = group.students.users.filter(validStudents.contains)
				GroupAllocation(group.name, validMarkers, students)
			})

			SetAllocation(
				set,
				getGroupAllocations(assignment.markingWorkflow.firstMarkers.users),
				getGroupAllocations(assignment.markingWorkflow.secondMarkers.users)
			)
		})

		// do not return sets that have groups that don't have at least one tutor who is a marker
		setAllocations.filterNot( s =>
			s.firstMarkerGroups.isEmpty ||
			s.firstMarkerGroups.exists(_.tutors.isEmpty) ||
			(assignment.markingWorkflow.hasSecondMarker && (
				s.secondMarkerGroups.isEmpty ||
				s.secondMarkerGroups.exists(_.tutors.isEmpty)
			))
		)
	}
}

trait SmallGroupsMarkerAllocationCommandState {
	val assignment: Assignment
}

trait SmallGroupsMarkerAllocationCommandPermissions extends RequiresPermissionsChecking with PermissionsCheckingMethods {
	self: SmallGroupsMarkerAllocationCommandState =>
	override def permissionsCheck(p: PermissionsChecking) {
		p.PermissionCheck(Permissions.SmallGroups.ReadMembership, mandatory(assignment))
	}
}