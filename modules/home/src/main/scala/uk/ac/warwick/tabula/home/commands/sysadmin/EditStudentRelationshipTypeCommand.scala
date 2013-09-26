package uk.ac.warwick.tabula.home.commands.sysadmin

import uk.ac.warwick.tabula.commands.ComposableCommand
import uk.ac.warwick.tabula.data.Transactions._
import uk.ac.warwick.tabula.data.model.StudentRelationshipType
import uk.ac.warwick.tabula.permissions.Permissions
import uk.ac.warwick.tabula.services.AutowiringRelationshipServiceComponent

import uk.ac.warwick.tabula.services.RelationshipServiceComponent
import uk.ac.warwick.tabula.system.permissions.PermissionsChecking
import uk.ac.warwick.tabula.system.permissions.RequiresPermissionsChecking

object EditStudentRelationshipTypeCommand {
	def apply(tpe: StudentRelationshipType) =
		new EditStudentRelationshipTypeCommandInternal(tpe)
			with ComposableCommand[StudentRelationshipType]
			with AutowiringRelationshipServiceComponent
			with EditStudentRelationshipTypeCommandPermissions
			with ModifyStudentRelationshipTypeCommandDescription
}

class EditStudentRelationshipTypeCommandInternal(val relationshipType: StudentRelationshipType) 
	extends ModifyStudentRelationshipTypeCommandInternal with HasExistingStudentRelationshipType {
	this: RelationshipServiceComponent =>
		
	this.copyFrom(relationshipType)
		
	override def applyInternal() = transactional() {
		copyTo(relationshipType)
		relationshipService.saveOrUpdate(relationshipType)
		relationshipType
	}
}

trait EditStudentRelationshipTypeCommandPermissions extends RequiresPermissionsChecking {
	this: HasExistingStudentRelationshipType =>
	
	def permissionsCheck(p: PermissionsChecking) {
		p.mandatory(relationshipType)
		p.PermissionCheck(Permissions.StudentRelationshipType.Update)
	}
}