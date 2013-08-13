package uk.ac.warwick.tabula.attendance.commands

import uk.ac.warwick.tabula.JavaImports._
import uk.ac.warwick.tabula.commands._
import uk.ac.warwick.tabula.data.model.attendance.{MonitoringPoint, MonitoringPointSet}
import uk.ac.warwick.tabula.services.{AutowiringRouteServiceComponent, RouteServiceComponent}
import uk.ac.warwick.tabula.system.permissions.{PermissionsChecking, RequiresPermissionsChecking}
import uk.ac.warwick.tabula.permissions.Permissions
import uk.ac.warwick.tabula.data.model.Route
import org.springframework.validation.Errors

object AddMonitoringPointSetCommand {
	def apply() =
		new AddMonitoringPointSetCommand()
		with ComposableCommand[MonitoringPointSet]
		with AutowiringRouteServiceComponent
		with ModifyMonitoringPointSetPermissions
		with AddMonitoringPointSetDescription
		with AddMonitoringPointSetValidation
}


class AddMonitoringPointSetCommand extends CommandInternal[MonitoringPointSet] with ModifyMonitoringPointSetState {
	self: RouteServiceComponent =>

	override def applyInternal() = {
		val set = new MonitoringPointSet
		routeService.save(set)
		set
	}
}

trait AddMonitoringPointSetValidation extends SelfValidating {
	self: ModifyMonitoringPointSetState with RouteServiceComponent =>

	override def validate(errors: Errors) {
		year match {
			case null => // fine
			case _ if year < 1  => errors.rejectValue("year", "monitoringPointSet.year.min")
			case _ if year > 99 => errors.rejectValue("year", "monitoringPointSet.year.calendar")
		}

		routeService.findMonitoringPointSet(this.route, Option(this.year)) foreach { existingSet =>
			errors.rejectValue("year", "monitoringPointSet.duplicate")
		}
	}
}

trait AddMonitoringPointSetDescription extends Describable[MonitoringPointSet] {
	self: ModifyMonitoringPointSetState =>

	override lazy val eventName = "AddMonitoringPointSet"

	override def describe(d: Description) {
		d.route(route)
		d.property("year", year)
	}
}

trait ModifyMonitoringPointSetPermissions extends RequiresPermissionsChecking {
	self: ModifyMonitoringPointSetState =>

	override def permissionsCheck(p: PermissionsChecking) {
		p.PermissionCheck(Permissions.MonitoringPoints.Manage, route)
	}
}

trait ModifyMonitoringPointSetState {
	var route: Route = _
	var year: JInteger = _
}
