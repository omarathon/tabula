package uk.ac.warwick.tabula.profiles.commands

import uk.ac.warwick.tabula.{Fixtures, TestBase, Mockito}
import org.springframework.validation.BindException

class CreateMemberNoteCommandTest extends TestBase with Mockito {

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
