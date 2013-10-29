package uk.ac.warwick.tabula.attendance.commands

import uk.ac.warwick.tabula.commands._
import uk.ac.warwick.tabula.services._
import uk.ac.warwick.tabula.data.model.{StudentMember, Route, Department}
import uk.ac.warwick.tabula.system.permissions.{PermissionsChecking, PermissionsCheckingMethods, RequiresPermissionsChecking}
import uk.ac.warwick.tabula.permissions.Permissions
import scala.collection.mutable
import scala.collection.JavaConverters._
import uk.ac.warwick.tabula.AcademicYear
import org.joda.time.DateTime
import uk.ac.warwick.tabula.data.model.attendance.{MonitoringCheckpointState, MonitoringPoint, MonitoringPointSet}
import scala.Some
import uk.ac.warwick.tabula.permissions.CheckablePermission
import uk.ac.warwick.tabula.CurrentUser

object ViewMonitoringPointSetsCommand {
	def apply(user: CurrentUser, dept: Department, academicYearOption: Option[AcademicYear], routeOption: Option[Route], pointSetOption: Option[MonitoringPointSet]) =
		new ViewMonitoringPointSetsCommand(user, dept, academicYearOption, routeOption, pointSetOption)
			with AutowiringSecurityServicePermissionsAwareRoutes
			with ViewMonitoringPointSetsPermissions
			with AutowiringCourseAndRouteServiceComponent
			with AutowiringMonitoringPointServiceComponent
			with AutowiringTermServiceComponent
			with AutowiringProfileServiceComponent
			with ComposableCommand[Map[StudentMember, Map[MonitoringPoint, String]]]
			with ReadOnly with Unaudited
}


abstract class ViewMonitoringPointSetsCommand(
		val user: CurrentUser, val dept: Department, val academicYearOption: Option[AcademicYear],
		val routeOption: Option[Route], val pointSetOption: Option[MonitoringPointSet]
	)	extends CommandInternal[Map[StudentMember, Map[MonitoringPoint, String]]] with ViewMonitoringPointSetsState with MembersForPointSet {

	self: MonitoringPointServiceComponent with ProfileServiceComponent with TermServiceComponent with PermissionsAwareRoutes =>

	override def applyInternal() = {
		pointSetOption match {
			case Some(p) => {
				val members = getMembers(p)
				val currentAcademicWeek = termService.getAcademicWeekForAcademicYear(new DateTime(), academicYear)
				monitoringPointService.getChecked(members, p).filter{
					case (member, checkMap) =>
						checkMap.exists{
							case (_, Some(MonitoringCheckpointState.MissedUnauthorised)) => true
							case (point, None) => point.isLate(currentAcademicWeek)
							case _ => false
						}
				}.map{ case(member, checkMap) =>
					member -> checkMap.map{ case(point, option) => point -> (option match {
						case Some(state) => state.dbValue
						case _ =>
							if (point.isLate(currentAcademicWeek))
								"late"
							else
								""
					})}
				}
			}
			case None => Map()
		}
	}
}

trait ViewMonitoringPointSetsPermissions extends RequiresPermissionsChecking with PermissionsCheckingMethods {
	self: ViewMonitoringPointSetsState with PermissionsAwareRoutes =>

	override def permissionsCheck(p: PermissionsChecking) {
		p.PermissionCheckAny(
			Seq(CheckablePermission(Permissions.MonitoringPoints.View, mandatory(dept))) ++
			routesForPermission(user, Permissions.MonitoringPoints.View, dept).map { route => CheckablePermission(Permissions.MonitoringPoints.View, route) }
		)
	}
}

trait ViewMonitoringPointSetsState extends CourseAndRouteServiceComponent with MonitoringPointServiceComponent with GroupMonitoringPointsByTerm {
	self: PermissionsAwareRoutes =>

	def dept: Department
	def user: CurrentUser
	def academicYearOption: Option[AcademicYear]
	def routeOption: Option[Route]
	def pointSetOption: Option[MonitoringPointSet]

	val thisAcademicYear = AcademicYear.guessByDate(new DateTime())
	val academicYear = academicYearOption.getOrElse(thisAcademicYear)
	val route = routeOption.getOrElse(null)
	val pointSet = pointSetOption.getOrElse(null)

	lazy val setsByRouteByAcademicYear = {
		routesForPermission(user, Permissions.MonitoringPoints.View, dept).toSeq.sorted(Route.DegreeTypeOrdering).collect{
			case r: Route => r.monitoringPointSets.asScala.filter(s =>
				s.academicYear.equals(thisAcademicYear.previous)
				|| s.academicYear.equals(thisAcademicYear)
				|| s.academicYear.equals(thisAcademicYear.next)
			)
		}.flatten.sortBy(set => Option(set.year)).groupBy(_.academicYear.toString).mapValues(_.groupBy(_.route))
	}
	def setsByRouteCodeByAcademicYear(academicYear: String, route: Route) =
		setsByRouteByAcademicYear(academicYear)(route)

	def sortedRoutesByAcademicYear(academicYear: String) =
		setsByRouteByAcademicYear(academicYear).keySet.toSeq.sorted(Route.DegreeTypeOrdering)

	def monitoringPointsByTerm = groupByTerm(pointSet.points.asScala, academicYear)

}
