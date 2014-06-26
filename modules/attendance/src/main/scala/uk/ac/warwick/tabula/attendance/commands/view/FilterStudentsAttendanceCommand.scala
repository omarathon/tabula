package uk.ac.warwick.tabula.attendance.commands.view

import org.hibernate.criterion.Order
import org.hibernate.criterion.Order._
import uk.ac.warwick.tabula.JavaImports._
import uk.ac.warwick.tabula.attendance.commands.{AutowiringSecurityServicePermissionsAwareRoutes, PermissionsAwareRoutes}
import uk.ac.warwick.tabula.commands._
import uk.ac.warwick.tabula.data.ScalaRestriction
import uk.ac.warwick.tabula.data.model._
import uk.ac.warwick.tabula.permissions.Permissions
import uk.ac.warwick.tabula.services.{AttendanceMonitoringServiceComponent, AutowiringAttendanceMonitoringServiceComponent, AutowiringProfileServiceComponent, AutowiringTermServiceComponent, ProfileServiceComponent, TermServiceComponent}
import uk.ac.warwick.tabula.system.permissions.{PermissionsChecking, PermissionsCheckingMethods, RequiresPermissionsChecking}
import uk.ac.warwick.tabula.{AcademicYear, CurrentUser}

import scala.collection.JavaConverters._

object FilterStudentsAttendanceCommand {
	def apply(department: Department, academicYear: AcademicYear, user: CurrentUser) =
		new FilterStudentsAttendanceCommandInternal(department, academicYear, user)
			with AutowiringSecurityServicePermissionsAwareRoutes
			with AutowiringProfileServiceComponent
			with AutowiringTermServiceComponent
			with AutowiringAttendanceMonitoringServiceComponent
			with ComposableCommand[FilteredStudentsAttendanceResult]
			with FilterStudentsAttendancePermissions
			with FilterStudentsAttendanceCommandState
			with ReadOnly with Unaudited
}


class FilterStudentsAttendanceCommandInternal(val department: Department, val academicYear: AcademicYear, val user: CurrentUser)
	extends CommandInternal[FilteredStudentsAttendanceResult] with BuildsFilteredStudentsAttendanceResult with TaskBenchmarking {

	self: FilterStudentsAttendanceCommandState with TermServiceComponent
		with ProfileServiceComponent with AttendanceMonitoringServiceComponent =>

	override def applyInternal() = {
		val totalResults = benchmarkTask("profileService.countStudentsByRestrictionsInAffiliatedDepartments") {
			profileService.countStudentsByRestrictionsInAffiliatedDepartments(
				department = department,
				restrictions = buildRestrictions()
			)
		}

		val (offset, students) = benchmarkTask("profileService.findStudentsByRestrictionsInAffiliatedDepartments") {
			profileService.findStudentsByRestrictionsInAffiliatedDepartments(
				department = department,
				restrictions = buildRestrictions(),
				orders = buildOrders(),
				maxResults = studentsPerPage,
				startResult = studentsPerPage * (page - 1)
			)
		}

		if (offset == 0) page = 1

		buildAttendanceResult(totalResults, students, Option(department), academicYear)
	}

}

trait FilterStudentsAttendancePermissions extends RequiresPermissionsChecking with PermissionsCheckingMethods {

	self: FilterStudentsAttendanceCommandState =>

	override def permissionsCheck(p: PermissionsChecking) {
		p.PermissionCheck(Permissions.MonitoringPoints.View, department)
	}

}

trait FilterStudentsAttendanceCommandState extends AttendanceFilterExtras with PermissionsAwareRoutes {
	def department: Department
	def academicYear: AcademicYear
	def user: CurrentUser

	var studentsPerPage = FiltersStudents.DefaultStudentsPerPage
	val defaultOrder = Seq(asc("lastName"), asc("firstName")) // Don't allow this to be changed atm

	// Bind variables

	var page = 1
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

}

trait AttendanceFilterExtras extends FiltersStudents {

	final val UNAUTHORISED = "Missed (unauthorised)"
	final val AUTHORISED = "Missed (authorised)"
	final val UNRECORDED = "Unrecorded"

	// For Attendance Monitoring, we shouldn't consider sub-departments
	// but we will use the root department if the current dept has no routes at all
	override lazy val allRoutes =
		if (department.routes.isEmpty) {
			department.rootDepartment.routes.asScala.sorted(Route.DegreeTypeOrdering)
		} else department.routes.asScala.sorted(Route.DegreeTypeOrdering)

	override lazy val allOtherCriteria: Seq[String] = Seq(
		"Tier 4 only",
		"Visiting",
		UNAUTHORISED,
		AUTHORISED,
		UNRECORDED
	)

	override def getAliasPaths(table: String) = {
		(FiltersStudents.AliasPaths ++ Map(
			"attendanceCheckpointTotals" -> Seq(
				"attendanceCheckpointTotals" -> "attendanceCheckpointTotals"
			)
		))(table)
	}

	override def buildRestrictions(): Seq[ScalaRestriction] = {
		super.buildRestrictions() ++ Seq(
			attendanceCheckpointTotalsRestriction,
			unrecordedAttendanceRestriction,
			authorisedAttendanceRestriction,
			unauthorisedAttendanceRestriction
		).flatten
	}

	def attendanceCheckpointTotalsRestriction: Option[ScalaRestriction] =
		// Only apply if required (otherwise only students with a checkpoint total are returned)
		Seq(unrecordedAttendanceRestriction, authorisedAttendanceRestriction, unauthorisedAttendanceRestriction).flatten.isEmpty match {
			case true => None
			case false =>  ScalaRestriction.is(
				"attendanceCheckpointTotals.department",
				department,
				getAliasPaths("attendanceCheckpointTotals"): _*
			)
		}

	def unrecordedAttendanceRestriction: Option[ScalaRestriction] = otherCriteria.contains(UNRECORDED) match {
		case false => None
		case true => ScalaRestriction.gt(
			"attendanceCheckpointTotals.unrecorded",
			0,
			getAliasPaths("attendanceCheckpointTotals"): _*
		)
	}

	def authorisedAttendanceRestriction: Option[ScalaRestriction] = otherCriteria.contains(AUTHORISED) match {
		case false => None
		case true => ScalaRestriction.gt(
			"attendanceCheckpointTotals.authorised",
			0,
			getAliasPaths("attendanceCheckpointTotals"): _*
		)
	}

	def unauthorisedAttendanceRestriction: Option[ScalaRestriction] = otherCriteria.contains(UNAUTHORISED) match {
		case false => None
		case true => ScalaRestriction.gt(
			"attendanceCheckpointTotals.unauthorised",
			0,
			getAliasPaths("attendanceCheckpointTotals"): _*
		)
	}
}
