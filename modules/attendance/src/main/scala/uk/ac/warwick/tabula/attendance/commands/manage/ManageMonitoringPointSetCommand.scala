package uk.ac.warwick.tabula.attendance.commands.manage

import uk.ac.warwick.tabula.commands._
import uk.ac.warwick.tabula.services.{MonitoringPointServiceComponent, AutowiringMonitoringPointServiceComponent, CourseAndRouteServiceComponent, AutowiringCourseAndRouteServiceComponent}
import uk.ac.warwick.tabula.data.model.{Route, Department}
import uk.ac.warwick.tabula.system.permissions.{PermissionsChecking, PermissionsCheckingMethods, RequiresPermissionsChecking}
import uk.ac.warwick.tabula.permissions.Permissions
import scala.collection.JavaConverters._
import uk.ac.warwick.tabula.AcademicYear
import org.joda.time.DateTime
import uk.ac.warwick.tabula.data.model.attendance.MonitoringPointSetTemplate
import uk.ac.warwick.tabula.permissions.CheckablePermission
import uk.ac.warwick.tabula.CurrentUser
import uk.ac.warwick.tabula.attendance.commands.{PermissionsAwareRoutes, AutowiringSecurityServicePermissionsAwareRoutes}

object ManageMonitoringPointSetCommand {
	def apply(user: CurrentUser, dept: Department, academicYearOption: Option[AcademicYear]) =
		new ManageMonitoringPointSetCommand(user, dept, academicYearOption)
			with AutowiringSecurityServicePermissionsAwareRoutes
			with ManageMonitoringPointSetPermissions
			with AutowiringCourseAndRouteServiceComponent
			with AutowiringMonitoringPointServiceComponent
			with ComposableCommand[Seq[MonitoringPointSetTemplate]]
			with ReadOnly with Unaudited
}


abstract class ManageMonitoringPointSetCommand(val user: CurrentUser, val dept: Department, val academicYearOption: Option[AcademicYear]) extends CommandInternal[Seq[MonitoringPointSetTemplate]]
	with ManageMonitoringPointSetState {

	override def applyInternal() = {
		monitoringPointService.listTemplates
	}
}

trait ManageMonitoringPointSetPermissions extends RequiresPermissionsChecking with PermissionsCheckingMethods with PermissionsAwareRoutes {
	self: ManageMonitoringPointSetState =>

	override def permissionsCheck(p: PermissionsChecking) {
		p.PermissionCheckAny(
			Seq(CheckablePermission(Permissions.MonitoringPoints.Manage, mandatory(dept))) ++
			routesForPermission(user, Permissions.MonitoringPoints.Manage, dept).map { route => CheckablePermission(Permissions.MonitoringPoints.Manage, route) }
		)
	}
}

trait ManageMonitoringPointSetState extends CourseAndRouteServiceComponent with MonitoringPointServiceComponent with PermissionsAwareRoutes {

	def dept: Department
	def user: CurrentUser
	def academicYearOption: Option[AcademicYear]
	val thisAcademicYear = AcademicYear.guessByDate(new DateTime())
	val academicYear = academicYearOption.getOrElse(thisAcademicYear)
	lazy val setsByRouteByAcademicYear = {
		routesForPermission(user, Permissions.MonitoringPoints.Manage, dept).toSeq.collect{
			case r: Route => r.monitoringPointSets.asScala.filter(s =>
				s.academicYear.equals(thisAcademicYear.previous)
				|| s.academicYear.equals(thisAcademicYear)
				|| s.academicYear.equals(thisAcademicYear.next)
			)
		}.flatten.groupBy(_.academicYear.toString).mapValues(_.groupBy(_.route))
	}
	def setsByRouteCodeByAcademicYear(academicYear: String, route: Route) =
		setsByRouteByAcademicYear(academicYear)(route)

	def sortedRoutesByAcademicYear(academicYear: String) =
		setsByRouteByAcademicYear(academicYear).keySet.toSeq.sorted(Route.DegreeTypeOrdering)

}
