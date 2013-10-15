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
		with ComposableCommand[Unit]
		with ViewMonitoringPointSetsPermissions
		with AutowiringRouteServiceComponent
		with AutowiringMonitoringPointServiceComponent
		with AutowiringTermServiceComponent
		with AutowiringProfileServiceComponent
		with SecurityServicePermissionsAwareRoutes
		with AutowiringSecurityServiceComponent
		with AutowiringModuleAndDepartmentServiceComponent
		with ReadOnly with Unaudited
}


abstract class ViewMonitoringPointSetsCommand(
		val user: CurrentUser, val dept: Department, val academicYearOption: Option[AcademicYear],
		val routeOption: Option[Route], val pointSetOption: Option[MonitoringPointSet]
	)	extends CommandInternal[Unit] with ViewMonitoringPointSetsState with MembersForPointSet {

	self: MonitoringPointServiceComponent with ProfileServiceComponent with TermServiceComponent =>

	override def applyInternal() = {
		pointSetOption match {
			case Some(p) => {
				val members = getMembers(p)
				val currentAcademicWeek = termService.getAcademicWeekForAcademicYear(new DateTime(), academicYear)
				membersWithMissedOrLateCheckpoints = monitoringPointService.getChecked(members, p).filter{
					case (member, checkMap) =>
						checkMap.exists{
							case (_, Some(MonitoringCheckpointState.MissedUnauthorised)) => true
							case (point, None) => currentAcademicWeek >= point.requiredFromWeek
							case _ => false
						}
				}.map{ case(member, checkMap) =>
					member -> checkMap.map{ case(point, option) => point -> (option match {
						case Some(state) => state.dbValue
						case _ =>
							if (currentAcademicWeek >= point.requiredFromWeek)
								"late"
							else
								""
					})}
				}
			}
			case None =>
		}
	}
}

trait ViewMonitoringPointSetsPermissions extends RequiresPermissionsChecking with PermissionsCheckingMethods with PermissionsAwareRoutes {
	self: ViewMonitoringPointSetsState =>

	override def permissionsCheck(p: PermissionsChecking) {
		p.PermissionCheckAny(
			Seq(CheckablePermission(Permissions.MonitoringPoints.Manage, mandatory(dept))) ++
			routesForPermission(user, Permissions.MonitoringPoints.Manage, dept).map { route => CheckablePermission(Permissions.MonitoringPoints.Manage, route) }
		)
	}
}

trait ViewMonitoringPointSetsState extends RouteServiceComponent with MonitoringPointServiceComponent with GroupMonitoringPointsByTerm with PermissionsAwareRoutes {

	def dept: Department
	def user: CurrentUser
	def academicYearOption: Option[AcademicYear]
	def routeOption: Option[Route]
	def pointSetOption: Option[MonitoringPointSet]

	val thisAcademicYear = AcademicYear.guessByDate(new DateTime())
	val academicYear = academicYearOption.getOrElse(thisAcademicYear)
	val route = routeOption.getOrElse(null)
	val pointSet = pointSetOption.getOrElse(null)

	val setsByRouteByAcademicYear = {
		val sets: mutable.HashMap[String, mutable.HashMap[Route, mutable.Buffer[MonitoringPointSet]]] = mutable.HashMap()
		routesForPermission(user, Permissions.MonitoringPoints.Manage, dept).toSeq.collect{
			case r: Route => r.monitoringPointSets.asScala.filter(s =>
				s.academicYear.equals(thisAcademicYear.previous)
				|| s.academicYear.equals(thisAcademicYear)
				|| s.academicYear.equals(thisAcademicYear.next)
			)
		}.flatten.sortWith{(a, b) =>
			if (a.year == null)
				true
			else if (b.year == null)
				false
			else
				a.year < b.year
		}.foreach{set =>
			sets
				.getOrElseUpdate(set.academicYear.toString, mutable.HashMap())
				.getOrElseUpdate(set.route, mutable.Buffer())
				.append(set)
		}
		sets
	}
	def setsByRouteCodeByAcademicYear(academicYear: String, route: Route) =
		setsByRouteByAcademicYear(academicYear)(route)

	def monitoringPointsByTerm = groupByTerm(pointSet.points.asScala, academicYear)

	var membersWithMissedOrLateCheckpoints: Map[StudentMember, Map[MonitoringPoint, String]] = _

	def missedCheckpointsByMember(member: StudentMember) =
		membersWithMissedOrLateCheckpoints(member)

	def missedCheckpointsByMemberByPoint(member: StudentMember, point: MonitoringPoint) =
		membersWithMissedOrLateCheckpoints(member)(point)

}
