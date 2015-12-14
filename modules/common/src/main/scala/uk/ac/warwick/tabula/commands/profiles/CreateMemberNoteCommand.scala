package uk.ac.warwick.tabula.commands.profiles

import org.springframework.validation.Errors
import uk.ac.warwick.tabula.CurrentUser
import uk.ac.warwick.tabula.commands.Description
import uk.ac.warwick.tabula.data.model.{Member, MemberNote}
import uk.ac.warwick.tabula.permissions.Permissions

class CreateMemberNoteCommand(member: Member, submitter: CurrentUser) extends ModifyMemberNoteCommand(member, submitter) {

	PermissionCheck(Permissions.MemberNotes.Create, member)

	val memberNote = new MemberNote

	def describe(d: Description) = d.member(member)

	def contextSpecificValidation(errors: Errors) {}
}
