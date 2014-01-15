package uk.ac.warwick.tabula.attendance.commands

import uk.ac.warwick.tabula.commands._
import uk.ac.warwick.tabula.data.model.attendance.{AttendanceState, MonitoringCheckpoint, MonitoringPoint}
import uk.ac.warwick.tabula.data.model._
import uk.ac.warwick.tabula.permissions.Permissions
import uk.ac.warwick.tabula.services._
import uk.ac.warwick.tabula.system.permissions.{PermissionsChecking, RequiresPermissionsChecking}
import org.springframework.validation.{BindingResult, Errors}
import uk.ac.warwick.tabula.CurrentUser
import scala.collection.JavaConverters._
import uk.ac.warwick.tabula.JavaImports._
import uk.ac.warwick.tabula.helpers.LazyMaps
import org.hibernate.criterion.Order._
import org.hibernate.criterion.Order
import uk.ac.warwick.tabula.system.BindListener

object SetMonitoringCheckpointCommand {
	def apply(department: Department, templateMonitoringPoint: MonitoringPoint, user: CurrentUser, routes: JList[Route]) =
		new SetMonitoringCheckpointCommand(department, templateMonitoringPoint, user, routes)
			with AutowiringProfileServiceComponent
			with AutowiringSecurityServicePermissionsAwareRoutes
			with SetMonitoringCheckpointCommandPermissions
			with ComposableCommand[Seq[MonitoringCheckpoint]]
			with SetMonitoringCheckpointCommandValidation
			with SetMonitoringPointDescription
			with SetMonitoringCheckpointState
			with AutowiringMonitoringPointServiceComponent
			with AutowiringSecurityServiceComponent
			with AutowiringTermServiceComponent
}

abstract class SetMonitoringCheckpointCommand(val department: Department, val templateMonitoringPoint: MonitoringPoint, val user: CurrentUser, val routes: JList[Route])
	extends CommandInternal[Seq[MonitoringCheckpoint]] with Appliable[Seq[MonitoringCheckpoint]] with SetMonitoringCheckpointState
	with BindListener with PopulateOnForm with PopulateGroupedPoints with TaskBenchmarking {

	self: MonitoringPointServiceComponent with ProfileServiceComponent =>

	def populate() {
		val students = benchmarkTask("Get students matching the filter") {
			profileService.findAllStudentsByRestrictions(
				department = department,
				restrictions = buildRestrictions(),
				orders = buildOrders()
			)
		}
		benchmarkTask("Populate grouped points") {
			populateGroupedPoints(students, templateMonitoringPoint) match {
				case (state, descriptions) =>
					studentsState = state
					checkpointDescriptions = descriptions
			}
		}
	}

	def applyInternal(): Seq[MonitoringCheckpoint] = {
		studentsStateAsScala.flatMap{ case (student, pointMap) =>
			pointMap.flatMap{ case (point, state) =>
				if (state == null) {
					monitoringPointService.deleteCheckpoint(student, point)
					None
				} else {
					Option(monitoringPointService.saveOrUpdateCheckpoint(student, point, state, user))
				}
			}
		}.toSeq
	}

	def onBind(result: BindingResult) = {
		studentsStateAsScala = studentsState.asScala.map{case(student, pointMap) => student -> pointMap.asScala.toMap}.toMap
	}
}

trait SetMonitoringCheckpointCommandValidation extends SelfValidating with GroupedPointValidation {
	self: SetMonitoringCheckpointState with SecurityServiceComponent with TermServiceComponent with MonitoringPointServiceComponent =>

	def validate(errors: Errors) {
		def permissionValidation(student: StudentMember, route: Route) = {
			!securityService.can(user, Permissions.MonitoringPoints.Record, route)
		}
		validateGroupedPoint(errors,templateMonitoringPoint, studentsStateAsScala, permissionValidation)
	}

}

trait SetMonitoringCheckpointCommandPermissions extends RequiresPermissionsChecking with PermissionsChecking {
	self: SetMonitoringCheckpointState =>

	def permissionsCheck(p: PermissionsChecking) {
		if (routesForPermission(user, Permissions.MonitoringPoints.View, department).size == department.routes.asScala.size)
			p.PermissionCheck(Permissions.MonitoringPoints.Record, department)
		else
			p.PermissionCheckAll(Permissions.MonitoringPoints.Record, routes.asScala)
	}
}


trait SetMonitoringPointDescription extends Describable[Seq[MonitoringCheckpoint]] {
	self: SetMonitoringCheckpointState =>

	override lazy val eventName = "SetMonitoringCheckpoint"

	def describe(d: Description) {
		d.property("checkpoints", studentsStateAsScala.map{ case (student, pointMap) =>
			student.universityId -> pointMap.map{ case(point, state) => point -> {
				if (state == null)
					"null"
				else
					state.dbValue
			}}
		})
	}
}


trait SetMonitoringCheckpointState extends FiltersStudents with PermissionsAwareRoutes with GroupMonitoringPointsByTerm with MonitoringPointServiceComponent{
	def templateMonitoringPoint: MonitoringPoint
	def department: Department
	def user: CurrentUser
	def routes: JList[Route]

	var studentsState: JMap[StudentMember, JMap[MonitoringPoint, AttendanceState]] =
		LazyMaps.create{student: StudentMember => JHashMap(): JMap[MonitoringPoint, AttendanceState] }.asJava
	var studentsStateAsScala: Map[StudentMember, Map[MonitoringPoint, AttendanceState]] = _

	var checkpointDescriptions: Map[StudentMember, Map[MonitoringPoint, String]] = _

	var courseTypes: JList[CourseType] = JArrayList()
	var modesOfAttendance: JList[ModeOfAttendance] = JArrayList()
	var yearsOfStudy: JList[JInteger] = JArrayList()
	var sprStatuses: JList[SitsStatus] = JArrayList()
	var modules: JList[Module] = JArrayList()

	// We don't actually allow any sorting, but these need to be defined
	val defaultOrder = Seq(asc("lastName"), asc("firstName")) // Don't allow this to be changed atm
	var sortOrder: JList[Order] = JArrayList()

}
