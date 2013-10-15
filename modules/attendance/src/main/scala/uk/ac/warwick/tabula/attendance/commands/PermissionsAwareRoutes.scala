package uk.ac.warwick.tabula.attendance.commands

import uk.ac.warwick.tabula.data.model.Department
import uk.ac.warwick.tabula.permissions.Permission
import scala.collection.JavaConverters._
import uk.ac.warwick.tabula.data.model.Route
import uk.ac.warwick.tabula.services.SecurityServiceComponent
import uk.ac.warwick.tabula.CurrentUser
import uk.ac.warwick.tabula.system.permissions.PermissionsCheckingMethods
import uk.ac.warwick.tabula.services.ModuleAndDepartmentServiceComponent
import uk.ac.warwick.tabula.services.SecurityService
import uk.ac.warwick.spring.Wire
import uk.ac.warwick.tabula.services.AutowiringSecurityServiceComponent
import uk.ac.warwick.tabula.services.AutowiringModuleAndDepartmentServiceComponent

trait PermissionsAwareRoutes {
	def routesForPermission(user: CurrentUser, p: Permission, dept: Department): Set[Route]
}

trait AutowiringSecurityServicePermissionsAwareRoutes 
	extends PermissionsAwareRoutes 
		with PermissionsCheckingMethods 
		with AutowiringSecurityServiceComponent 
		with AutowiringModuleAndDepartmentServiceComponent {
	
	def routesForPermission(user: CurrentUser, p: Permission, dept: Department): Set[Route] = {
		if (securityService.can(user, p, mandatory(dept))) dept.routes.asScala.toSet
		else moduleAndDepartmentService.routesWithPermission(user, p, dept)
	}
	
}