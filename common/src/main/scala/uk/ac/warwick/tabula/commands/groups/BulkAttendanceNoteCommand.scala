package uk.ac.warwick.tabula.commands.groups

import org.joda.time.DateTime
import org.springframework.validation.{BindingResult, Errors}
import uk.ac.warwick.tabula.CurrentUser
import uk.ac.warwick.tabula.commands._
import uk.ac.warwick.tabula.data.model.groups.{SmallGroupEventAttendanceNote, SmallGroupEventOccurrence}
import uk.ac.warwick.tabula.data.model.{AbsenceType, FileAttachment, Member, StudentMember}
import uk.ac.warwick.tabula.permissions.Permissions
import uk.ac.warwick.tabula.services._
import uk.ac.warwick.tabula.system.BindListener
import uk.ac.warwick.tabula.system.permissions.{PermissionsChecking, PermissionsCheckingMethods, RequiresPermissionsChecking}

import scala.jdk.CollectionConverters._

object BulkAttendanceNoteCommand {
  type Command = Appliable[Seq[SmallGroupEventAttendanceNote]] with PopulateOnForm

  def apply(occurrence: SmallGroupEventOccurrence, user: CurrentUser): Command =
    new BulkAttendanceNoteCommand(occurrence, user)
      with ComposableCommand[Seq[SmallGroupEventAttendanceNote]]
      with BulkAttendanceNoteRequest
      with BulkAttendanceNoteRequestPopulateOnForm
      with BulkAttendanceNotePermissions
      with BulkAttendanceNoteDescription
      with BulkAttendanceNoteValidation
      with AutowiringSmallGroupServiceComponent
      with AutowiringFileAttachmentServiceComponent
      with AutowiringProfileServiceComponent
}

abstract class BulkAttendanceNoteCommand(val occurrence: SmallGroupEventOccurrence, val user: CurrentUser)
  extends CommandInternal[Seq[SmallGroupEventAttendanceNote]] with BulkAttendanceNoteCommandState {
  self: BulkAttendanceNoteRequest
    with SmallGroupServiceComponent
    with FileAttachmentServiceComponent
    with ProfileServiceComponent =>

  override def applyInternal(): Seq[SmallGroupEventAttendanceNote] = members.flatMap { student =>
    val existingNote = smallGroupService.getAttendanceNote(student.universityId, occurrence)

    if (overwrite || existingNote.isEmpty) {
      val attendanceNote = existingNote.getOrElse({
        val newNote = new SmallGroupEventAttendanceNote
        newNote.student = student
        newNote.occurrence = occurrence
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
      smallGroupService.saveOrUpdate(attendanceNote)
      Some(attendanceNote)
    } else {
      existingNote
    }
  }
}

trait BulkAttendanceNoteValidation extends SelfValidating {
  self: BulkAttendanceNoteRequest =>

  override def validate(errors: Errors): Unit = {
    if (absenceType == null) {
      errors.rejectValue("absenceType", "attendanceNote.absenceType.empty")
    }
  }
}

trait BulkAttendanceNoteDescription extends Describable[Seq[SmallGroupEventAttendanceNote]] {
  self: BulkAttendanceNoteCommandState =>

  override lazy val eventName = "UpdateBulkAttendanceNote"

  override def describe(d: Description): Unit =
    d.studentIds(members.map(_.universityId))
     .smallGroupEventOccurrence(occurrence)

  override def describeResult(d: Description, result: Seq[SmallGroupEventAttendanceNote]): Unit =
    result.headOption.map(d.attendanceMonitoringNote).getOrElse(d)
}

trait BulkAttendanceNotePermissions extends RequiresPermissionsChecking with PermissionsCheckingMethods {
  self: BulkAttendanceNoteCommandState =>

  def permissionsCheck(p: PermissionsChecking): Unit = {
    p.PermissionCheck(Permissions.SmallGroupEvents.Register, mandatory(occurrence))
  }
}

trait BulkAttendanceNoteCommandState {
  self: ProfileServiceComponent =>

  def occurrence: SmallGroupEventOccurrence

  lazy val members: Seq[Member] =
    (occurrence.event.group.students.users.flatMap { user =>
      profileService.getMemberByUniversityId(user.getWarwickId)
    } ++ occurrence.attendance.asScala.toSeq.flatMap { a =>
      profileService.getMemberByUniversityId(a.universityId)
    }).toSeq.distinct.sortBy { m => (m.lastName, m.firstName, m.universityId) }
}

trait BulkAttendanceNoteRequest extends BindListener {
  override def onBind(result: BindingResult): Unit = {
    result.pushNestedPath("file")
    file.onBind(result)
    result.popNestedPath()
  }

  var student: StudentMember = _ // used for attachment url
  var overwrite: Boolean = _

  var note: String = _
  var file: UploadedFile = new UploadedFile
  var attachedFile: FileAttachment = _
  var absenceType: AbsenceType = _

  var isNew: Boolean = false
}

trait BulkAttendanceNoteRequestPopulateOnForm extends PopulateOnForm {
  self: BulkAttendanceNoteRequest
    with BulkAttendanceNoteCommandState
    with SmallGroupServiceComponent =>

  override def populate(): Unit = {
    val firstNote = members.flatMap(mou => smallGroupService.getAttendanceNote(mou.universityId, occurrence)).headOption
    firstNote.foreach { n =>
      note = n.note
      attachedFile = n.attachment
      absenceType = n.absenceType
    }

    members.collectFirst { case s: StudentMember => s }.foreach(student = _)
    isNew = firstNote.isEmpty
  }
}
