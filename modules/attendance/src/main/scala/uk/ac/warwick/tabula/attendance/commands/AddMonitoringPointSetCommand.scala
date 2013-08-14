package uk.ac.warwick.tabula.attendance.commands

import uk.ac.warwick.tabula.helpers.StringUtils._
import uk.ac.warwick.tabula.JavaImports._
import uk.ac.warwick.tabula.commands._
import uk.ac.warwick.tabula.data.model.attendance.{MonitoringPoint, MonitoringPointSet}
import uk.ac.warwick.tabula.services.{AutowiringRouteServiceComponent, RouteServiceComponent}
import uk.ac.warwick.tabula.system.permissions.{PermissionsCheckingMethods, PermissionsChecking, RequiresPermissionsChecking}
import uk.ac.warwick.tabula.permissions.Permissions
import uk.ac.warwick.tabula.data.model.Route
import org.springframework.validation.Errors
import org.joda.time.DateTime

object AddMonitoringPointSetCommand {
	def apply(route: Route) =
		new AddMonitoringPointSetCommand(route)
		with ComposableCommand[MonitoringPointSet]
		with AutowiringRouteServiceComponent
		with ModifyMonitoringPointSetPermissions
		with AddMonitoringPointSetDescription
		with AddMonitoringPointSetValidation
}


class AddMonitoringPointSetCommand(val route: Route) extends CommandInternal[MonitoringPointSet] with ModifyMonitoringPointSetState {
	self: RouteServiceComponent =>

	override def applyInternal() = {
		val set = new MonitoringPointSet
		this.copyTo(set)
		routeService.save(set)
		set
	}
}

trait AddMonitoringPointSetValidation extends SelfValidating {
	self: ModifyMonitoringPointSetState with RouteServiceComponent =>

	override def validate(errors: Errors) {
		year match {
			case y if y < 1  => errors.rejectValue("year", "monitoringPointSet.year.min")
			case y if y > 99 => errors.rejectValue("year", "monitoringPointSet.year.calendar")
			case _ => // null or within range, fine either way.
		}

		if (!templateNameToUse.hasText) {
			errors.rejectValue("templateName", "NotEmpty")
		}

		val existingPointSets = routeService.findMonitoringPointSets(this.route)
		if (year == null && !existingPointSets.isEmpty) {
			// existing sets with years - can't add one without year
			errors.rejectValue("year", "monitoringPointSet.alreadyYearBased")
		} else if (existingPointSets exists { _.year == this.year }) {
			// existing set with this year
			errors.rejectValue("year", "monitoringPointSet.duplicate")
		} else if (existingPointSets exists { _.year == null }) {
			// existing set matching all years - can't add a year-specific one
			errors.rejectValue("year", "monitoringPointSet.notYearBased")
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

trait ModifyMonitoringPointSetPermissions extends RequiresPermissionsChecking with PermissionsCheckingMethods {
	self: ModifyMonitoringPointSetState =>

	override def permissionsCheck(p: PermissionsChecking) {
		p.PermissionCheck(Permissions.MonitoringPoints.Manage, mandatory(route))
	}
}

trait ModifyMonitoringPointSetState {

	def route: Route
	var year: JInteger = _
	var templateName: String = _
	var customTemplateName: String = _

	def templateNameToUse = if (templateName.hasText) templateName else customTemplateName

	def copyTo(set: MonitoringPointSet) {
		set.route = route
		set.year = year
		set.templateName = templateNameToUse
		set.updatedDate = DateTime.now()
	}
}
