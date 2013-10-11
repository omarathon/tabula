package uk.ac.warwick.tabula.profiles.commands

import uk.ac.warwick.tabula.{Fixtures, TestBase, Mockito}
import uk.ac.warwick.tabula.data.model.{StaffMember, MemberNote}
import org.springframework.validation.BindException
import uk.ac.warwick.tabula.services.{UserLookupService, FileAttachmentService, MemberNoteService, ProfileService}

class EditMemberNoteCommandTest extends TestBase with Mockito {

	private trait Fixture {
		val userLookup  = mock[UserLookupService]
		val member = new StaffMember
		member.lastName = "O'Toole"
		val note = new MemberNote
		note.member = member
		note.note = "Existing note"
		note.creatorId = "cuscao"
		note.userLookup = userLookup

		val cmd = new EditMemberNoteCommand(note, currentUser)
		cmd.userLookup = userLookup
}

	@Test
	def testApply = withUser("cuscao") {
		val member = Fixtures.student(universityId = "12345")
		val note = Fixtures.memberNoteWithId("some notes", member, "123")
		val submitter = Fixtures.staff(currentUser.universityId, currentUser.userId)
		val cmd = new EditMemberNoteCommand(note, currentUser)

		val profileService = mock[ProfileService]
		profileService.getMemberByUniversityId(currentUser.universityId) returns Some(submitter)

		cmd.profileService = profileService
		cmd.memberNoteService = mock[MemberNoteService]
		cmd.fileAttachmentService = mock[FileAttachmentService]
		cmd.note = "the note"

		val memberNote = cmd.applyInternal
		memberNote.member should be (member)
		memberNote.note should be (cmd.note)
	}

	@Test
	def validExistingNote { new Fixture {

		val errors = new BindException(cmd, "command")
		cmd.showForm()
		cmd.validate(errors)
		errors.hasFieldErrors should be (false)

	}}

	@Test
	def invalidEditedNote { new Fixture{

		val errors = new BindException(cmd, "command")
		cmd.showForm()
		cmd.validate(errors)
		errors.hasFieldErrors should be (false)

		cmd.showForm()
		cmd.note = " "
		cmd.validate(errors)
		errors.hasFieldErrors should be (true)
		errors.getErrorCount should be (1)
		errors.getFieldError.getField should be ("note")
		errors.getFieldError.getCode should be ("profiles.memberNote.empty")

	}}

	@Test
	def editDeletedNote { new Fixture{

		note.deleted = true

		val errors = new BindException(cmd, "command")

		cmd.showForm()
		cmd.validate(errors)
		errors.hasFieldErrors should be (true)
		errors.getErrorCount should be (1)
		errors.getFieldError.getField should be ("note")
		errors.getFieldError.getCode should be ("profiles.memberNote.edit.deleted")
	}}

}
