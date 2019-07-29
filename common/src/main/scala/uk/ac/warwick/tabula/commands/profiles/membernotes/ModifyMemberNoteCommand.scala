package uk.ac.warwick.tabula.commands.profiles.membernotes

import org.joda.time.{DateTime, LocalDate}
import org.springframework.validation.BindingResult
import uk.ac.warwick.tabula.JavaImports._
import uk.ac.warwick.tabula.commands.{CommandInternal, UploadedFile}
import uk.ac.warwick.tabula.data.HibernateHelpers
import uk.ac.warwick.tabula.data.model.{AbstractMemberNote, FileAttachment, MemberNote}
import uk.ac.warwick.tabula.services.{FileAttachmentServiceComponent, MemberNoteServiceComponent}
import uk.ac.warwick.tabula.system.BindListener

import scala.collection.JavaConverters._
import scala.collection.mutable

abstract class ModifyMemberNoteCommandInternal extends CommandInternal[AbstractMemberNote] {

  self: ModifyMemberNoteCommandRequest with ModifyAbstractMemberNoteCommandState
    with FileAttachmentServiceComponent with MemberNoteServiceComponent =>

  override def applyInternal(): AbstractMemberNote = {
    copyTo(abstractMemberNote)

    if (abstractMemberNote.attachments != null) {
      val filesToKeep = Option(attachedFiles).map(_.asScala.toList).getOrElse(List())
      val filesToRemove: mutable.Buffer[FileAttachment] = abstractMemberNote.attachments.asScala -- filesToKeep
      abstractMemberNote.attachments = JArrayList[FileAttachment](filesToKeep)
      fileAttachmentService.deleteAttachments(filesToRemove)
    }

    if (!file.attached.isEmpty) {
      for (attachment <- file.attached.asScala) {
        abstractMemberNote.addAttachment(attachment)
      }
    }

    HibernateHelpers.initialiseAndUnproxy(abstractMemberNote) match {
      case memberNote: MemberNote => memberNoteService.saveOrUpdate(memberNote)
    }

    abstractMemberNote
  }

}

trait ModifyMemberNoteCommandBindListener extends BindListener {

  self: ModifyMemberNoteCommandRequest =>

  override def onBind(result: BindingResult) {
    result.pushNestedPath("file")
    file.onBind(result)
    result.popNestedPath()
  }
}

trait ModifyAbstractMemberNoteCommandState {
  def abstractMemberNote: AbstractMemberNote

  val attachmentTypes: Seq[String] = Seq[String]()
}

trait ModifyMemberNoteCommandState extends ModifyAbstractMemberNoteCommandState {
  def memberNote: MemberNote

  override def abstractMemberNote: AbstractMemberNote = memberNote
}

trait ModifyMemberNoteCommandRequest {
  var title: String = _
  var note: String = _
  var file: UploadedFile = new UploadedFile
  var attachedFiles: JList[FileAttachment] = JArrayList()

  def copyTo(memberNote: AbstractMemberNote) {
    memberNote.note = note
    memberNote.title = title
    memberNote.lastUpdatedDate = DateTime.now
  }
}
