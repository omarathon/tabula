package uk.ac.warwick.tabula.coursework

import uk.ac.warwick.tabula.BrowserTest
import org.openqa.selenium.By
import org.joda.time.DateTime

class CourseworkAssignmentManagementTest extends BrowserTest with CourseworkFixtures {

	"Department admin" should "be able to set up some assignments" in {
		withAssignment("xxx101", "Fully featured assignment") { id =>
		// withAssignment() leads to the dept admin page while logged in as an admin, so we don't need to do any more login

		// Check that the assignment is there
			id should not be ('empty)

			// Check that an empty assignment looks right
			click on getAssignmentInfo("xxx101", "Fully featured assignment").findElement(By.partialLinkText("0 submissions"))

			pageSource contains "Fully featured assignment (XXX101)" should be (true)

			// Go back to the admin page
			click on linkText("Test Services")

			// Ensure the edit properties page has been created successfully
		{
			val info = getAssignmentInfo("xxx101", "Fully featured assignment")
			click on (info.findElement(By.partialLinkText("Actions")))
			val editAssignment = info.findElement(By.partialLinkText("Edit properties"))
			eventually {
				editAssignment.isDisplayed should be (true)
			}
			click on (editAssignment)

			textField("name").value should be ("Fully featured assignment")

			// Delete link is there
			linkText("delete").webElement.isDisplayed should be (true)

			click on linkText("Cancel")
		}
		}
	}

	"Department admin" should "be able to disallow extensions with warning for pending requests" in {
		withAssignment("xxx102", "Assignment with extension requests") { id =>
			// student1 is enrolled by default
			requestExtension(P.Student1, "xxx102", "Assignment with extension requests", id, new DateTime().plusMonths(1), true)
			// student3 is not, but should be able to ask for an extension anyway
			requestExtension(P.Student3, "xxx102", "Assignment with extension requests", id, new DateTime().plusMonths(3), false)
			// create an *approved* extension for student4 (using FixturesDriver rather than UI)
			createExtension(P.Student4.usercode, id, true)

			// check page looks correct (with 2 outstanding extensions)
			???

			val info = getAssignmentInfo("xxx102", "Assignment with extension requests")
			click on (info.findElement(By.partialLinkText("Actions")))
			val editAssignment = info.findElement(By.partialLinkText("Edit properties"))
			eventually {
				editAssignment.isDisplayed should be (true)
			}
			click on (editAssignment)

			textField("name").value should be ("Assignment with extension requests")

			checkbox("allowExtensions").select()

			click on linkText("Save")

			// check modal has correct count (2) unapproved extensions
			???

			// click cancel doesn't do anything (can we check this?)
			???

			// re-click save, and confirm
			???

			// check page looks correct (with no outstanding extensions)
			???
		}
	}

	"Department admin" should "be able to archive an assignment" in {
		withAssignment("xxx101", "Fully featured assignment for archiving") { assignmentId =>
			val info = getAssignmentInfo("xxx101", "Fully featured assignment for archiving")

			click on (info.findElement(By.partialLinkText("Actions")))
			val archiveAssignment = info.findElement(By.partialLinkText("Archive assignment"))
			eventually {
				archiveAssignment.isDisplayed should be (true)
			}

			click on (archiveAssignment)

			// Wait for our Ajax popup to load
			eventuallyAjax {
				id("command").webElement.findElement(By.className("btn")).isDisplayed() should be (true)
			}

			click on (id("command").webElement.findElement(By.className("btn")))

			// This works, but it doesn't reload the page automatically properly. Do it manually
			eventuallyAjax {
				find(className("ajax-response")) map { _.underlying.isDisplayed() } should be (Some(true))
			}

			reloadPage

			// Wait for the page reload...
			eventually {
				linkText("Show").findElement should be ('defined)
				click on linkText("Show") // Modules with no non-archived assignments are hidden

				val minfo = getModuleInfo("xxx101")
				click on (minfo.findElement(By.partialLinkText("Manage")))

				eventually {
					minfo.findElement(By.partialLinkText("Show archived assignments")).isDisplayed should be (true)
				}

				click on (minfo.findElement(By.partialLinkText("Show archived assignments")))
			}

			getAssignmentInfo("xxx101", "Fully featured assignment for archiving (Archived)").isDisplayed() should be (true)
		}
	}

}