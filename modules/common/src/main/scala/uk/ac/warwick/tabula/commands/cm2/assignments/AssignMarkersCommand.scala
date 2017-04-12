package uk.ac.warwick.tabula.commands.cm2.assignments

import uk.ac.warwick.tabula.commands._
import uk.ac.warwick.tabula.system.permissions.{PermissionsChecking, PermissionsCheckingMethods, RequiresPermissionsChecking}
import org.springframework.validation.Errors
import uk.ac.warwick.tabula.data.model.Assignment
import uk.ac.warwick.tabula.services._
import uk.ac.warwick.tabula.JavaImports._
import uk.ac.warwick.tabula.data.model.markingworkflow.MarkingWorkflowStage
import uk.ac.warwick.tabula.helpers.LazyMaps
import uk.ac.warwick.tabula.permissions.Permissions
import uk.ac.warwick.tabula.services.CM2MarkingWorkflowService.Allocations

import scala.collection.JavaConverters._

object AssignMarkersCommand {

	def apply(assignment: Assignment) =
		new AssignMarkersCommandInternal(assignment)
			with ComposableCommand[Assignment]
			with AssignMarkersValidation
			with AssignMarkersPermissions
			with AssignMarkersDescription
			with AutowiringUserLookupComponent
			with AutowiringCM2MarkingWorkflowServiceComponent
}

class AssignMarkersCommandInternal(val assignment: Assignment)
	extends CommandInternal[Assignment] with AssignMarkersState with AssignMarkersValidation {

	this: UserLookupComponent with CM2MarkingWorkflowServiceComponent  =>

	def applyInternal(): Assignment = {
		assignment.cm2MarkingWorkflow.allStages.foreach(stage => {
			val allocations = allocationMap.getOrElse(stage, Map())
			cm2MarkingWorkflowService.allocateMarkersForStage(assignment, stage, allocations)
		})

		assignment
	}
}

trait AssignMarkersPermissions extends RequiresPermissionsChecking with PermissionsCheckingMethods {
	self: AssignMarkersState =>

	def permissionsCheck(p: PermissionsChecking) {
		p.PermissionCheck(Permissions.Assignment.Update, assignment.module)
	}
}

trait AssignMarkersValidation extends SelfValidating {
	self: AssignMarkersState =>
	def validate(errors: Errors) {
		//errors.rejectValue("reviewerComments", "extension.reviewerComments.provideReasons")
	}
}

trait AssignMarkersDescription extends Describable[Assignment] {
	self: AssignMarkersState =>

	private def printAllocation(allocation: Allocations): String = allocation.map{ case(marker, students) =>
			s"${marker.getUserId} -> ${students.map(_.getUserId).mkString(",")}"
	}.mkString("\n")

	def describe(d: Description) {
		d.assignment(assignment)
		 .properties("allocations" -> allocationMap.map{
				case(stage, allocation) => s"${stage.roleName}:\n${printAllocation(allocation)}"
			}.mkString("\n"))
	}
}

trait AssignMarkersState {

	this: UserLookupComponent =>

	def assignment: Assignment

	// the nasty java mutable bindable map transformed into lovely immutable scala maps with the users resolved
	def allocationMap: Map[MarkingWorkflowStage, Allocations] = {
		allocations.asScala.map{ case (stage, jmap) =>
			stage -> jmap.asScala.map{ case (marker, students) =>
				userLookup.getUserByUserId(marker) -> students.asScala.map(userLookup.getUserByUserId).toSet
			}.toMap
		}.toMap
	}

	// will this bind? who knows! - spring is a capricious monster
	var allocations: JMap[MarkingWorkflowStage, JMap[String, JList[String]]] = LazyMaps.create{ _: MarkingWorkflowStage =>
		LazyMaps.create{ _: String => JArrayList(): JList[String] }.asJava
	}.asJava
}