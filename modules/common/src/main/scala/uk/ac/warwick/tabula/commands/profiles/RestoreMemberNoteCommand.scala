package uk.ac.warwick.tabula.commands.profiles

import org.springframework.validation.Errors
import uk.ac.warwick.spring.Wire
import uk.ac.warwick.tabula.CurrentUser
import uk.ac.warwick.tabula.commands.{Command, Description, SelfValidating}
import uk.ac.warwick.tabula.data.Transactions._
import uk.ac.warwick.tabula.data.model.{Member, MemberNote}
import uk.ac.warwick.tabula.permissions.Permissions
import uk.ac.warwick.tabula.services.MemberNoteService

class RestoreMemberNoteCommand (val memberNote: MemberNote, val member: Member, val user: CurrentUser) extends Command[MemberNote] with SelfValidating {

	mustBeLinked(memberNote, member)

	var memberNoteService = Wire[MemberNoteService]

	PermissionCheck(Permissions.MemberNotes.Delete, memberNote)

	protected def applyInternal(): MemberNote = transactional() {
		memberNote.deleted = false
		memberNoteService.saveOrUpdate(memberNote)
		memberNote
	}
	def validate(errors:Errors) {
		if (!memberNote.deleted) errors.reject("profiles.memberNote.restore.notDeleted")
	}

	def describe(d: Description) {
		d.memberNote(memberNote)
	}
}
