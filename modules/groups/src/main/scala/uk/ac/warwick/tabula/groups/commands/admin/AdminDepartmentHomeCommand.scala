package uk.ac.warwick.tabula.groups.commands.admin

import scala.collection.JavaConverters._

import uk.ac.warwick.tabula.data.model.{Module, Department}
import uk.ac.warwick.tabula.{PermissionDeniedException, CurrentUser}
import uk.ac.warwick.tabula.commands.{Unaudited, ReadOnly, Command}
import uk.ac.warwick.spring.Wire
import uk.ac.warwick.tabula.services.{ModuleAndDepartmentService, SecurityService}
import uk.ac.warwick.tabula.permissions.Permissions

/**
 * Displays groups in a given department.
 */
class AdminDepartmentHomeCommand(val department: Department, val user: CurrentUser) extends Command[Seq[Module]] with ReadOnly with Unaudited {

	var securityService = Wire[SecurityService]
	var moduleService = Wire[ModuleAndDepartmentService]

	val modules: Seq[Module] =
		if (securityService.can(user, Permissions.Module.ManageSmallGroups, mandatory(department))) {
			// This may seem silly because it's rehashing the above; but it avoids an assertion error where we don't have any explicit permission definitions
			PermissionCheck(Permissions.Module.ManageSmallGroups, department)

			department.modules.asScala
		} else {
			val managedModules = moduleService.modulesWithPermission(user, Permissions.Module.ManageSmallGroups, department).toList

			// This is implied by the above, but it's nice to check anyway
			PermissionCheckAll(Permissions.Module.ManageSmallGroups, managedModules)

			if (managedModules.isEmpty)
				throw new PermissionDeniedException(user, Permissions.Module.ManageSmallGroups, department)

			managedModules
		}

	def applyInternal() = modules.sortBy { (module) => (module.groupSets.isEmpty, module.code) }

}
