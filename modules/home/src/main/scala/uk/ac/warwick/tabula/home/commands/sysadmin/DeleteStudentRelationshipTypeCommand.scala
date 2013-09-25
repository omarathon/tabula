package uk.ac.warwick.tabula.home.commands.sysadmin

import uk.ac.warwick.tabula.services.RelationshipServiceComponent
import uk.ac.warwick.tabula.data.Transactions._
import uk.ac.warwick.tabula.commands.CommandInternal
import uk.ac.warwick.tabula.system.permissions.RequiresPermissionsChecking
import uk.ac.warwick.tabula.system.permissions.PermissionsChecking
import uk.ac.warwick.tabula.permissions.Permissions
import uk.ac.warwick.tabula.commands.Unaudited
import uk.ac.warwick.tabula.services.AutowiringRelationshipServiceComponent
import uk.ac.warwick.tabula.data.model.StudentRelationshipType
import uk.ac.warwick.tabula.commands.ComposableCommand
import uk.ac.warwick.tabula.commands.Describable
import uk.ac.warwick.tabula.commands.Description
import uk.ac.warwick.tabula.commands.SelfValidating
import org.springframework.validation.Errors

object DeleteStudentRelationshipTypeCommand {
	def apply(tpe: StudentRelationshipType) =
		new DeleteStudentRelationshipTypeCommandInternal(tpe)
			with ComposableCommand[StudentRelationshipType]
			with AutowiringRelationshipServiceComponent
			with DeleteStudentRelationshipTypeCommandPermissions
			with DeleteStudentRelationshipTypeCommandDescription
}

class DeleteStudentRelationshipTypeCommandInternal(val relationshipType: StudentRelationshipType) 
	extends CommandInternal[StudentRelationshipType] with HasExistingStudentRelationshipType with SelfValidating {
	this: RelationshipServiceComponent =>
		
	override def applyInternal() = transactional() {
		relationshipService.delete(relationshipType)
		relationshipType
	}
		
	def validate(errors: Errors) {
		// Don't allow removal if non-empty
		if (!relationshipType.empty) {
			errors.reject("errors.relationshipType.nonEmpty")
		}
	}
}

trait DeleteStudentRelationshipTypeCommandPermissions extends RequiresPermissionsChecking {
	this: HasExistingStudentRelationshipType =>
	
	def permissionsCheck(p: PermissionsChecking) {
		p.mandatory(relationshipType)
		p.PermissionCheck(Permissions.StudentRelationshipType.Delete)
	}
}

trait DeleteStudentRelationshipTypeCommandDescription extends Describable[StudentRelationshipType] {
	this: HasExistingStudentRelationshipType =>
		
	// describe the thing that's happening.
	override def describe(d: Description) =
		d.properties(
			"id" -> relationshipType.id,
			"urlPart" -> relationshipType.urlPart,
			"description" -> relationshipType.description
		)
}