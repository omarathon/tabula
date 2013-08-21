package uk.ac.warwick.tabula.attendance.commands

import uk.ac.warwick.tabula.commands.CommandInternal
import uk.ac.warwick.tabula.commands.Appliable
import uk.ac.warwick.tabula.services.RouteServiceComponent
import uk.ac.warwick.tabula.services.AutowiringRouteServiceComponent
import uk.ac.warwick.tabula.data.model.attendance.{MonitoringPoint, MonitoringPointSet}
import uk.ac.warwick.tabula.commands.Unaudited
import uk.ac.warwick.tabula.commands.ReadOnly
import uk.ac.warwick.tabula.commands.ComposableCommand
import uk.ac.warwick.tabula.system.permissions.PermissionsChecking
import uk.ac.warwick.tabula.system.permissions.RequiresPermissionsChecking
import uk.ac.warwick.tabula.permissions.Permissions
import uk.ac.warwick.tabula.data.model.Route
import scala.collection.JavaConverters._

object GetMonitoringPointsCommand {
	def apply(route: Route, year: Option[Int]) =
		new GetMonitoringPointsCommand(route, year)
			with ComposableCommand[Option[Pair[MonitoringPointSet, Map[String, Seq[MonitoringPoint]]]]]
			with GetMonitoringPointsCommandPermissions
			with AutowiringRouteServiceComponent
			with ReadOnly 
			with Unaudited 
}

abstract class GetMonitoringPointsCommand(val route: Route, val year: Option[Int])
	extends CommandInternal[Option[Pair[MonitoringPointSet, Map[String, Seq[MonitoringPoint]]]]]
	with Appliable[Option[Pair[MonitoringPointSet, Map[String, Seq[MonitoringPoint]]]]]
			with GetMonitoringPointsCommandState {
	self: RouteServiceComponent =>

	def applyInternal(): Option[Pair[MonitoringPointSet, Map[String, Seq[MonitoringPoint]]]] = {
		routeService.findMonitoringPointSet(route, year) match {
			case None => None
			case pso:Some[MonitoringPointSet] => Option(Pair(pso.get, groupByTerm(pso.get)))
		}

	}

	// TODO: Replace this naive way of grouping with real term weeks
	private def groupByTerm(pointSet: MonitoringPointSet) = {
		pointSet.points.asScala.groupBy {
			case point if (point.week < 11) => "Autumn"
			case point if (point.week < 16) => "Christmas vacation"
			case point if (point.week < 26) => "Spring"
			case point if (point.week < 31) => "Easter vacation"
			case point if (point.week < 41) => "Summer"
			case _ => "Summer vacation"
		}
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