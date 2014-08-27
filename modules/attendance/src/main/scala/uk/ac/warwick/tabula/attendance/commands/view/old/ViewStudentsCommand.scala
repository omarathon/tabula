package uk.ac.warwick.tabula.attendance.commands.view.old

import org.hibernate.criterion.Order
import org.hibernate.criterion.Order._
import org.joda.time.DateTime
import org.springframework.validation.BindingResult
import uk.ac.warwick.tabula.JavaImports._
import uk.ac.warwick.tabula.attendance.commands.old.{AutowiringSecurityServicePermissionsAwareRoutes, BuildStudentPointsData, PermissionsAwareRoutes, StudentPointsData}
import uk.ac.warwick.tabula.commands.{CommandInternal, ComposableCommand, FiltersStudents, ReadOnly, Unaudited}
import uk.ac.warwick.tabula.data.model.{CourseType, Department, ModeOfAttendance, Module, Route, SitsStatus}
import uk.ac.warwick.tabula.permissions.{CheckablePermission, Permissions}
import uk.ac.warwick.tabula.services._
import uk.ac.warwick.tabula.system.BindListener
import uk.ac.warwick.tabula.system.permissions.{PermissionsChecking, PermissionsCheckingMethods, RequiresPermissionsChecking}
import uk.ac.warwick.tabula.{AcademicYear, CurrentUser}

import scala.collection.JavaConverters._

case class ViewStudentsResults(
	students: Seq[StudentPointsData],
	totalResults: Int
)

object ViewStudentsCommand {
	def apply(department: Department, academicYearOption: Option[AcademicYear], user: CurrentUser) =
		new ViewStudentsCommand(department, academicYearOption, user)
			with ViewStudentsPermissions
			with AutowiringUserLookupComponent
			with AutowiringProfileServiceComponent
			with AutowiringSecurityServicePermissionsAwareRoutes
			with AutowiringMonitoringPointServiceComponent
			with AutowiringTermServiceComponent
			with ComposableCommand[ViewStudentsResults]
			with ViewStudentsState
			with ReadOnly with Unaudited
}

abstract class ViewStudentsCommand(val department: Department, val academicYearOption: Option[AcademicYear], val user: CurrentUser)
	extends CommandInternal[ViewStudentsResults] with ViewStudentsState with BindListener with BuildStudentPointsData {
	self: UserLookupComponent with ProfileServiceComponent with MonitoringPointServiceComponent =>

	def applyInternal() = {
		val currentAcademicWeek = termService.getAcademicWeekForAcademicYear(DateTime.now(), academicYear)

		if (sortOrder.asScala.exists(o => o.getPropertyName == "missedMonitoringPoints")) {
			val filteredUniversityIds = profileService.findAllUniversityIdsByRestrictions(department, buildRestrictions())
			val sortedStudents = monitoringPointService.studentsByMissedCount(
				filteredUniversityIds,
				academicYear,
				sortOrder.asScala.filter(o => o.getPropertyName == "missedMonitoringPoints").head.isAscending,
				studentsPerPage,
				studentsPerPage * (page-1)
			)
			ViewStudentsResults(buildData(sortedStudents.map(_._1), academicYear, missedCounts = sortedStudents), filteredUniversityIds.size)
		} else if (sortOrder.asScala.exists(o => o.getPropertyName == "unrecordedMonitoringPoints")) {
			val filteredUniversityIds = profileService.findAllUniversityIdsByRestrictions(department, buildRestrictions())
			val sortedStudents = monitoringPointService.studentsByUnrecordedCount (
				filteredUniversityIds,
				academicYear,
				currentAcademicWeek,
				1,
				52,
				sortOrder.asScala.filter(o => o.getPropertyName == "unrecordedMonitoringPoints").head.isAscending,
				studentsPerPage,
				studentsPerPage * (page-1)
			)
			ViewStudentsResults(buildData(sortedStudents.map(_._1), academicYear, unrecordedCounts = sortedStudents), filteredUniversityIds.size)
		} else {
			val totalResults = profileService.countStudentsByRestrictions(
				department = department,
				restrictions = buildRestrictions()
			)

			val (offset, students) = profileService.findStudentsByRestrictions(
				department = department,
				restrictions = buildRestrictions(),
				orders = buildOrders(),
				maxResults = studentsPerPage,
				startResult = studentsPerPage * (page-1)
			)

			if (offset == 0) page = 1

			ViewStudentsResults(buildData(students, academicYear), totalResults)
		}

	}

	def onBind(result: BindingResult) {
		// Add all non-withdrawn codes to SPR statuses by default
		if (!hasBeenFiltered) {
			allSprStatuses.filter { status => !status.code.startsWith("P") && !status.code.startsWith("T") }.foreach { sprStatuses.add }
		}

		// Filter chosen routes by those that the user has permission to see
		routes = (routes.asScala.toSet & visibleRoutes).toSeq.asJava

		/** The above only works if routes isn't empty
			* (if routes IS empty there is NO route restriction, rather than 'show no routes').
			* If the user can't see ALL the routes, they can't select 'none' (which means 'Any'),
			* so if they pick none, change it to all the ones they can see.
			*/
		if (!canSeeAllRoutes && routes.size() == 0) {
			routes = visibleRoutes.toSeq.asJava
		}
	}
}

trait ViewStudentsPermissions extends RequiresPermissionsChecking with PermissionsCheckingMethods {
	self: ViewStudentsState with PermissionsAwareRoutes =>

	def permissionsCheck(p: PermissionsChecking) {
		p.PermissionCheckAny(
			Seq(CheckablePermission(Permissions.MonitoringPoints.View, mandatory(department))) ++
				routesForPermission(user, Permissions.MonitoringPoints.View, department).map { route => CheckablePermission(Permissions.MonitoringPoints.View, route) }
		)
	}
}

trait ViewStudentsState extends FiltersStudents with PermissionsAwareRoutes {
	def department: Department
	def academicYearOption: Option[AcademicYear]
	def user: CurrentUser

	val thisAcademicYear = AcademicYear.guessSITSAcademicYearByDate(new DateTime())
	val academicYear = academicYearOption.getOrElse(thisAcademicYear)

	var studentsPerPage = FiltersStudents.DefaultStudentsPerPage
	var page = 1

	val defaultOrder = Seq(asc("lastName"), asc("firstName")) // Don't allow this to be changed atm
	var sortOrder: JList[Order] = JArrayList()

	var courseTypes: JList[CourseType] = JArrayList()
	var routes: JList[Route] = JArrayList()
	lazy val visibleRoutes =
		if (department.routes.isEmpty) {
			routesForPermission(user, Permissions.MonitoringPoints.View, department.rootDepartment)
		} else routesForPermission(user, Permissions.MonitoringPoints.View, department)
	var modesOfAttendance: JList[ModeOfAttendance] = JArrayList()
	var yearsOfStudy: JList[JInteger] = JArrayList()
	var sprStatuses: JList[SitsStatus] = JArrayList()
	var modules: JList[Module] = JArrayList()

	// For Attendance Monitoring, we shouldn't consider sub-departments
	// but we will use the root department if the current dept has no routes at all
	override lazy val allRoutes =
		if (department.routes.isEmpty) {
			department.rootDepartment.routes.asScala.sorted(Route.DegreeTypeOrdering)
		} else department.routes.asScala.sorted(Route.DegreeTypeOrdering)


	lazy val canSeeAllRoutes = visibleRoutes.size == allRoutes.size

	var hasBeenFiltered = false
}