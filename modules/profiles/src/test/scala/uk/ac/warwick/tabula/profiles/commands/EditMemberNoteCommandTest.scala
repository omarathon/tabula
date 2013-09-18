package uk.ac.warwick.tabula.profiles.commands

import uk.ac.warwick.tabula.{TestBase, Mockito}
import uk.ac.warwick.tabula.data.model.{StaffMember, MemberNote}
import org.springframework.validation.BindException

class EditMemberNoteCommandTest extends TestBase with Mockito {

	@Test
	def validExistingNote = withUser("cuscao") {
		val member = new StaffMember
		member.lastName = "O'Toole"
		val note = new MemberNote
		note.member = member
		note.note = "Existing, non-edited note"
		val cmd = new EditMemberNoteCommand(note, currentUser)
		val errors = new BindException(cmd, "command")
		cmd.validate(errors)
		errors.hasFieldErrors should be (false)

	}

	@Test
	def invalidEditedNote = withUser("cuscao") {
		val member = new StaffMember
		member.lastName = "O'Toole"
		val note = new MemberNote
		note.member = member
		note.note = "Valid existing, non-edited note"
		val cmd = new EditMemberNoteCommand(note, currentUser)
		val errors = new BindException(cmd, "command")
		cmd.validate(errors)
		errors.hasFieldErrors should be (false)

		cmd.note = " "
		cmd.validate(errors)
		errors.hasFieldErrors should be (true)
		errors.getErrorCount should be (1)
		errors.getFieldError.getField should be ("note")
		errors.getFieldError.getCode should be ("profiles.memberNote.empty")

	}

}
