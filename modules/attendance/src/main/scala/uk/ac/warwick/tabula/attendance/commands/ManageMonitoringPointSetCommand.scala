package uk.ac.warwick.tabula.attendance.commands

import uk.ac.warwick.tabula.commands._
import uk.ac.warwick.tabula.services.{MonitoringPointServiceComponent, AutowiringMonitoringPointServiceComponent, RouteServiceComponent, AutowiringRouteServiceComponent}
import uk.ac.warwick.tabula.data.model.{Route, Department}
import uk.ac.warwick.tabula.system.permissions.{PermissionsChecking, PermissionsCheckingMethods, RequiresPermissionsChecking}
import uk.ac.warwick.tabula.permissions.Permissions
import scala.collection.mutable
import scala.collection.JavaConverters._
import uk.ac.warwick.tabula.{ItemNotFoundException, AcademicYear}
import org.joda.time.DateTime
import uk.ac.warwick.tabula.data.model.attendance.{MonitoringPointSetTemplate, MonitoringPointSet}

object ManageMonitoringPointSetCommand {
	def apply(dept: Department, academicYearOption: Option[AcademicYear]) =
		new ManageMonitoringPointSetCommand(dept, academicYearOption)
		with ComposableCommand[Unit]
		with ManageMonitoringPointSetPermissions
		with AutowiringRouteServiceComponent
		with AutowiringMonitoringPointServiceComponent
		with ReadOnly with Unaudited
}


abstract class ManageMonitoringPointSetCommand(val dept: Department, val academicYearOption: Option[AcademicYear]) extends CommandInternal[Unit]
	with ManageMonitoringPointSetState {

	override def applyInternal() = {
		templates = monitoringPointService.listTemplates
	}
}

trait ManageMonitoringPointSetPermissions extends RequiresPermissionsChecking with PermissionsCheckingMethods {
	self: ManageMonitoringPointSetState =>

	override def permissionsCheck(p: PermissionsChecking) {
		p.PermissionCheck(Permissions.MonitoringPoints.Manage, mandatory(dept))
	}
}

trait ManageMonitoringPointSetState extends RouteServiceComponent with MonitoringPointServiceComponent {

	def dept: Department
	def academicYearOption: Option[AcademicYear]
	val thisAcademicYear = AcademicYear.guessByDate(new DateTime())
	val academicYear = academicYearOption.getOrElse(thisAcademicYear)
	val setsByRouteByAcademicYear = {
		val sets: mutable.HashMap[String, mutable.HashMap[Route, mutable.Buffer[MonitoringPointSet]]] = mutable.HashMap()
		dept.routes.asScala.collect{
			case r: Route => r.monitoringPointSets.asScala.filter(s =>
				s.academicYear.equals(thisAcademicYear.previous)
				|| s.academicYear.equals(thisAcademicYear)
				|| s.academicYear.equals(thisAcademicYear.next)
			)
		}.flatten.foreach{set =>
			sets
				.getOrElseUpdate(set.academicYear.toString, mutable.HashMap())
				.getOrElseUpdate(set.route, mutable.Buffer())
				.append(set)
		}
		sets
	}
	def setsByRouteCodeByAcademicYear(academicYear: String, route: Route) =
		setsByRouteByAcademicYear(academicYear)(route)

	var templates: Seq[MonitoringPointSetTemplate] = _

}
