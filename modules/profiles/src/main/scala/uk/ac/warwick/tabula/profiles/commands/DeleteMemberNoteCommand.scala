package uk.ac.warwick.tabula.profiles.commands

import uk.ac.warwick.tabula.CurrentUser
import uk.ac.warwick.tabula.data.model.{Member, MemberNote}
import uk.ac.warwick.tabula.commands.{SelfValidating, Description, Command}
import uk.ac.warwick.spring.Wire
import uk.ac.warwick.tabula.services.ProfileService
import uk.ac.warwick.tabula.permissions.Permissions
import uk.ac.warwick.tabula.data.Transactions._
import uk.ac.warwick.tabula.data.Daoisms
import org.springframework.validation.{Errors, BindingResult}

class DeleteMemberNoteCommand(val memberNote: MemberNote, val member: Member, val user: CurrentUser) extends Command[MemberNote] with Daoisms with SelfValidating {

	var profileService = Wire[ProfileService]

	PermissionCheck(Permissions.MemberNotes.Delete, memberNote)         // ???

	/**
	Subclasses do their work in here.

		Classes using a command should NOT call this method! call apply().
		The method here is protected but subclasses can easily override it
		to be publicly visible, so there's little to stop you from calling it.
		TODO somehow stop this being callable
		*/
	protected def applyInternal(): MemberNote = transactional() {
			memberNote.deleted = true
			session.saveOrUpdate(memberNote)
			memberNote
	}

	def validate(errors:Errors){
		// should any member note be able to be deleted?
		// should only the creator be able to delete???
	}

	// describe the thing that's happening.
	def describe(d: Description) {
		d.memberNote(memberNote)
	}

}
