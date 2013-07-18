package uk.ac.warwick.tabula.coursework.commands.assignments


import extensions.DeleteExtensionCommand
import uk.ac.warwick.tabula.{AppContextTestBase, Mockito, TestBase}
import uk.ac.warwick.tabula.data.model.forms.Extension
import org.joda.time.DateTime
import org.springframework.transaction.annotation.Transactional


class RevokeExtensionTest extends AppContextTestBase with Mockito {

	@Transactional @Test
	def revokeExtension(){
		withUser("cuslaj") {
			val assignment = newDeepAssignment()
			assignment.id = "assignment"
			val extension = new Extension()
			extension.universityId = "0000001"
			extension.assignment = assignment
			extension.expiryDate =  new DateTime()
			extension.reason = "I feel like it"
			assignment.extensions add extension
			assignment.extensions.size should be (1)

			val command = new DeleteExtensionCommand(assignment.module, assignment, "0000001" ,currentUser)
			command.apply()

			assignment.extensions.size should be (0)
		}
	}

}
