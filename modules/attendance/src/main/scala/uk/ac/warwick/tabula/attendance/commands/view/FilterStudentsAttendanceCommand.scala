package uk.ac.warwick.tabula.attendance.commands.view

import uk.ac.warwick.tabula.commands._
import uk.ac.warwick.tabula.system.permissions.{PermissionsChecking, PermissionsCheckingMethods, RequiresPermissionsChecking}
import uk.ac.warwick.tabula.permissions.Permissions
import uk.ac.warwick.tabula.data.model._
import uk.ac.warwick.tabula.{CurrentUser, AcademicYear}
import org.hibernate.criterion.Order._
import uk.ac.warwick.tabula.JavaImports._
import org.hibernate.criterion.Order
import uk.ac.warwick.tabula.attendance.commands.{GroupsPoints, AutowiringSecurityServicePermissionsAwareRoutes, PermissionsAwareRoutes}
import uk.ac.warwick.tabula.services.{AutowiringAttendanceMonitoringServiceComponent, AutowiringTermServiceComponent, TermServiceComponent, AttendanceMonitoringServiceComponent, ProfileServiceComponent, AutowiringProfileServiceComponent}
import collection.JavaConverters._
import uk.ac.warwick.tabula.data.ScalaRestriction
import uk.ac.warwick.tabula.data.model.attendance.{AttendanceMonitoringCheckpointTotal, AttendanceMonitoringCheckpoint, AttendanceMonitoringPoint}

case class FilteredStudentResult(
	student: StudentMember,
	groupedPointCheckpointPairs: Map[String, Seq[(AttendanceMonitoringPoint, AttendanceMonitoringCheckpoint)]],
	attendanceNotes: Map[AttendanceMonitoringPoint, AttendanceNote],
	checkpointTotal: AttendanceMonitoringCheckpointTotal
)

case class FilterStudentsAttendanceCommandResult(
	totalResults: Int,
	results: Seq[FilteredStudentResult],
	students: Seq[StudentMember]
)

object FilterStudentsAttendanceCommand {
	def apply(department: Department, academicYear: AcademicYear, user: CurrentUser) =
		new FilterStudentsAttendanceCommandInternal(department, academicYear, user)
			with AutowiringSecurityServicePermissionsAwareRoutes
			with AutowiringProfileServiceComponent
			with AutowiringTermServiceComponent
			with AutowiringAttendanceMonitoringServiceComponent
			with ComposableCommand[FilterStudentsAttendanceCommandResult]
			with FilterStudentsAttendancePermissions
			with FilterStudentsAttendanceCommandState
			with ReadOnly with Unaudited
}


class FilterStudentsAttendanceCommandInternal(val department: Department, val academicYear: AcademicYear, val user: CurrentUser)
	extends CommandInternal[FilterStudentsAttendanceCommandResult] with GroupsPoints with TaskBenchmarking {

	self: FilterStudentsAttendanceCommandState with TermServiceComponent
		with ProfileServiceComponent with AttendanceMonitoringServiceComponent =>

	override def applyInternal() = {
		val totalResults = benchmarkTask("profileService.countStudentsByRestrictions") {
			profileService.countStudentsByRestrictions(
				department = department,
				restrictions = buildRestrictions()
			)
		}

		val (offset, students) = benchmarkTask("profileService.findStudentsByRestrictionss") {
			profileService.findStudentsByRestrictions(
				department = department,
				restrictions = buildRestrictions(),
				orders = buildOrders(),
				maxResults = studentsPerPage,
				startResult = studentsPerPage * (page - 1)
			)
		}

		if (offset == 0) page = 1

		val results = benchmarkTask("Build FilteredStudentResults"){ students.map { student=>
			val points = benchmarkTask("listStudentsPoints") {
				attendanceMonitoringService.listStudentsPoints(student, department, academicYear)
			}
			val checkpointMap = benchmarkTask("getCheckpoints") {
				attendanceMonitoringService.getCheckpoints(points, student)
			}
			val groupedPoints = benchmarkTask("groupedPoints") {
				groupByTerm(points, groupSimilar = false) ++ groupByMonth(points, groupSimilar = false)
			}
			val groupedPointCheckpointPairs = benchmarkTask("groupedPointCheckpointPairs") {
				groupedPoints.map { case (period, thesePoints) =>
					period -> thesePoints.map { groupedPoint =>
						groupedPoint.templatePoint -> checkpointMap.get(groupedPoint.templatePoint).getOrElse(null)
					}
				}
			}
			val attendanceNotes = benchmarkTask("attendanceNotes") {
				attendanceMonitoringService.getAttendanceNoteMap(student)
			}
			val checkpointTotal = benchmarkTask("checkpointTotal") {
				attendanceMonitoringService.getCheckpointTotal(student, department, academicYear)
			}
			FilteredStudentResult(student, groupedPointCheckpointPairs, attendanceNotes, checkpointTotal)
		}}
		FilterStudentsAttendanceCommandResult(totalResults, results, students)
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

	def attendanceCheckpointTotalsRestriction: Option[ScalaRestriction] = ScalaRestriction.is(
		"attendanceCheckpointTotals.department",
		department,
		getAliasPaths("attendanceCheckpointTotals"): _*
	)

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
