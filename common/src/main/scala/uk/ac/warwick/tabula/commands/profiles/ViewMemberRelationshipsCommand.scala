package uk.ac.warwick.tabula.commands.profiles

import uk.ac.warwick.tabula.commands.profiles.ViewMemberRelationshipsCommand.Result
import uk.ac.warwick.tabula.commands.{CommandInternal, ComposableCommand, ReadOnly, TaskBenchmarking, Unaudited, _}
import uk.ac.warwick.tabula.data.model._
import uk.ac.warwick.tabula.permissions.{Permissions, PermissionsSelector}
import uk.ac.warwick.tabula.services.{RelationshipServiceComponent, _}
import uk.ac.warwick.tabula.system.permissions.{PermissionsChecking, PermissionsCheckingMethods, RequiresPermissionsChecking}


object ViewMemberRelationshipsCommand {

	type CommandType = Appliable[Result]

	case class Result(
		entities: Map[StudentRelationshipType, Seq[StudentCourseDetails]]
	)

	def apply(currentMember: Member): Command[Result] = {
		new ViewMemberRelationshipsCommandInternal(currentMember)
			with ComposableCommand[Result]
			with AutowiringProfileServiceComponent
			with AutowiringRelationshipServiceComponent
			with ViewMemberRelationshipsCommandPermissions
			with ViewMemberRelationshipsCommandState
			with Unaudited with ReadOnly
	}
}


abstract class ViewMemberRelationshipsCommandInternal(val currentMember: Member)
	extends CommandInternal[Result] with TaskBenchmarking with ViewMemberRelationshipsCommandState {
	self: ProfileServiceComponent with RelationshipServiceComponent =>

	def applyInternal(): Result = {
		val rslt = relationshipTypes.map { r =>
			r -> profileService.getSCDsByAgentRelationshipAndRestrictions(r, currentMember, Nil)
		}.filter { case (_, stuDetails) => stuDetails.nonEmpty }
		Result(rslt.toMap)
	}

}

trait ViewMemberRelationshipsCommandState {
	self: RelationshipServiceComponent =>

	def currentMember: Member

	lazy val relationshipTypes: Seq[StudentRelationshipType] = relationshipService.allStudentRelationshipTypes

}

trait ViewMemberRelationshipsCommandPermissions extends RequiresPermissionsChecking with PermissionsCheckingMethods {
	self: ViewMemberRelationshipsCommandState =>

	def permissionsCheck(p: PermissionsChecking) {
		p.PermissionCheck(Permissions.Profiles.StudentRelationship.Read(PermissionsSelector.Any[StudentRelationshipType]), currentMember)
	}
}
