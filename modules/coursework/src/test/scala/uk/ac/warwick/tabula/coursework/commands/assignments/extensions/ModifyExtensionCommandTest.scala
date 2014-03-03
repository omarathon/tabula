
import scala.collection.JavaConversions._
import org.joda.time.DateTime
import uk.ac.warwick.tabula.coursework.commands.assignments.extensions.{EditExtensionCommand, ExtensionItem, AddExtensionCommand}
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
				val extensions = addAnExtension(assignment, targetUniversityId)

				extensions.head.approved should be (false)
				extensions.head.rejected should be (false)
				extensions.head.state should be (ExtensionState.Unreviewed)
				extensions.head.reviewerComments should be (null)
				extensions.head.universityId should be (targetUniversityId)
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
				var extensions = addAnExtension(assignment, targetUniversityId)

				extensions.head.approved should be (false)
				extensions.head.rejected should be (false)
				extensions.head.state should be (ExtensionState.Unreviewed)
				extensions.head.reviewerComments should be (null)
				extensions.head.universityId should be (targetUniversityId)

				val reviewerComments = "I've always thought that Tabula should have a photo sharing component"

				val editCommand = new EditExtensionCommand(assignment.module, assignment, targetUniversityId, Some(extensions.head), currentUser, "approve")
	 			editCommand.userLookup = mock[UserLookupService]
				editCommand.extensionItems = List(makeExtensionItem(targetUniversityId, reviewerComments))
				extensions = editCommand.copyExtensionItems()

				extensions.head.approved should be (true)
				extensions.head.rejected should be (false)
				extensions.head.state should be (ExtensionState.Approved)
				extensions.head.reviewedOn should be (DateTime.now)
				extensions.head.reviewerComments should be (reviewerComments)
				extensions.head.universityId should be (targetUniversityId)
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
				var extensions = addAnExtension(assignment, targetUniversityId)

				extensions.head.approved should be (false)
				extensions.head.rejected should be (false)
				extensions.head.state should be (ExtensionState.Unreviewed)
				extensions.head.reviewerComments should be (null)
				extensions.head.universityId should be (targetUniversityId)

				val reviewerComments = "something something messaging service something something $17 billion cheers thanks"

				val editCommand = new EditExtensionCommand(assignment.module, assignment, targetUniversityId, Some(extensions.head), currentUser, "reject")
				editCommand.userLookup = mock[UserLookupService]
				editCommand.extensionItems = List(makeExtensionItem(targetUniversityId, reviewerComments))
				extensions = editCommand.copyExtensionItems()

				extensions.head.approved should be (false)
				extensions.head.rejected should be (true)
				extensions.head.state should be (ExtensionState.Rejected)
				extensions.head.reviewedOn should be (DateTime.now)
				extensions.head.reviewerComments should be (reviewerComments)
				extensions.head.universityId should be (targetUniversityId)
			}
		}
	}


	def createAssignment(): Assignment = {
		val assignment = newDeepAssignment()
		assignment.closeDate = DateTime.now.plusMonths(1)
		assignment.extensions += new Extension(currentUser.universityId)
		assignment
	}

	def addAnExtension(assignment: Assignment, targetUniversityId: String) : Seq[Extension] = {
		val addCommand = new AddExtensionCommand(assignment.module, assignment, currentUser, "")
		addCommand.userLookup = mock[UserLookupService]
		addCommand.extensionItems = List(makeExtensionItem(targetUniversityId))
		addCommand.copyExtensionItems()
	}

	def makeExtensionItem(targetUniversityId: String, reviewerComments: String = "something something cats"): ExtensionItem = {
		val extensionItem = new ExtensionItem
		extensionItem.universityId = targetUniversityId
		extensionItem.expiryDate = DateTime.now.plusMonths(2)
		extensionItem.reviewerComments = reviewerComments
		extensionItem
	}

}