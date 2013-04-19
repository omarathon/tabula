package uk.ac.warwick.tabula.coursework.commands.assignments


import extensions.DeleteExtensionCommand
import uk.ac.warwick.tabula.{AppContextTestBase, Mockito, TestBase}
import uk.ac.warwick.tabula.data.model.forms.Extension
import org.joda.time.DateTime



class RevokeExtensionTest extends AppContextTestBase with Mockito {

	@Test
	def revokeExtension = transactional { tx =>
		withUser("cuslaj") {
			val assignment = newDeepAssignment()
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
