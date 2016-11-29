package uk.ac.warwick.tabula.commands.attendance.note

import uk.ac.warwick.tabula.commands.CommandInternal
import uk.ac.warwick.tabula.commands.ComposableCommand
import uk.ac.warwick.tabula.commands.Describable
import uk.ac.warwick.tabula.commands.Description
import uk.ac.warwick.tabula.commands.PopulateOnForm
import uk.ac.warwick.tabula.commands.SelfValidating
import uk.ac.warwick.tabula.commands.UploadedFile
import uk.ac.warwick.tabula.data.model.{AbsenceType, FileAttachment, StudentMember}
import uk.ac.warwick.tabula.data.model.attendance.{AttendanceMonitoringCheckpoint, AttendanceMonitoringNote, AttendanceMonitoringPoint}
import uk.ac.warwick.tabula.services.AutowiringFileAttachmentServiceComponent
import uk.ac.warwick.tabula.services.AutowiringUserLookupComponent
import uk.ac.warwick.tabula.services.FileAttachmentServiceComponent
import uk.ac.warwick.tabula.services.UserLookupComponent
import uk.ac.warwick.tabula.services.attendancemonitoring.{AttendanceMonitoringServiceComponent, AutowiringAttendanceMonitoringServiceComponent}
import uk.ac.warwick.tabula.system.permissions.{PermissionsChecking, PermissionsCheckingMethods, RequiresPermissionsChecking}
import uk.ac.warwick.tabula.permissions.Permissions
import uk.ac.warwick.tabula.CurrentUser
import org.joda.time.DateTime
import org.springframework.validation.{BindingResult, Errors}
import uk.ac.warwick.tabula.system.BindListener
import uk.ac.warwick.tabula.JavaImports._

import scala.collection.JavaConverters._
import scala.collection.mutable


object BulkAttendanceNoteCommand {
	def apply(point: AttendanceMonitoringPoint, user: CurrentUser) =
		new BulkAttendanceNoteCommand(point, user)
			with ComposableCommand[Seq[AttendanceMonitoringNote]]
			with BulkAttendanceNotePermissions
			with BulkAttendanceNoteDescription
			with BulkAttendanceNoteValidation
			with BulkAttendanceNoteCommandState
			with AutowiringAttendanceMonitoringServiceComponent
			with AutowiringFileAttachmentServiceComponent
			with AutowiringUserLookupComponent
}

abstract class BulkAttendanceNoteCommand (
	val point: AttendanceMonitoringPoint,
	val user: CurrentUser
) extends CommandInternal[Seq[AttendanceMonitoringNote]] with PopulateOnForm with BindListener
	with BulkAttendanceNoteCommandState {

	self: AttendanceMonitoringServiceComponent with FileAttachmentServiceComponent with UserLookupComponent =>

	def populate(): Unit = {
		val firstNote = students.asScala.flatMap(attendanceMonitoringService.getAttendanceNote(_, point)).headOption
		firstNote.foreach(n => {
			note = n.note
			attachedFile = n.attachment
			absenceType = n.absenceType
		})
		students.asScala.headOption.foreach(student = _)
		isNew = firstNote.isEmpty
	}

	def onBind(result: BindingResult) {
		file.onBind(result)
	}

	def applyInternal(): mutable.Buffer[AttendanceMonitoringNote] = {

		val notes = students.asScala.flatMap(student => {

			val existingNote = attendanceMonitoringService.getAttendanceNote(student, point)

			if(overwrite || existingNote.isEmpty) {
				val attendanceNote = existingNote.getOrElse({
					val newNote = new AttendanceMonitoringNote
					newNote.student = student
					newNote.point = point
					newNote
				})

				attendanceNote.note = note
				if (attendanceNote.attachment != null && attachedFile == null) {
					fileAttachmentService.deleteAttachments(Seq(attendanceNote.attachment))
					attendanceNote.attachment = null
				}

				if (file.hasAttachments) {
					attendanceNote.attachment = file.attached.iterator.next.duplicate()
					attendanceNote.attachment.temporary = false
				}
				attendanceNote.absenceType = absenceType
				attendanceNote.updatedBy = user.apparentId
				attendanceNote.updatedDate = DateTime.now
				attendanceMonitoringService.saveOrUpdate(attendanceNote)
				Some(attendanceNote)
			} else {
				existingNote
			}
		})
		notes
	}
}

trait BulkAttendanceNoteValidation extends SelfValidating {
	self: BulkAttendanceNoteCommandState =>

	override def validate(errors: Errors): Unit = {
		if (absenceType == null) {
			errors.rejectValue("absenceType", "attendanceNote.absenceType.empty")
		}
	}
}

trait BulkAttendanceNoteDescription extends Describable[Seq[AttendanceMonitoringNote]] {
	self: BulkAttendanceNoteCommandState =>

	override lazy val eventName = "UpdateBulkAttendanceNote"

	override def describe(d: Description) {
		d.studentIds(students.asScala.map(_.universityId))
		d.attendanceMonitoringPoints(Seq(point))
	}

	override def describeResult(d: Description, result: Seq[AttendanceMonitoringNote]) {
		d.property("note", result.head.escapedNote)
	}
}

trait BulkAttendanceNotePermissions extends RequiresPermissionsChecking with PermissionsCheckingMethods {

	self: BulkAttendanceNoteCommandState =>

	def permissionsCheck(p: PermissionsChecking) {
		p.PermissionCheck(Permissions.MonitoringPoints.Record, point.scheme)
	}
}

trait BulkAttendanceNoteCommandState {

	def point: AttendanceMonitoringPoint
	var students: JList[StudentMember] = new JArrayList()
	var student: StudentMember = _ // used for attachment url
	var overwrite: Boolean = _

	var note: String = _
	var file: UploadedFile = new UploadedFile
	var attachedFile: FileAttachment = _
	var absenceType: AbsenceType = _

	var isNew: Boolean = false

	var checkpoint: AttendanceMonitoringCheckpoint = _
}
