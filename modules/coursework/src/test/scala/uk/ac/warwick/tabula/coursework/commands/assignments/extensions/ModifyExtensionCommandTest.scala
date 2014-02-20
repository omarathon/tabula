
import scala.collection.JavaConversions._
import org.joda.time.DateTime
import uk.ac.warwick.tabula.coursework.commands.assignments.extensions.{EditExtensionCommand, ExtensionItem, AddExtensionCommand}
import uk.ac.warwick.tabula.data.model.forms.{ExtensionState, Extension}
import uk.ac.warwick.tabula.data.model.{Assignment, Module}
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
				val extensions = addAnExtension(assignment: Assignment)

				extensions.head.approved should be (false)
				extensions.head.rejected should be (false)
				extensions.head.state should be (ExtensionState.Unreviewed)
				extensions.head.reviewerComments should be ("something something cats")
			}
		}
	}

	@Test
	def approveExtension() {
		withUser("cuslat", "1171795") {
			withFakeTime(dateTime(2014, 2, 11)) {

				val currentUser = RequestInfo.fromThread.get.user
				val assignment = createAssignment()
				var extensions = addAnExtension(assignment)

				extensions.head.approved should be (false)
				extensions.head.rejected should be (false)
				extensions.head.state should be (ExtensionState.Unreviewed)
				extensions.head.reviewerComments should be ("something something cats")

				val reviewerComments = "I've always thought that Tabula should have a photo sharing component"

				val editCommand = new EditExtensionCommand(assignment.module, assignment, extensions.head, currentUser, "approve")
	 			editCommand.userLookup = mock[UserLookupService]
				editCommand.extensionItems = List(makeExtensionItem(reviewerComments))
				extensions = editCommand.copyExtensionItems()

				extensions.head.approved should be (true)
				extensions.head.rejected should be (false)
				extensions.head.state should be (ExtensionState.Approved)
				extensions.head.reviewedOn should be (DateTime.now)
				extensions.head.reviewerComments should be (reviewerComments)
			}
		}
	}

	@Test
	def rejectExtension() {
		withUser("cuslat", "1171795") {
			withFakeTime(dateTime(2014, 2, 11)) {

				val currentUser = RequestInfo.fromThread.get.user
				val assignment = createAssignment()
				var extensions = addAnExtension(assignment)

				extensions.head.approved should be (false)
				extensions.head.rejected should be (false)
				extensions.head.state should be (ExtensionState.Unreviewed)

				val reviewerComments = "something something messaging service something something $17 billion cheers thanks"

				val editCommand = new EditExtensionCommand(assignment.module, assignment, extensions.head, currentUser, "reject")
				editCommand.userLookup = mock[UserLookupService]
				editCommand.extensionItems = List(makeExtensionItem(reviewerComments))
				extensions = editCommand.copyExtensionItems()

				extensions.head.approved should be (false)
				extensions.head.rejected should be (true)
				extensions.head.state should be (ExtensionState.Rejected)
				extensions.head.reviewedOn should be (DateTime.now)
				extensions.head.reviewerComments should be (reviewerComments)
			}
		}
	}


	def createAssignment(): Assignment = {
		val assignment = newDeepAssignment()
		assignment.closeDate = DateTime.now.plusMonths(1)
		assignment.extensions += new Extension(currentUser.universityId)
		assignment
	}

	def addAnExtension(assignment: Assignment) : Seq[Extension] = {
		val addCommand = new AddExtensionCommand(assignment.module, assignment, currentUser, "")
		addCommand.userLookup = mock[UserLookupService]
		addCommand.extensionItems = List(makeExtensionItem())
		addCommand.copyExtensionItems()
	}


	def makeExtensionItem(reviewerComments: String = "something something cats"): ExtensionItem = {
		val extensionItem = new ExtensionItem
		extensionItem.universityId = currentUser.universityId
		extensionItem.expiryDate = DateTime.now.plusMonths(2)
		extensionItem.reviewerComments = reviewerComments
		extensionItem
	}

}