
import scala.collection.JavaConversions._
import org.joda.time.DateTime
import uk.ac.warwick.tabula.coursework.commands.assignments.extensions.EditExtensionCommand
import uk.ac.warwick.tabula.data.model.forms.{ExtensionState, Extension}
import uk.ac.warwick.tabula.data.model.Assignment
import uk.ac.warwick.tabula.events.EventHandling
import uk.ac.warwick.tabula.services.UserLookupService
import uk.ac.warwick.tabula.{RequestInfo, Mockito, TestBase}

// scalastyle:off magic.number
class ModifyExtensionCommandTest extends TestBase with Mockito {

	EventHandling.enabled = false

	@Test
	def addExtension() {
		withUser("cuslat", "1171795") {
			withFakeTime(dateTime(2014, 2, 11)) {

				val assignment = createAssignment()
				val targetUniversityId = "1234567"
				val extension = createExtension(assignment, targetUniversityId)

				extension.approved should be (false)
				extension.rejected should be (false)
				extension.state should be (ExtensionState.Unreviewed)
				extension.reviewerComments should be (null)
				extension.universityId should be (targetUniversityId)
			}
		}
	}

	@Test
	def approveExtension() {
		withUser("cuslat", "1171795") {
			withFakeTime(dateTime(2014, 2, 11)) {

				val currentUser = RequestInfo.fromThread.get.user
				val assignment = createAssignment()
				val targetUniversityId = "1234567"
				val extension = createExtension(assignment, targetUniversityId)

				extension.approved should be (false)
				extension.rejected should be (false)
				extension.state should be (ExtensionState.Unreviewed)
				extension.reviewerComments should be (null)
				extension.universityId should be (targetUniversityId)

				val reviewerComments = "I've always thought that Tabula should have a photo sharing component"

				val editCommand = EditExtensionCommand(assignment.module, assignment, targetUniversityId, currentUser, "approve")
	 			editCommand.userLookup = mock[UserLookupService]
				editCommand.copyTo(createExtension(assignment, targetUniversityId, reviewerComments))

				extension.approved should be (true)
				extension.rejected should be (false)
				extension.state should be (ExtensionState.Approved)
				extension.reviewedOn should be (DateTime.now)
				extension.reviewerComments should be (reviewerComments)
				extension.universityId should be (targetUniversityId)
			}
		}
	}

	@Test
	def rejectExtension() {
		withUser("cuslat", "1171795") {
			withFakeTime(dateTime(2014, 2, 11)) {

				val currentUser = RequestInfo.fromThread.get.user
				val assignment = createAssignment()
				val targetUniversityId = "1234567"
				val extension = createExtension(assignment, targetUniversityId)

				extension.approved should be (false)
				extension.rejected should be (false)
				extension.state should be (ExtensionState.Unreviewed)
				extension.reviewerComments should be (null)
				extension.universityId should be (targetUniversityId)

				val reviewerComments = "something something messaging service something something $17 billion cheers thanks"

				val editCommand = EditExtensionCommand(assignment.module, assignment, targetUniversityId, currentUser, "reject")
				editCommand.userLookup = mock[UserLookupService]
				editCommand.copyTo(createExtension(assignment, targetUniversityId, reviewerComments))

				extension.approved should be (false)
				extension.rejected should be (true)
				extension.state should be (ExtensionState.Rejected)
				extension.reviewedOn should be (DateTime.now)
				extension.reviewerComments should be (reviewerComments)
				extension.universityId should be (targetUniversityId)
			}
		}
	}


	def createAssignment(): Assignment = {
		val assignment = newDeepAssignment()
		assignment.closeDate = DateTime.now.plusMonths(1)
		assignment.extensions += new Extension(currentUser.universityId)
		assignment
	}

	def createExtension(assignment: Assignment, targetUniversityId: String, reviewerComments: String = "something something cats") : Extension = {
		val extension = new Extension(targetUniversityId)
		extension.assignment = assignment
		extension.expiryDate = DateTime.now.plusMonths(2)
		extension.reviewerComments = reviewerComments
		extension
	}
}