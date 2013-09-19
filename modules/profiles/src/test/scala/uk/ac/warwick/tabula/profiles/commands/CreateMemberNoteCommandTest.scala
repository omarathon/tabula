package uk.ac.warwick.tabula.profiles.commands

import uk.ac.warwick.tabula.{Fixtures, TestBase, Mockito}
import org.springframework.validation.BindException
import uk.ac.warwick.tabula.services.{MemberNoteService, ProfileService}

class CreateMemberNoteCommandTest extends TestBase with Mockito {

	@Test
	def testApply = withUser("cuscao") {
		val member = Fixtures.student(universityId = "12345")
		val submitter = Fixtures.staff(currentUser.universityId, currentUser.userId)
		val cmd = new CreateMemberNoteCommand(member, currentUser)

		val profileService = mock[ProfileService]
		profileService.getMemberByUniversityId(currentUser.universityId) returns Some(submitter)
		cmd.profileService = profileService
		cmd.memberNoteService = mock[MemberNoteService]

		cmd.note = "the note"

		val memberNote = cmd.applyInternal
		memberNote.member should be (member)
		memberNote.creator should be (submitter)
		memberNote.note should be (cmd.note)

	}

	@Test
	def validMemberNote = withUser("cuscao") {

		val member = Fixtures.student(universityId = "12345")
		val cmd = new CreateMemberNoteCommand(member, currentUser)

		val errors = new BindException(cmd, "command")

		cmd.note = "   a note"
		cmd.validate(errors)
		errors.hasErrors should be(false)
	}

	@Test
	def invalidMemberNote = withUser("cuscao") {

		val member = Fixtures.student(universityId = "12345")
		val cmd = new CreateMemberNoteCommand(member, currentUser)

		val errors = new BindException(cmd, "command")
		cmd.validate(errors)
		errors.hasErrors should be (true)
		errors.getErrorCount should be (1)
		errors.getFieldError.getField should be ("note")
		errors.getFieldError.getCode should be ("profiles.memberNote.empty")

		cmd.note = " "
		cmd.validate(errors)
		errors.hasErrors should be (true)
		errors.getErrorCount should be (2)
	}

}
