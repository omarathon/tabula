package uk.ac.warwick.tabula.commands.scheduling

import org.joda.time.DateTime
import uk.ac.warwick.tabula.commands._
import uk.ac.warwick.tabula.commands.attendance.manage.GetsPointsToCreate
import uk.ac.warwick.tabula.data.model.StudentMember
import uk.ac.warwick.tabula.data.model.attendance._
import uk.ac.warwick.tabula.permissions.Permissions
import uk.ac.warwick.tabula.services.attendancemonitoring.{AttendanceMonitoringServiceComponent, AutowiringAttendanceMonitoringServiceComponent}
import uk.ac.warwick.tabula.services.{AutowiringMonitoringPointServiceComponent, AutowiringTermServiceComponent, MonitoringPointServiceComponent, TermServiceComponent}
import uk.ac.warwick.tabula.system.permissions.{PermissionsChecking, PermissionsCheckingMethods, RequiresPermissionsChecking}

import scala.collection.JavaConverters._

object MonitoringPointMigrationCommand {
	def apply(set: MonitoringPointSet) =
		new MonitoringPointMigrationCommandInternal(set)
			with AutowiringTermServiceComponent
			with AutowiringMonitoringPointServiceComponent
			with AutowiringAttendanceMonitoringServiceComponent
			with ComposableCommand[AttendanceMonitoringScheme]
			with MonitoringPointMigrationDescription
			with MonitoringPointMigrationPermissions
			with MonitoringPointMigrationCommandState
}


class MonitoringPointMigrationCommandInternal(val set: MonitoringPointSet)
	extends CommandInternal[AttendanceMonitoringScheme] with GetsPointsToCreate {

	self: TermServiceComponent with MonitoringPointServiceComponent with AttendanceMonitoringServiceComponent =>

	override def applyInternal() = {
		val scheme = createScheme

		val oldAttendanceNotes: Map[MonitoringPoint, Seq[MonitoringPointAttendanceNote]] = monitoringPointService.findAttendanceNotes(set.points.asScala).groupBy(_.point)
		val oldAndNewPoints = set.points.asScala.map(oldPoint => {
			(oldPoint, createPoint(oldPoint, scheme))
		})

		val allStudents = oldAndNewPoints.flatMap { case (oldPoint, _) => oldPoint.checkpoints.asScala.map(_.student) }.distinct
		val newCheckpoints = oldAndNewPoints.flatMap { case (oldPoint, newPoint) => oldPoint.checkpoints.asScala.map(c => createCheckpoint(c, newPoint)) }
		// Group membership has very flaky in the old system, so we're going to add any student with a checkpoint to this scheme
		// That might mean that they only have checkpoints for some of the points
		// So we don't suddenly start notifying about missing checkpoints from 2+ years ago,
		// create 'missed authorised' checkpoints for any points that don't have legit checkpoints
		val autoCheckpoints = oldAndNewPoints.flatMap { case (oldPoint, newPoint) =>
			val existingStudents = oldPoint.checkpoints.asScala.map(_.student)
			allStudents.diff(existingStudents).map(s => createAutoCheckpoint(s, newPoint))
		}

		val newPoints = oldAndNewPoints.map { case (_, newPoint) => newPoint }
		scheme.points.addAll(newPoints.asJava)

		val newNotes = oldAndNewPoints.flatMap { case (oldPoint, newPoint) =>
			oldAttendanceNotes.get(oldPoint).map(_.map(oldNote => createNote(oldNote, newPoint))).getOrElse(Seq())
		}

		scheme.members.includedUserIds = allStudents.map(_.universityId)

		attendanceMonitoringService.saveOrUpdate(scheme)
		newPoints.foreach(attendanceMonitoringService.saveOrUpdate)
		newNotes.foreach(attendanceMonitoringService.saveOrUpdate)
		(newCheckpoints ++ autoCheckpoints).foreach(attendanceMonitoringService.saveOrUpdateDangerously)

		set.migratedTo = scheme
		monitoringPointService.saveOrUpdate(set)

		allStudents.foreach(student =>
			attendanceMonitoringService.updateCheckpointTotal(student, scheme.department, scheme.academicYear)
		)

		scheme
	}

	private def createScheme: AttendanceMonitoringScheme = {
		val scheme = new AttendanceMonitoringScheme
		scheme.academicYear = set.academicYear
		scheme.department = set.route.adminDepartment
		scheme.name = set.shortDisplayName
		scheme.pointStyle = AttendanceMonitoringPointStyle.Week
		scheme.createdDate = set.createdDate
		scheme.updatedDate = set.updatedDate

		scheme
	}

	private def createPoint(oldPoint: MonitoringPoint, scheme: AttendanceMonitoringScheme): AttendanceMonitoringPoint = {
		val point = new AttendanceMonitoringPoint
		point.scheme = scheme
		copyFromOldPoint(oldPoint, point, scheme.academicYear)
		point.createdDate = oldPoint.createdDate
		point.updatedDate = oldPoint.updatedDate

		point
	}

	private def createCheckpointTotal(student: StudentMember): AttendanceMonitoringCheckpointTotal = {
		attendanceMonitoringService.getCheckpointTotal(student, Option(set.route.adminDepartment), set.academicYear)
	}

	private def createNote(oldNote: MonitoringPointAttendanceNote, newPoint: AttendanceMonitoringPoint): AttendanceMonitoringNote = {
		val note = new AttendanceMonitoringNote
		note.student = oldNote.student
		note.updatedDate = oldNote.updatedDate
		note.updatedBy = oldNote.updatedBy
		note.note = oldNote.note
		note.attachment = oldNote.attachment
		note.absenceType = oldNote.absenceType
		note.point = newPoint

		note
	}

	private def createCheckpoint(oldCheckpoint: MonitoringCheckpoint, newPoint: AttendanceMonitoringPoint): AttendanceMonitoringCheckpoint = {
		val checkpoint = new AttendanceMonitoringCheckpoint
		checkpoint.student = oldCheckpoint.student
		checkpoint.point = newPoint
		checkpoint.setStateDangerously(oldCheckpoint.state)
		checkpoint.updatedBy = oldCheckpoint.updatedBy
		checkpoint.updatedDate = oldCheckpoint.updatedDate
		checkpoint.autoCreated = oldCheckpoint.autoCreated

		checkpoint
	}

	private def createAutoCheckpoint(student: StudentMember, newPoint: AttendanceMonitoringPoint): AttendanceMonitoringCheckpoint = {
		val checkpoint = new AttendanceMonitoringCheckpoint
		checkpoint.student = student
		checkpoint.point = newPoint
		checkpoint.setStateDangerously(AttendanceState.MissedAuthorised)
		checkpoint.updatedBy = "tabula"
		checkpoint.updatedDate = DateTime.now
		checkpoint.autoCreated = true

		checkpoint
	}

}

trait MonitoringPointMigrationPermissions extends RequiresPermissionsChecking with PermissionsCheckingMethods {

	self: MonitoringPointMigrationCommandState =>

	override def permissionsCheck(p: PermissionsChecking) {
		p.PermissionCheck(Permissions.ImportSystemData)
	}

}

trait MonitoringPointMigrationDescription extends Describable[AttendanceMonitoringScheme] {

	self: MonitoringPointMigrationCommandState =>

	override lazy val eventName = "MonitoringPointMigration"

	override def describe(d: Description) {
		d.property("monitoringPointSet", set.id)
	}

	override def describeResult(d: Description, result: AttendanceMonitoringScheme) {
		d.property("attendanceMonitoringScheme", result.id)
	}
}

trait MonitoringPointMigrationCommandState {
	def set: MonitoringPointSet
}