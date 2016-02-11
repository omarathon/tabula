package uk.ac.warwick.tabula.commands.attendance.agent.old

import org.springframework.validation.{BindingResult, Errors}
import uk.ac.warwick.tabula.CurrentUser
import uk.ac.warwick.tabula.JavaImports._
import uk.ac.warwick.tabula.commands.attendance.old.{GroupMonitoringPointsByTerm, PopulateGroupedPoints}
import uk.ac.warwick.tabula.commands.attendance.view.old.GroupedPointValidation
import uk.ac.warwick.tabula.commands._
import uk.ac.warwick.tabula.data.model.attendance.{AttendanceState, MonitoringCheckpoint, MonitoringPoint}
import uk.ac.warwick.tabula.data.model.{AttendanceNote, Member, StudentMember, StudentRelationshipType}
import uk.ac.warwick.tabula.helpers.LazyMaps
import uk.ac.warwick.tabula.permissions.Permissions
import uk.ac.warwick.tabula.services._
import uk.ac.warwick.tabula.system.BindListener
import uk.ac.warwick.tabula.system.permissions.{PermissionsChecking, PermissionsCheckingMethods, RequiresPermissionsChecking}

import scala.collection.JavaConverters._

object AgentPointRecordCommand {
	def apply(agent: Member, user: CurrentUser, relationshipType: StudentRelationshipType, templateMonitoringPoint: MonitoringPoint) =
		new AgentPointRecordCommand(agent, user, relationshipType, templateMonitoringPoint)
			with AgentPointRecordValidation
			with AutowiringMonitoringPointServiceComponent
			with AutowiringUserLookupComponent
			with AutowiringTermServiceComponent
			with AutowiringRelationshipServiceComponent
			with AgentPointRecordPermissions
			with ComposableCommand[Seq[MonitoringCheckpoint]]
			with AgentPointRecordDescription
}

abstract class AgentPointRecordCommand(
	val agent: Member,
	val user: CurrentUser,
	val relationshipType: StudentRelationshipType,
	val templateMonitoringPoint: MonitoringPoint
) extends CommandInternal[Seq[MonitoringCheckpoint]] with AgentPointRecordCommandState
	with BindListener with PopulateOnForm with TaskBenchmarking with PopulateGroupedPoints {

	this: MonitoringPointServiceComponent with RelationshipServiceComponent with UserLookupComponent =>

	def populate() = {
		val students = benchmarkTask("Get relationships with current user") {
			relationshipService.listStudentRelationshipsWithMember(relationshipType, agent).flatMap(_.studentMember).distinct
		}
		benchmarkTask("Populate grouped points") {
			populateGroupedPoints(students, templateMonitoringPoint) match {
				case (state, descriptions, notes) =>
					studentsState = state
					checkpointDescriptions = descriptions
					attendanceNotes = notes
			}
		}
	}

	def applyInternal() = {
		studentsStateAsScala.flatMap{ case (student, pointMap) =>
			pointMap.flatMap{ case (point, state) =>
				if (state == null) {
					monitoringPointService.deleteCheckpoint(student, point)
					None
				} else {
					Option(monitoringPointService.saveOrUpdateCheckpointByMember(student, point, state, agent))
				}
			}
		}.toSeq
	}

	def onBind(result: BindingResult) = {
		studentsStateAsScala = studentsState.asScala.map{case(student, pointMap) => student -> pointMap.asScala.toMap}.toMap
	}

}

trait AgentPointRecordValidation extends SelfValidating with GroupedPointValidation {
	self: AgentPointRecordCommandState =>

	override def validate(errors: Errors) = {
		validateGroupedPoint(errors,templateMonitoringPoint, studentsStateAsScala)
	}
}

trait AgentPointRecordPermissions extends RequiresPermissionsChecking with PermissionsCheckingMethods {
	this: AgentPointRecordCommandState with RelationshipServiceComponent =>

	def permissionsCheck(p: PermissionsChecking) {
		p.PermissionCheck(Permissions.Profiles.StudentRelationship.Read(mandatory(relationshipType)), agent)
		p.PermissionCheckAll(
			Permissions.MonitoringPoints.Record,
			relationshipService.listStudentRelationshipsWithMember(relationshipType, agent).flatMap(_.studentMember).distinct
		)
	}
}

trait AgentPointRecordDescription extends Describable[Seq[MonitoringCheckpoint]] {
	self: AgentPointRecordCommandState =>

	override lazy val eventName = "AgentPointRecordCheckpoints"

	def describe(d: Description) {
		d.property("checkpoints", studentsStateAsScala.map{ case (student, pointMap) =>
			student.universityId -> pointMap.map{ case(point, state) => point.id -> {
				if (state == null)
					"null"
				else
					state.dbValue
			}}
		})
	}
}

trait AgentPointRecordCommandState extends GroupMonitoringPointsByTerm with MonitoringPointServiceComponent {
	def agent: Member
	def user: CurrentUser
	def relationshipType: StudentRelationshipType
	def templateMonitoringPoint: MonitoringPoint

	var studentsState: JMap[StudentMember, JMap[MonitoringPoint, AttendanceState]] =
		LazyMaps.create{student: StudentMember => JHashMap(): JMap[MonitoringPoint, AttendanceState] }.asJava
	var studentsStateAsScala: Map[StudentMember, Map[MonitoringPoint, AttendanceState]] = _

	var checkpointDescriptions: Map[StudentMember, Map[MonitoringPoint, String]] = _
	var attendanceNotes: Map[StudentMember, Map[MonitoringPoint, AttendanceNote]] = _

}
