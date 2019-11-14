package uk.ac.warwick.tabula.commands.profiles.membernotes

import org.springframework.validation.Errors
import uk.ac.warwick.tabula.commands._
import uk.ac.warwick.tabula.data.HibernateHelpers
import uk.ac.warwick.tabula.data.model.{AbstractMemberNote, Member, MemberNote}
import uk.ac.warwick.tabula.services.{AutowiringMemberNoteServiceComponent, MemberNoteServiceComponent}

object PurgeMemberNoteCommand {
  def apply(memberNote: MemberNote, member: Member) =
    new PurgeAbstractMemberNoteCommandInternal(memberNote, member)
      with AutowiringMemberNoteServiceComponent
      with ComposableCommand[AbstractMemberNote]
      with PurgeMemberNoteValidation
      with DeleteMemberNotePermissions
      with DeleteMemberNoteCommandState
      with Describable[AbstractMemberNote] {

      override lazy val eventName = "PurgeMemberNote"

      override def describe(d: Description): Unit = {
        d.memberNote(memberNote)
      }
    }
}

class PurgeAbstractMemberNoteCommandInternal(val abstractMemberNote: AbstractMemberNote, val member: Member)
  extends CommandInternal[AbstractMemberNote] {

  self: MemberNoteServiceComponent =>

  override def applyInternal(): AbstractMemberNote = {
    HibernateHelpers.initialiseAndUnproxy(abstractMemberNote) match {
      case memberNote: MemberNote => memberNoteService.delete(memberNote)
    }
    abstractMemberNote
  }

}

trait PurgeMemberNoteValidation extends SelfValidating {

  self: DeleteMemberNoteCommandState =>

  override def validate(errors: Errors): Unit = {
    if (!abstractMemberNote.deleted) {
      errors.reject("profiles.memberNote.delete.notDeleted")
    }
  }

}
