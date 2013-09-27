package uk.ac.warwick.tabula.profiles.commands

import uk.ac.warwick.tabula.CurrentUser
import uk.ac.warwick.tabula.data.model.{Member, MemberNote}
import uk.ac.warwick.tabula.commands.{SelfValidating, Description, Command}
import uk.ac.warwick.spring.Wire
import uk.ac.warwick.tabula.services.MemberNoteService
import uk.ac.warwick.tabula.permissions.Permissions
import uk.ac.warwick.tabula.data.Transactions._
import org.springframework.validation.Errors

class DeleteMemberNoteCommand(val memberNote: MemberNote, val member: Member, val user: CurrentUser) extends Command[MemberNote] with SelfValidating {

	mustBeLinked(memberNote, member)

	var memberNoteService = Wire[MemberNoteService]

	PermissionCheck(Permissions.MemberNotes.Delete, memberNote)

	protected def applyInternal(): MemberNote = transactional() {
			memberNote.deleted = true
			memberNoteService.saveOrUpdate(memberNote)
			memberNote
	}
	def validate(errors:Errors) {
		if (memberNote.deleted) errors.reject("profiles.memberNote.delete.notDeleted")
	}

	// describe the thing that's happening.
	def describe(d: Description) {
		d.memberNote(memberNote)
	}

}
