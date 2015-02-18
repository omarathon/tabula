package uk.ac.warwick.tabula.commands.sysadmin.pointsettemplates

import uk.ac.warwick.tabula.system.permissions.{PermissionsChecking, RequiresPermissionsChecking}
import uk.ac.warwick.tabula.permissions.Permissions

trait MonitoringPointSetTemplatesPermissions  extends RequiresPermissionsChecking {

	def permissionsCheck(p: PermissionsChecking) {
		p.PermissionCheck(Permissions.MonitoringPointTemplates.Manage)
	}

}
