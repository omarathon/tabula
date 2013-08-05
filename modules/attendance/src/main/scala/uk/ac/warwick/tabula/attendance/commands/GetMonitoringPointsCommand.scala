package uk.ac.warwick.tabula.attendance.commands

import uk.ac.warwick.tabula.commands.CommandInternal
import uk.ac.warwick.tabula.commands.Appliable
import uk.ac.warwick.tabula.services.RouteServiceComponent
import uk.ac.warwick.tabula.services.AutowiringRouteServiceComponent
import uk.ac.warwick.tabula.data.model.attendance.MonitoringPointSet
import uk.ac.warwick.tabula.commands.Unaudited
import uk.ac.warwick.tabula.commands.ReadOnly
import scala.collection.JavaConverters._
import uk.ac.warwick.tabula.commands.ComposableCommand
import uk.ac.warwick.tabula.system.permissions.PermissionsChecking
import uk.ac.warwick.tabula.system.permissions.RequiresPermissionsChecking
import uk.ac.warwick.tabula.permissions.Permissions
import uk.ac.warwick.tabula.data.model.Route

object GetMonitoringPointsCommand {	
	def apply(route: Route, year: Option[Int]) =
		new GetMonitoringPointsCommand(route, year) 
			with ComposableCommand[Option[MonitoringPointSet]]	
			with GetMonitoringPointsCommandPermissions
			with AutowiringRouteServiceComponent
			with ReadOnly 
			with Unaudited 
}

abstract class GetMonitoringPointsCommand(val route: Route, val year: Option[Int]) 
	extends CommandInternal[Option[MonitoringPointSet]] with Appliable[Option[MonitoringPointSet]] 
			with GetMonitoringPointsCommandState {
	self: RouteServiceComponent =>

	def applyInternal(): Option[MonitoringPointSet] = {
		routeService.findMonitoringPointSet(route, year)
	}

}

trait GetMonitoringPointsCommandPermissions extends RequiresPermissionsChecking {
	self: GetMonitoringPointsCommandState =>
	def permissionsCheck(p: PermissionsChecking) {
		p.PermissionCheck(Permissions.MonitoringPoints.Manage, route)
	}
}

trait GetMonitoringPointsCommandState {
	val route: Route
	val year: Option[Int]
}