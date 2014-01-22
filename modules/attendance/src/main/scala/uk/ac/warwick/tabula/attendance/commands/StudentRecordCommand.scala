package uk.ac.warwick.tabula.attendance.commands

import uk.ac.warwick.tabula.data.model.{AttendanceNote, Department, StudentMember}
import uk.ac.warwick.tabula.commands._
import uk.ac.warwick.tabula.system.permissions.{PermissionsChecking, RequiresPermissionsChecking}
import uk.ac.warwick.tabula.permissions.Permissions
import uk.ac.warwick.tabula.{CurrentUser, ItemNotFoundException, AcademicYear}
import uk.ac.warwick.tabula.data.model.attendance._
import uk.ac.warwick.tabula.services.{ProfileServiceComponent, AutowiringProfileServiceComponent, AutowiringMonitoringPointServiceComponent, MonitoringPointServiceComponent, AutowiringTermServiceComponent}
import scala.collection.JavaConverters._
import uk.ac.warwick.tabula.JavaImports._
import org.springframework.validation.{BindingResult, Errors}
import org.joda.time.DateTime
import uk.ac.warwick.tabula.system.BindListener

object StudentRecordCommand {
	def apply(department: Department, student: StudentMember, user: CurrentUser, academicYearOption: Option[AcademicYear]) =
		new StudentRecordCommand(department, student, user, academicYearOption)
			with ComposableCommand[Seq[MonitoringCheckpoint]]
			with StudentRecordPermissions
			with StudentRecordDescription
			with StudentRecordValidation
			with GroupMonitoringPointsByTerm
			with AutowiringMonitoringPointServiceComponent
			with AutowiringTermServiceComponent
			with AutowiringProfileServiceComponent
}

abstract class StudentRecordCommand(
	val department: Department,
	val student: StudentMember,
	val user: CurrentUser,
	val academicYearOption: Option[AcademicYear]
) extends CommandInternal[Seq[MonitoringCheckpoint]] with StudentRecordCommandState with PopulateOnForm with BindListener with CheckpointUpdatedDescription {
	this: MonitoringPointServiceComponent with ProfileServiceComponent =>

	def populate() = {
		val checkpoints = monitoringPointService.getCheckpoints(Seq(student), pointSet)(student)
		checkpointMap = checkpoints.map{ case(point, checkpointOption) =>
			point -> checkpointOption.map(c => c.state).getOrElse(null)
		}.asJava
	}

	def onBind(result: BindingResult) = {
		val checkpoints = monitoringPointService.getCheckpoints(Seq(student), pointSet)(student)
		checkpointDescriptions = checkpoints.map{case (point, checkpointOption) =>
			point -> checkpointOption.map{c => describeCheckpoint(c)}.getOrElse("")
		}
		attendanceNotes = monitoringPointService.findAttendanceNotes(Seq(student), pointSet.points.asScala)
			.map{ note =>	note.point -> note }.toMap
	}

	def applyInternal() = {
		checkpointMap.asScala.flatMap{case(point, state) =>
			if (state == null) {
				monitoringPointService.deleteCheckpoint(student, point)
				None
			} else {
				Option(monitoringPointService.saveOrUpdateCheckpoint(student, point, state, user))
			}
		}.toSeq
	}

}

trait StudentRecordValidation extends SelfValidating {
	self: StudentRecordCommandState =>

	override def validate(errors: Errors) = {
		val currentAcademicWeek = termService.getAcademicWeekForAcademicYear(DateTime.now(), pointSet.academicYear)
		val points = pointSet.points.asScala
		checkpointMap.asScala.foreach{case (point, state) =>
			errors.pushNestedPath(s"checkpointMap[${point.id}]")
			if (!points.contains(point)) {
				errors.rejectValue("", "monitoringPointSet.invalidPoint")
			}

			if (!nonReportedTerms.contains(termService.getTermFromAcademicWeek(point.validFromWeek, pointSet.academicYear).getTermTypeAsString)){
				errors.rejectValue("", "monitoringCheckpoint.student.alreadyReportedThisTerm")
			}

			if (thisAcademicYear.startYear <= pointSet.academicYear.startYear
				&& currentAcademicWeek < point.validFromWeek
				&& !(state == null || state == AttendanceState.MissedAuthorised)
			) {
				errors.rejectValue("", "monitoringCheckpoint.beforeValidFromWeek")
			}
			errors.popNestedPath()
		}
	}

}

trait StudentRecordPermissions extends RequiresPermissionsChecking with PermissionsChecking {
	this: StudentRecordCommandState =>

	def permissionsCheck(p: PermissionsChecking) {
		p.PermissionCheck(Permissions.MonitoringPoints.Record, student)
	}
}

trait StudentRecordDescription extends Describable[Seq[MonitoringCheckpoint]] {
	self: StudentRecordCommandState =>

	override lazy val eventName = "StudentRecordCheckpoints"

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

trait StudentRecordCommandState extends GroupMonitoringPointsByTerm with MonitoringPointServiceComponent {
	def department: Department
	def student: StudentMember
	def user: CurrentUser
	def academicYearOption: Option[AcademicYear]
	val thisAcademicYear = AcademicYear.guessByDate(new DateTime())
	val academicYear = academicYearOption.getOrElse(thisAcademicYear)
	lazy val pointSet = monitoringPointService.getPointSetForStudent(student, academicYear).getOrElse(throw new ItemNotFoundException)

	var checkpointMap: JMap[MonitoringPoint, AttendanceState] =  JHashMap()
	var checkpointDescriptions: Map[MonitoringPoint, String] = _
	var attendanceNotes: Map[MonitoringPoint, AttendanceNote] = _

	def monitoringPointsByTerm = groupByTerm(pointSet.points.asScala, pointSet.academicYear)
	def nonReportedTerms = monitoringPointService.findNonReportedTerms(Seq(student), pointSet.academicYear)
}
