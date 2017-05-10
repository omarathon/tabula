package uk.ac.warwick.tabula.commands.groups.admin

import uk.ac.warwick.tabula.AcademicYear
import uk.ac.warwick.tabula.helpers.Logging
import uk.ac.warwick.tabula.permissions.Permissions
import uk.ac.warwick.tabula.data.model.{Assessment, Module}
import uk.ac.warwick.tabula.services.{AssessmentMembershipServiceComponent, AutowiringAssessmentMembershipServiceComponent, AutowiringSmallGroupServiceComponent, SmallGroupServiceComponent}
import uk.ac.warwick.userlookup.User

import scala.collection.JavaConverters._
import uk.ac.warwick.tabula.commands._
import uk.ac.warwick.tabula.data.model.groups.SmallGroupSet
import uk.ac.warwick.tabula.system.permissions.{PermissionsChecking, PermissionsCheckingMethods, RequiresPermissionsChecking}

object OldSmallGroupsMarkerAllocationCommand {
	def apply(assessment: Assessment) =
		new OldSmallGroupsMarkerAllocationCommandInternal(assessment)
			with ComposableCommand[Seq[SetAllocation]]
			with OldSmallGroupsMarkerAllocationCommandPermissions
			with AutowiringSmallGroupServiceComponent
			with AutowiringAssessmentMembershipServiceComponent
			with Unaudited
}

case class SetAllocation(set: SmallGroupSet, firstMarkerGroups: Seq[GroupAllocation], secondMarkerGroups: Seq[GroupAllocation])
case class GroupAllocation(name: String, tutors: Seq[User], students: Seq[User])

class OldSmallGroupsMarkerAllocationCommandInternal(val assessment: Assessment)
	extends CommandInternal[Seq[SetAllocation]]	with OldSmallGroupsMarkerAllocationCommandState with Logging {

	self : SmallGroupServiceComponent with AssessmentMembershipServiceComponent =>

	val module: Module = assessment.module
	val academicYear: AcademicYear = assessment.academicYear

	def applyInternal(): Seq[SetAllocation] = {
		val sets = smallGroupService.getSmallGroupSets(module, academicYear)
		val validStudents = assessmentMembershipService.determineMembershipUsers(assessment)

		val setAllocations = sets.map(set => {
			def getGroupAllocations(markers: Seq[User]) = set.groups.asScala.map(group => {
				val validMarkers = group.events
					.flatMap(_.tutors.users)
					.filter(markers.contains)
					.distinct

				val students = group.students.users.filter(validStudents.contains)
				GroupAllocation(group.name, validMarkers, students)
			})

			SetAllocation(
				set,
				getGroupAllocations(assessment.markingWorkflow.firstMarkers.users),
				getGroupAllocations(assessment.markingWorkflow.secondMarkers.users)
			)
		})

		// do not return sets that have groups that don't have at least one tutor who is a marker
		setAllocations.filterNot( s =>
			s.firstMarkerGroups.isEmpty ||
				assessment.markingWorkflow.hasSecondMarker && s.secondMarkerGroups.isEmpty ||
				s.firstMarkerGroups.forall(_.tutors.isEmpty) && s.secondMarkerGroups.forall(_.tutors.isEmpty)
		)
	}
}

trait OldSmallGroupsMarkerAllocationCommandState {
	val assessment: Assessment
}

trait OldSmallGroupsMarkerAllocationCommandPermissions extends RequiresPermissionsChecking with PermissionsCheckingMethods {
	self: OldSmallGroupsMarkerAllocationCommandState =>
	override def permissionsCheck(p: PermissionsChecking) {
		p.PermissionCheck(Permissions.SmallGroups.ReadMembership, mandatory(assessment))
	}
}