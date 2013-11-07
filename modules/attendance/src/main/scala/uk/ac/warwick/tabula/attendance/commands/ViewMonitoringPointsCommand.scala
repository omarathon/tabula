package uk.ac.warwick.tabula.attendance.commands

import uk.ac.warwick.tabula.commands.{FiltersStudents, CommandInternal, ReadOnly, Unaudited, ComposableCommand}
import uk.ac.warwick.tabula.system.permissions.RequiresPermissionsChecking
import uk.ac.warwick.tabula.system.permissions.PermissionsCheckingMethods
import uk.ac.warwick.tabula.system.permissions.PermissionsChecking
import uk.ac.warwick.tabula.permissions.{CheckablePermission, Permissions}
import uk.ac.warwick.tabula.services.{AutowiringTermServiceComponent, AutowiringMonitoringPointServiceComponent, MonitoringPointServiceComponent, ProfileServiceComponent, AutowiringProfileServiceComponent}
import org.hibernate.criterion.Order._
import uk.ac.warwick.tabula.data.model._
import uk.ac.warwick.tabula.JavaImports._
import uk.ac.warwick.tabula.system.BindListener
import org.springframework.validation.BindingResult
import org.hibernate.criterion.Order
import uk.ac.warwick.tabula.{AcademicYear, CurrentUser}
import org.joda.time.DateTime
import scala.collection.JavaConverters._

object ViewMonitoringPointsCommand {
	def apply(department: Department, academicYearOption: Option[AcademicYear], user: CurrentUser) =
		new ViewMonitoringPointsCommand(department, academicYearOption, user)
			with ViewMonitoringPointsPermissions
			with AutowiringProfileServiceComponent
			with AutowiringSecurityServicePermissionsAwareRoutes
			with AutowiringMonitoringPointServiceComponent
			with AutowiringTermServiceComponent
			with ComposableCommand[Map[String, Seq[GroupedMonitoringPoint]]]
			with ReadOnly with Unaudited
}

abstract class ViewMonitoringPointsCommand(val department: Department, val academicYearOption: Option[AcademicYear], val user: CurrentUser)
	extends CommandInternal[Map[String, Seq[GroupedMonitoringPoint]]] with ViewMonitoringPointsState with BindListener with GroupMonitoringPointsByTerm {

	self: ProfileServiceComponent with MonitoringPointServiceComponent with PermissionsAwareRoutes =>
	
	def applyInternal() = {
		val pointSets = monitoringPointService.findPointSetsForStudents(students, academicYear)
		groupSimilarPointsByTerm(pointSets.flatMap(_.points.asScala), allRoutes, academicYear)
	}
	
	def onBind(result: BindingResult) {
		// Add all non-withdrawn codes to SPR statuses by default
		// TODO: What if the user wants to select 'Any' status? The collection will be empty
		if (sprStatuses.isEmpty) {
			allSprStatuses.filter { status => !status.code.startsWith("P") && !status.code.startsWith("T") }.foreach { sprStatuses.add }
		}
		routes = (routes.asScala.toSet & visibleRoutes).toSeq.asJava
		students = profileService.findAllStudentsByRestrictions(
			department = department,
			restrictions = buildRestrictions(),
			orders = buildOrders()
		)
	}
}

trait ViewMonitoringPointsState extends FiltersStudents with PermissionsAwareRoutes {
	def department: Department
	def academicYearOption: Option[AcademicYear]
	def user: CurrentUser

	val thisAcademicYear = AcademicYear.guessByDate(new DateTime())
	val academicYear = academicYearOption.getOrElse(thisAcademicYear)
	var students: Seq[StudentMember] = _

	val defaultOrder = Seq(asc("lastName"), asc("firstName")) // Don't allow this to be changed atm
	var sortOrder: JList[Order] = JArrayList()

	var courseTypes: JList[CourseType] = JArrayList()
	var routes: JList[Route] = JArrayList()
	lazy val visibleRoutes = routesForPermission(user, Permissions.MonitoringPoints.View, department)
	var modesOfAttendance: JList[ModeOfAttendance] = JArrayList()
	var yearsOfStudy: JList[JInteger] = JArrayList()
	var sprStatuses: JList[SitsStatus] = JArrayList()
	var modules: JList[Module] = JArrayList()
}

trait ViewMonitoringPointsPermissions extends RequiresPermissionsChecking with PermissionsCheckingMethods {
	self: ViewMonitoringPointsState with PermissionsAwareRoutes =>
	
	def permissionsCheck(p: PermissionsChecking) {
		p.PermissionCheckAny(
			Seq(CheckablePermission(Permissions.MonitoringPoints.View, mandatory(department))) ++
				routesForPermission(user, Permissions.MonitoringPoints.View, department).map { route => CheckablePermission(Permissions.MonitoringPoints.View, route) }
		)
	}
}