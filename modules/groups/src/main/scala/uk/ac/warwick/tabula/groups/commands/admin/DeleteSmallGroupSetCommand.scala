package uk.ac.warwick.tabula.groups.commands.admin

import uk.ac.warwick.tabula.commands.SelfValidating
import org.springframework.validation.Errors
import uk.ac.warwick.tabula.data.model.groups.SmallGroupSet
import uk.ac.warwick.tabula.commands.Command
import uk.ac.warwick.tabula.commands.Description
import uk.ac.warwick.tabula.permissions.Permissions
import uk.ac.warwick.tabula.data.Transactions._
import uk.ac.warwick.tabula.services.SmallGroupService
import uk.ac.warwick.spring.Wire
import uk.ac.warwick.tabula.data.model.Module

class DeleteSmallGroupSetCommand(val module: Module, val set: SmallGroupSet) extends Command[SmallGroupSet] with SelfValidating {
	
	mustBeLinked(set, module)
	PermissionCheck(Permissions.SmallGroups.Delete, set)
	
	var service = Wire[SmallGroupService]
	
	var confirm = false
	
	override def applyInternal() = transactional() {
		set.markDeleted()
		service.saveOrUpdate(set)
		set
	}
	
	def validate(errors: Errors) {
		if (!confirm) {
			errors.rejectValue("confirm", "smallGroupSet.delete.confirm")
		} else validateCanDelete(errors)
	}
	
	def validateCanDelete(errors: Errors) {
		if (set.deleted) {
			errors.reject("smallGroupSet.delete.deleted")
		} else if (set.released) {
			errors.reject("smallGroupSet.delete.released")
		}
	}

	override def describe(d: Description) = d.smallGroupSet(set)

}