package uk.ac.warwick.tabula.commands.cm2.assignments

import uk.ac.warwick.tabula.commands._
import uk.ac.warwick.tabula.system.permissions.{PermissionsChecking, PermissionsCheckingMethods, RequiresPermissionsChecking}
import uk.ac.warwick.userlookup.User
import ListMarkerAllocationsCommand._
import uk.ac.warwick.tabula.data.model.Assignment
import uk.ac.warwick.tabula.permissions.Permissions
import uk.ac.warwick.tabula.services.CM2MarkingWorkflowService.Allocations
import uk.ac.warwick.tabula.services._

import scala.collection.immutable.SortedSet
import scala.collection.mutable


case class MarkerAllocations(
	keys: List[String], // ordered keys for all of the other maps - getting freemarker to iterate across maps in order is dumb
	unallocatedStudents: Map[String, SortedSet[Student]],
	allocations: Map[String, Map[Marker, SortedSet[Student]]],
	markers: Map[String, SortedSet[Marker]]
)

object ListMarkerAllocationsCommand {

	type Student = User
	type Marker = User

	implicit val userOrdering: Ordering[User] = Ordering.by { u: User => (u.getLastName, u.getFirstName) }

	def apply(assignment: Assignment) = new ListMarkerAllocationsCommandInternal(assignment)
		with ComposableCommand[MarkerAllocations]
		with ListMarkerAllocationsPermissions
		with Unaudited
		with AutowiringUserLookupComponent
		with AutowiringCM2MarkingWorkflowServiceComponent
		with AutowiringAssessmentMembershipServiceComponent
}

class ListMarkerAllocationsCommandInternal(val assignment: Assignment) extends CommandInternal[MarkerAllocations]
	with ListMarkerAllocationsState {

	// selftype for dependencies go here
	this: UserLookupComponent with CM2MarkingWorkflowServiceComponent with AssessmentMembershipServiceComponent =>

	def applyInternal(): MarkerAllocations = {

		val workflow = assignment.cm2MarkingWorkflow
		val stagesByRole = workflow.allStages.groupBy(_.roleName)
		val allStudents = SortedSet(assessmentMembershipService.determineMembershipUsers(assignment): _*)

		val allocations = mutable.LinkedHashMap[String, Map[Marker, SortedSet[Student]]]()
		val markers = mutable.LinkedHashMap[String, SortedSet[Marker]]()

		def toSortedSet(allocations: Allocations) = allocations.map{case(marker, students) =>
			marker -> SortedSet(students.toSeq: _*)
		}

		for {
			(role, stages) <- stagesByRole
			stage <- stages
		} if(workflow.workflowType.rolesShareAllocations) {
				// key by role as the allocations are the same
				allocations += role -> toSortedSet(cm2MarkingWorkflowService.getMarkerAllocations(assignment, stage))
				markers += role -> SortedSet(workflow.markers(stage): _*)
		} else {
			allocations +=  stage.allocationName -> toSortedSet(cm2MarkingWorkflowService.getMarkerAllocations(assignment, stage))
			markers += stage.allocationName -> SortedSet(workflow.markers(stage): _*)
		}


		val keys: List[String] = if(workflow.workflowType.rolesShareAllocations) {
			stagesByRole.keys.toList.sortBy(r => stagesByRole(r).map(_.order).min) // sort roles by their earliest stages
		} else {
			workflow.allStages.sortBy(_.order).map(_.allocationName).toList
		}

		val unallocatedStudents = allocations.map{ case(key, allocation) =>
			key ->  (allStudents -- allocation.filterKeys(_.isFoundUser).values.flatten.toSet)
		}

		MarkerAllocations(keys, Map(unallocatedStudents.toList: _*), Map(allocations.toList: _*), Map(markers.toList: _*))
	}
}

trait ListMarkerAllocationsPermissions extends RequiresPermissionsChecking with PermissionsCheckingMethods {
	self: ListMarkerAllocationsState =>

	def permissionsCheck(p: PermissionsChecking) {
		p.PermissionCheck(Permissions.Assignment.Update, assignment.module)
	}
}

trait ListMarkerAllocationsState {
	val assignment: Assignment
}