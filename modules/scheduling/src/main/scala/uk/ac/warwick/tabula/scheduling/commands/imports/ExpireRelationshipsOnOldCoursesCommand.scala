package uk.ac.warwick.tabula.scheduling.commands.imports

import org.springframework.validation.Errors
import uk.ac.warwick.tabula.ItemNotFoundException
import uk.ac.warwick.tabula.commands._
import uk.ac.warwick.tabula.data.model.StudentMember
import uk.ac.warwick.tabula.permissions.Permissions
import uk.ac.warwick.tabula.services.{AutowiringRelationshipServiceComponent, RelationshipServiceComponent}
import uk.ac.warwick.tabula.system.permissions.{PermissionsChecking, PermissionsCheckingMethods, RequiresPermissionsChecking}

object ExpireRelationshipsOnOldCoursesCommand {
	def apply(student: StudentMember) =
		new ExpireRelationshipsOnOldCoursesCommandInternal(student)
			with ComposableCommand[Unit]
			with AutowiringRelationshipServiceComponent
			with ExpireRelationshipsOnOldCoursesValidation
			with ExpireRelationshipsOnOldCoursesPermissions
			with ExpireRelationshipsOnOldCoursesCommandState
			with Unaudited
}


class ExpireRelationshipsOnOldCoursesCommandInternal(val student: StudentMember) extends CommandInternal[Unit] {

	self: ExpireRelationshipsOnOldCoursesCommandState with RelationshipServiceComponent =>

	override def applyInternal() = {
		studentRelationships.groupBy(_.agent).map{ case(agent, agentRelationships) =>
			if (agentRelationships.exists(rel => rel.isCurrent && !rel.studentCourseDetails.isEnded)) {
				relationshipService.endStudentRelationships(
					agentRelationships.filter(rel => rel.isCurrent && rel.studentCourseDetails.isEnded)
				)
			}
		}
	}

}

trait ExpireRelationshipsOnOldCoursesValidation extends SelfValidating {

	self: ExpireRelationshipsOnOldCoursesCommandState with RelationshipServiceComponent =>

	override def validate(errors: Errors) {
		if (!student.freshStudentCourseDetails.exists(_.isEnded)) {
			errors.reject("No old courses for this student")
		} else if (relationshipService.findCurrentRelationships(personalTutorRelationshipType, student).isEmpty) {
			errors.reject("No current relationships for this student")
		} else {
			val hasExpirable = studentRelationships.groupBy(_.agent).exists{ case(agent, agentRels) =>
				// Has a current relationship on a non-ended course
				agentRels.exists(rel => rel.isCurrent && !rel.studentCourseDetails.isEnded) &&
					// Has some relationships to expire
					agentRels.exists(rel => rel.isCurrent && rel.studentCourseDetails.isEnded)
			}
			if (!hasExpirable) {
				errors.reject("No relationships to expire")
			}
		}
	}

}

trait ExpireRelationshipsOnOldCoursesPermissions extends RequiresPermissionsChecking with PermissionsCheckingMethods {

	self: ExpireRelationshipsOnOldCoursesCommandState =>

	override def permissionsCheck(p: PermissionsChecking) {
		p.PermissionCheck(Permissions.ImportSystemData)
	}

}

trait ExpireRelationshipsOnOldCoursesCommandState {

	self: RelationshipServiceComponent =>

	def student: StudentMember

	lazy val personalTutorRelationshipType = relationshipService.getStudentRelationshipTypeByUrlPart("tutor").getOrElse(
		throw new ItemNotFoundException("Could not find personal tutor relationship type")
	)
	lazy val studentRelationships = relationshipService.getRelationships(personalTutorRelationshipType, student)
}
