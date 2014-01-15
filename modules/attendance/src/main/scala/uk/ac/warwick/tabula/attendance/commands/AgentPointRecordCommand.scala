package uk.ac.warwick.tabula.attendance.commands

import uk.ac.warwick.tabula.data.model.{Route, StudentMember, StudentRelationshipType, Member}
import uk.ac.warwick.tabula.commands._
import uk.ac.warwick.tabula.system.permissions.{PermissionsChecking, RequiresPermissionsChecking}
import uk.ac.warwick.tabula.permissions.Permissions
import uk.ac.warwick.tabula.CurrentUser
import uk.ac.warwick.tabula.data.model.attendance.{AttendanceState, MonitoringCheckpoint, MonitoringPoint}
import uk.ac.warwick.tabula.services._
import scala.collection.JavaConverters._
import uk.ac.warwick.tabula.JavaImports._
import org.springframework.validation.{BindingResult, Errors}
import uk.ac.warwick.tabula.helpers.LazyMaps
import uk.ac.warwick.tabula.system.BindListener

object AgentPointRecordCommand {
	def apply(agent: Member, user: CurrentUser, relationshipType: StudentRelationshipType, templateMonitoringPoint: MonitoringPoint) =
		new AgentPointRecordCommand(agent, user, relationshipType, templateMonitoringPoint)
			with ComposableCommand[Seq[MonitoringCheckpoint]]
			with AgentPointRecordPermissions
			with AgentPointRecordDescription
			with AgentPointRecordValidation
			with AutowiringMonitoringPointServiceComponent
			with AutowiringProfileServiceComponent
			with AutowiringTermServiceComponent
			with AutowiringSecurityServiceComponent
			with AutowiringRelationshipServiceComponent
}

abstract class AgentPointRecordCommand(
	val agent: Member,
	val user: CurrentUser,
	val relationshipType: StudentRelationshipType,
	val templateMonitoringPoint: MonitoringPoint
) extends CommandInternal[Seq[MonitoringCheckpoint]] with Appliable[Seq[MonitoringCheckpoint]] with AgentPointRecordCommandState
	with BindListener with PopulateOnForm with TaskBenchmarking with PopulateGroupedPoints {

	this: MonitoringPointServiceComponent with RelationshipServiceComponent with ProfileServiceComponent =>

	def populate() = {
		val students = benchmarkTask("Get relationships with current user") {
			relationshipService.listStudentRelationshipsWithMember(relationshipType, agent).flatMap(_.studentMember)
		}
		benchmarkTask("Populate grouped points") {
			populateGroupedPoints(students, templateMonitoringPoint) match {
				case (state, descriptions) =>
					studentsState = state
					checkpointDescriptions = descriptions
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
					Option(monitoringPointService.saveOrUpdateCheckpoint(student, point, state, agent))
				}
			}
		}.toSeq
	}

	def onBind(result: BindingResult) = {
		studentsStateAsScala = studentsState.asScala.map{case(student, pointMap) => student -> pointMap.asScala.toMap}.toMap
	}

}

trait AgentPointRecordValidation extends SelfValidating with GroupedPointValidation {
	self: AgentPointRecordCommandState with SecurityServiceComponent =>

	override def validate(errors: Errors) = {
		def permissionValidation(student: StudentMember, route: Route) = {
			!securityService.can(user, Permissions.MonitoringPoints.Record, student)
		}
		validateGroupedPoint(errors,templateMonitoringPoint, studentsStateAsScala, permissionValidation)
	}
}

trait AgentPointRecordPermissions extends RequiresPermissionsChecking with PermissionsChecking {
	this: AgentPointRecordCommandState =>

	def permissionsCheck(p: PermissionsChecking) {
		p.PermissionCheck(Permissions.Profiles.StudentRelationship.Read(p.mandatory(relationshipType)), agent)
	}
}

trait AgentPointRecordDescription extends Describable[Seq[MonitoringCheckpoint]] {
	self: AgentPointRecordCommandState =>

	override lazy val eventName = "AgentPointRecordCheckpoints"

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

trait AgentPointRecordCommandState extends GroupMonitoringPointsByTerm with MonitoringPointServiceComponent {
	def agent: Member
	def user: CurrentUser
	def relationshipType: StudentRelationshipType
	def templateMonitoringPoint: MonitoringPoint

	var studentsState: JMap[StudentMember, JMap[MonitoringPoint, AttendanceState]] =
		LazyMaps.create{student: StudentMember => JHashMap(): JMap[MonitoringPoint, AttendanceState] }.asJava
	var studentsStateAsScala: Map[StudentMember, Map[MonitoringPoint, AttendanceState]] = _

	var checkpointDescriptions: Map[StudentMember, Map[MonitoringPoint, String]] = _

}
