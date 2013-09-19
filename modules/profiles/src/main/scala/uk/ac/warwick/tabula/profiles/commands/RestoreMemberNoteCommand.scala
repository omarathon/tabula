package uk.ac.warwick.tabula.profiles.commands

import uk.ac.warwick.tabula.data.model.{Member, MemberNote}
import uk.ac.warwick.tabula.CurrentUser
import uk.ac.warwick.tabula.commands.{Description, SelfValidating, Command}
import uk.ac.warwick.spring.Wire
import uk.ac.warwick.tabula.services.MemberNoteService
import uk.ac.warwick.tabula.permissions.Permissions
import uk.ac.warwick.tabula.data.Transactions._
import org.springframework.validation.Errors

class RestoreMemberNoteCommand (val memberNote: MemberNote, val member: Member, val user: CurrentUser) extends Command[MemberNote] with SelfValidating {

	var memberNoteService = Wire[MemberNoteService]

	PermissionCheck(Permissions.MemberNotes.Delete, memberNote)

	protected def applyInternal(): MemberNote = transactional() {
		memberNote.deleted = false
		memberNoteService.saveOrUpdate(memberNote)
		memberNote
	}
	def validate(errors:Errors)
	{
		if (!memberNote.deleted) errors.reject("profiles.memberNote.restore.notDeleted")

		// not sure we need this
		if (!memberNote.member.universityId.equals(member.universityId)) errors.reject("")
	}

	def describe(d: Description) {
		d.memberNote(memberNote)
	}
}

