package uk.ac.warwick.tabula.attendance.commands

import uk.ac.warwick.tabula.data.model.{StudentMember, StudentRelationshipType, Member}
import uk.ac.warwick.tabula.commands._
import uk.ac.warwick.tabula.system.permissions.{PermissionsChecking, RequiresPermissionsChecking}
import uk.ac.warwick.tabula.permissions.Permissions
import uk.ac.warwick.tabula.AcademicYear
import uk.ac.warwick.tabula.data.model.attendance.{MonitoringCheckpointState, MonitoringCheckpoint, MonitoringPointSet, MonitoringPoint}
import uk.ac.warwick.tabula.services.{TermServiceComponent, AutowiringMonitoringPointServiceComponent, MonitoringPointServiceComponent, AutowiringTermServiceComponent}
import scala.collection.JavaConverters._
import uk.ac.warwick.tabula.JavaImports._
import org.springframework.validation.Errors
import org.joda.time.DateTime

object AgentStudentRecordCommand {
	def apply(agent: Member, relationshipType: StudentRelationshipType,
		student: StudentMember, pointSet: MonitoringPointSet, academicYearOption: Option[AcademicYear]
	) =	new AgentStudentRecordCommand(agent, relationshipType, student, pointSet, academicYearOption)
		with ComposableCommand[Seq[MonitoringCheckpoint]]
		with AgentStudentRecordPermissions
		with AgentStudentRecordDescription
		with AgentStudentRecordValidation
		with AutowiringTermServiceComponent
		with AutowiringMonitoringPointServiceComponent
		with GroupMonitoringPointsByTerm
}

abstract class AgentStudentRecordCommand(val agent: Member, val relationshipType: StudentRelationshipType,
	val student: StudentMember, val pointSet: MonitoringPointSet, val academicYearOption: Option[AcademicYear]
) extends CommandInternal[Seq[MonitoringCheckpoint]] with Appliable[Seq[MonitoringCheckpoint]] with AgentStudentRecordCommandState {

	this: TermServiceComponent with MonitoringPointServiceComponent with GroupMonitoringPointsByTerm =>

	def populate() = {
		checkpointMap = monitoringPointService.getChecked(Seq(student), pointSet)(student).map{ case(point, stateOption) =>
			point -> stateOption.getOrElse(null)
		}.asJava
	}

	def applyInternal() = {
		checkpointMap.asScala.flatMap{case(point, state) =>
			if (state == null) {
				monitoringPointService.deleteCheckpoint(student, point)
				None
			} else {
				Option(monitoringPointService.saveOrUpdateCheckpoint(student, point, state, agent))
			}
		}.toSeq
	}

}

trait AgentStudentRecordValidation extends SelfValidating {
	self: AgentStudentRecordCommandState =>

	override def validate(errors: Errors) = {
		val currentAcademicWeek = termService.getAcademicWeekForAcademicYear(DateTime.now(), pointSet.academicYear)
		val points = pointSet.points.asScala
		checkpointMap.asScala.foreach{case (point, state) => {
			errors.pushNestedPath(s"checkpointMap[${point.id}]")
			if (!points.contains(point)) {
				errors.rejectValue("", "monitoringPointSet.invalidPoint")
			}
			if (point.sentToAcademicOffice) {
				errors.rejectValue("", "monitoringCheckpoint.sentToAcademicOffice")
			}
			if (currentAcademicWeek < point.validFromWeek && !(state == null || state == MonitoringCheckpointState.MissedAuthorised)) {
				errors.rejectValue("", "monitoringCheckpoint.beforeValidFromWeek")
			}
			errors.popNestedPath()
		}}
	}

}

trait AgentStudentRecordPermissions extends RequiresPermissionsChecking with PermissionsChecking {
	this: AgentStudentRecordCommandState =>

	def permissionsCheck(p: PermissionsChecking) {
		p.PermissionCheck(Permissions.MonitoringPoints.Record, student)
	}
}

trait AgentStudentRecordDescription extends Describable[Seq[MonitoringCheckpoint]] {
	self: AgentStudentRecordCommandState =>

	override lazy val eventName = "AgentStudentRecordCheckpoints"

	def describe(d: Description) {
		d.monitoringPointSet(pointSet)
		d.studentIds(Seq(student.universityId))
		d.property("checkpoints", checkpointMap.asScala.map{ case (point, state) =>
			if (state == null)
				point.id -> "null"
			else
				point.id -> state.dbValue
		})
	}
}

trait AgentStudentRecordCommandState extends GroupMonitoringPointsByTerm{
	def agent: Member
	def relationshipType: StudentRelationshipType
	def student: StudentMember
	def pointSet: MonitoringPointSet
	def academicYearOption: Option[AcademicYear]

	var checkpointMap: JMap[MonitoringPoint, MonitoringCheckpointState] =  JHashMap()

	def monitoringPointsByTerm = groupByTerm(pointSet.points.asScala, pointSet.academicYear)
}
