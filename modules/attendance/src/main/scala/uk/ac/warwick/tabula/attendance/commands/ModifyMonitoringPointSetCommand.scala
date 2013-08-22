package uk.ac.warwick.tabula.attendance.commands

import uk.ac.warwick.tabula.helpers.StringUtils._
import uk.ac.warwick.tabula.JavaImports._
import uk.ac.warwick.tabula.commands._
import uk.ac.warwick.tabula.data.model.attendance.MonitoringPointSet
import uk.ac.warwick.tabula.services.{AutowiringRouteServiceComponent, RouteServiceComponent}
import uk.ac.warwick.tabula.system.permissions.{PermissionsCheckingMethods, PermissionsChecking, RequiresPermissionsChecking}
import uk.ac.warwick.tabula.permissions.Permissions
import uk.ac.warwick.tabula.data.model.Route
import org.springframework.validation.Errors
import org.joda.time.DateTime
import scala.collection.JavaConverters._
import uk.ac.warwick.tabula.AcademicYear

object ModifyMonitoringPointSetCommand {
	def apply(route: Route, set: MonitoringPointSet) =
		new ModifyMonitoringPointSetCommand(route, set)
		with ComposableCommand[MonitoringPointSet]
		with AutowiringRouteServiceComponent
		with ModifyMonitoringPointSetPermissions
		with ModifyMonitoringPointSetDescription
		with ModifyMonitoringPointSetValidation
}


class ModifyMonitoringPointSetCommand(val route: Route, val theSet: MonitoringPointSet) extends CommandInternal[MonitoringPointSet] with ModifyMonitoringPointSetState {
	self: RouteServiceComponent =>

	set = theSet

	override def applyInternal() = {
		set.templateName = templateNameToUse
		set.updatedDate = DateTime.now()
		routeService.save(set)
		set
	}
}

trait ModifyMonitoringPointSetValidation extends SelfValidating {
	self: ModifyMonitoringPointSetState with RouteServiceComponent =>

	override def validate(errors: Errors) {
		if (!templateNameToUse.hasText) {
			errors.rejectValue("templateName", "NotEmpty")
		}
	}
}

trait ModifyMonitoringPointSetDescription extends Describable[MonitoringPointSet] {
	self: ModifyMonitoringPointSetState =>

	override lazy val eventName = "ModifyMonitoringPointSet"

	override def describe(d: Description) {
		d.route(route)
		d.monitoringPointSet(set)
		d.property("templateName", templateNameToUse)
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
	var academicYear: AcademicYear = AcademicYear.guessByDate(new DateTime())
	var templateName: String = "1st Year Undergraduate"
	var customTemplateName: String = _
	var set: MonitoringPointSet = _

	def templateNameToUse = if (templateName.hasText) templateName else customTemplateName

	def copyTo(set: MonitoringPointSet) {
		set.route = route
		set.year = year
		set.academicYear = academicYear
		set.templateName = templateNameToUse
		set.updatedDate = DateTime.now()
	}

	val validYears: JList[String] =
		(
			if (route.monitoringPointSets.isEmpty) {
				(((1 to 8).toList) map (i => i.toString)) ++ List("All")
			}	else if (route.monitoringPointSets.size() == 1 && route.monitoringPointSets.get(0).year == null) {
				List()
			}	else {
				1 to 8 filter (y => !route.monitoringPointSets.asScala.map(s => s.year).contains(y)) map (i => i.toString)
			}
		).asJava
}
