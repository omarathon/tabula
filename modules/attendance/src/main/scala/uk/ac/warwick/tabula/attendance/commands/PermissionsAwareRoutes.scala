package uk.ac.warwick.tabula.attendance.commands

import uk.ac.warwick.tabula.data.model.Department
import uk.ac.warwick.tabula.permissions.Permission
import scala.collection.JavaConverters._
import uk.ac.warwick.tabula.data.model.Route
import uk.ac.warwick.tabula.services.SecurityServiceComponent
import uk.ac.warwick.tabula.CurrentUser
import uk.ac.warwick.tabula.system.permissions.PermissionsCheckingMethods
import uk.ac.warwick.tabula.services.ModuleAndDepartmentServiceComponent

trait PermissionsAwareRoutes {
	def routesForPermission(user: CurrentUser, p: Permission, dept: Department): Set[Route]
}

trait SecurityServicePermissionsAwareRoutes extends PermissionsAwareRoutes with PermissionsCheckingMethods {
	self: SecurityServiceComponent with ModuleAndDepartmentServiceComponent =>
	
	def routesForPermission(user: CurrentUser, p: Permission, dept: Department): Set[Route] =
		if (securityService.can(user, p, mandatory(dept))) dept.routes.asScala.toSet
		else moduleAndDepartmentService.routesWithPermission(user, p, dept)
}