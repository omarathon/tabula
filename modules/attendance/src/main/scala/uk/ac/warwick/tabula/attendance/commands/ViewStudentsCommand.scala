package uk.ac.warwick.tabula.attendance.commands

import uk.ac.warwick.tabula.data.model.StudentMember
import uk.ac.warwick.tabula.commands.{FiltersStudents, CommandInternal, ReadOnly, Unaudited, ComposableCommand}
import uk.ac.warwick.tabula.system.permissions.RequiresPermissionsChecking
import uk.ac.warwick.tabula.system.permissions.PermissionsCheckingMethods
import uk.ac.warwick.tabula.system.permissions.PermissionsChecking
import uk.ac.warwick.tabula.permissions.{CheckablePermission, Permissions}
import uk.ac.warwick.tabula.services.{AutowiringTermServiceComponent, AutowiringMonitoringPointServiceComponent, MonitoringPointServiceComponent, ProfileServiceComponent, AutowiringProfileServiceComponent}
import org.hibernate.criterion.Order._
import uk.ac.warwick.tabula.data.model.Module
import uk.ac.warwick.tabula.data.model.Route
import uk.ac.warwick.tabula.JavaImports._
import uk.ac.warwick.tabula.data.model.SitsStatus
import uk.ac.warwick.tabula.data.model.CourseType
import uk.ac.warwick.tabula.data.model.ModeOfAttendance
import uk.ac.warwick.tabula.data.model.Department
import uk.ac.warwick.tabula.system.BindListener
import org.springframework.validation.BindingResult
import org.hibernate.criterion.Order
import uk.ac.warwick.tabula.{CurrentUser, AcademicYear}
import org.joda.time.DateTime
import scala.collection.JavaConverters._
import uk.ac.warwick.tabula.data.model.attendance.MonitoringPoint

case class StudentPointsData(
	student: StudentMember,
	pointsByTerm: Map[String, Map[MonitoringPoint, String]],
	unrecorded: Int,
	missed: Int
)

case class ViewStudentsResults(
	students: Seq[StudentPointsData],
	totalResults: Int
)

object ViewStudentsCommand {
	def apply(department: Department, academicYearOption: Option[AcademicYear], user: CurrentUser) =
		new ViewStudentsCommand(department, academicYearOption, user)
			with ViewStudentsPermissions
			with AutowiringProfileServiceComponent
			with AutowiringSecurityServicePermissionsAwareRoutes
			with AutowiringMonitoringPointServiceComponent
			with AutowiringTermServiceComponent
			with ComposableCommand[ViewStudentsResults]
			with ViewStudentsState
			with ReadOnly with Unaudited
}

abstract class ViewStudentsCommand(val department: Department, val academicYearOption: Option[AcademicYear], val user: CurrentUser)
	extends CommandInternal[ViewStudentsResults] with ViewStudentsState with BindListener  with GroupMonitoringPointsByTerm {
	self: ProfileServiceComponent with MonitoringPointServiceComponent =>

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
			buildData(sortedStudents, filteredUniversityIds.size)
		} else if (sortOrder.asScala.exists(o => o.getPropertyName == "unrecordedMonitoringPoints")) {
			val filteredUniversityIds = profileService.findAllUniversityIdsByRestrictions(department, buildRestrictions())
			val sortedStudents = monitoringPointService.studentsByUnrecordedCount(
				filteredUniversityIds,
				academicYear,
				currentAcademicWeek,
				sortOrder.asScala.filter(o => o.getPropertyName == "unrecordedMonitoringPoints").head.isAscending,
				studentsPerPage,
				studentsPerPage * (page-1)
			)
			buildData(sortedStudents, filteredUniversityIds.size)
		} else {
			val totalResults = profileService.countStudentsByRestrictions(
				department = department,
				restrictions = buildRestrictions()
			)

			val students = profileService.findStudentsByRestrictions(
				department = department,
				restrictions = buildRestrictions(),
				orders = buildOrders(),
				maxResults = studentsPerPage,
				startResult = studentsPerPage * (page-1)
			)

			buildData(students, totalResults)
		}



	}

	private def buildData(students: Seq[StudentMember], totalResults: Int) = {
		val pointSetsByStudent = monitoringPointService.findPointSetsForStudentsByStudent(students, academicYear)
		val allPoints = pointSetsByStudent.flatMap(_._2.points.asScala).toSeq
		val checkpoints = monitoringPointService.getCheckpointsByStudent(allPoints)
		val currentAcademicWeek = termService.getAcademicWeekForAcademicYear(DateTime.now(), academicYear)

		val studentsWithPoints = students.map{ student => {
			pointSetsByStudent.get(student).map{ pointSet =>
				val pointsByTerm = groupByTerm(pointSetsByStudent(student).points.asScala, academicYear)
				val pointsByTermWithCheckpointString = pointsByTerm.map{ case(term, points) =>
					term -> points.map{ point =>
						point -> {
							val checkpointOption = checkpoints.find{
								case (s, checkpoint) => s == student && checkpoint.point == point
							}
							checkpointOption.map{	case (_, checkpoint) => checkpoint.state.dbValue }.getOrElse({
								if (currentAcademicWeek > point.requiredFromWeek)	"late"
								else ""
							})
						}
					}.toMap
				}
				val unrecorded = pointsByTermWithCheckpointString.values.flatMap(_.values).count(_ == "late")
				StudentPointsData(student, pointsByTermWithCheckpointString, unrecorded, student.missedMonitoringPoints)
			}.getOrElse(
				StudentPointsData(student, Map(), 0, 0)
			)
		}}

		ViewStudentsResults(studentsWithPoints, totalResults)
	}

	def onBind(result: BindingResult) {
		// Add all non-withdrawn codes to SPR statuses by default
		if (sprStatuses.isEmpty) {
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

	val thisAcademicYear = AcademicYear.guessByDate(new DateTime())
	val academicYear = academicYearOption.getOrElse(thisAcademicYear)

	var studentsPerPage = FiltersStudents.DefaultStudentsPerPage
	var page = 1

	val defaultOrder = Seq(asc("lastName"), asc("firstName")) // Don't allow this to be changed atm
	var sortOrder: JList[Order] = JArrayList()

	var courseTypes: JList[CourseType] = JArrayList()
	var routes: JList[Route] = JArrayList()
	lazy val visibleRoutes = routesForPermission(user, Permissions.MonitoringPoints.View, department)
	var modesOfAttendance: JList[ModeOfAttendance] = JArrayList()
	var yearsOfStudy: JList[JInteger] = JArrayList()
	var sprStatuses: JList[SitsStatus] = JArrayList()
	var modules: JList[Module] = JArrayList()

	// For Attendance Monitoring, we shouldn't consider sub-departments
	override lazy val allRoutes = department.routes.asScala.sorted(Route.DegreeTypeOrdering)
	lazy val canSeeAllRoutes = visibleRoutes.size == allRoutes.size
}