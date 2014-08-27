package uk.ac.warwick.tabula.attendance.commands.view.old

import org.joda.time.DateTime
import uk.ac.warwick.tabula.AcademicYear
import uk.ac.warwick.tabula.commands.{CommandInternal, ComposableCommand, ReadOnly, TaskBenchmarking, Unaudited}
import uk.ac.warwick.tabula.data.model.attendance.AttendanceState
import uk.ac.warwick.tabula.data.model.{Department, Member, StudentMember, StudentRelationshipType}
import uk.ac.warwick.tabula.permissions.Permissions
import uk.ac.warwick.tabula.services.{AutowiringMonitoringPointServiceComponent, AutowiringRelationshipServiceComponent, AutowiringTermServiceComponent, MonitoringPointServiceComponent, RelationshipServiceComponent, TermServiceComponent}
import uk.ac.warwick.tabula.system.permissions.{PermissionsChecking, PermissionsCheckingMethods, RequiresPermissionsChecking}

import scala.collection.JavaConverters._

case class ViewAgentsResult(
	agent: String,
	agentMember: Option[Member],
	students: Seq[StudentMember],
	unrecorded: Int,
	missed: Int
)

object ViewAgentsCommand {
	def apply(department: Department, relationshipType: StudentRelationshipType, academicYear: Option[AcademicYear]) =
		new ViewAgentsCommand(department, relationshipType, academicYear)
			with ComposableCommand[Seq[ViewAgentsResult]]
			with ViewAgentsState
			with ViewAgentsPermissions
			with AutowiringRelationshipServiceComponent
			with AutowiringMonitoringPointServiceComponent
			with AutowiringTermServiceComponent
			with ReadOnly with Unaudited
}

class ViewAgentsCommand(val department: Department, val relationshipType: StudentRelationshipType, val academicYearOption: Option[AcademicYear])
	extends CommandInternal[Seq[ViewAgentsResult]] with ViewAgentsState with TaskBenchmarking {

	self: RelationshipServiceComponent with MonitoringPointServiceComponent with TermServiceComponent =>

	def applyInternal() = {
		val relationships = benchmarkTask("Get all relationships in department") {
			relationshipService.listStudentRelationshipsByDepartment(relationshipType, department)
		}
		val students = relationships.flatMap(_.studentMember)
		val pointSetsByStudent = benchmarkTask("Get point sets for students in relationships") {
			monitoringPointService.findPointSetsForStudentsByStudent(students, academicYear)
		}
		val allPoints = benchmarkTask("Collect all points") { pointSetsByStudent.flatMap(_._2.points.asScala).toSeq }
		val checkpoints = benchmarkTask("Get all checkpoints for all points") { monitoringPointService.getCheckpointsByStudent(allPoints) }
		val currentAcademicWeek = benchmarkTask("Get current academic week") { termService.getAcademicWeekForAcademicYear(DateTime.now(), academicYear) }

		benchmarkTask("Group points by agent to get results") {
			relationships.groupBy(_.agent).map { case(agent, agentRelationships) =>
				val counts = agentRelationships.flatMap(_.studentMember).map{ student =>
					pointSetsByStudent.get(student).map{ pointSet =>
						val studentCheckpoints = pointSet.points.asScala.map{ point => {
							val checkpointOption = checkpoints.find{
								case (s, checkpoint) => s == student && checkpoint.point == point
							}
							checkpointOption.map{	case (_, checkpoint) => checkpoint.state.dbValue }.getOrElse({
								if (currentAcademicWeek > point.requiredFromWeek)	"late"
								else ""
							})
						}}
						val unrecorded = studentCheckpoints.count(_ == "late")
						val missed = studentCheckpoints.count(_ == AttendanceState.MissedUnauthorised.dbValue)
						(unrecorded, missed)
					}.getOrElse((0,0))
				}
				val unrecorded = counts.map(_._1).sum
				val missed = counts.map(_._2).sum
				ViewAgentsResult(agent, agentRelationships.head.agentMember, agentRelationships.flatMap(_.studentMember), unrecorded, missed)
			}.toSeq
		}
	}
}

trait ViewAgentsPermissions extends RequiresPermissionsChecking with PermissionsCheckingMethods {
	self: ViewAgentsState  =>

	def permissionsCheck(p: PermissionsChecking) {
		p.PermissionCheck(Permissions.MonitoringPoints.View, department)
	}
}

trait ViewAgentsState {
	def department: Department
	def relationshipType: StudentRelationshipType
	def academicYearOption: Option[AcademicYear]

	val thisAcademicYear = AcademicYear.guessSITSAcademicYearByDate(new DateTime())
	val academicYear = academicYearOption.getOrElse(thisAcademicYear)
}