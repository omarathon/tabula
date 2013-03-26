package uk.ac.warwick.tabula.coursework

import scala.collection.JavaConverters._
import org.scalatest.BeforeAndAfter
import uk.ac.warwick.tabula.BrowserTest
import org.openqa.selenium.By

class CourseworkAssignmentSubmissionTest extends BrowserTest with CourseworkFixtures {
	
	// TAB-413, TAB-415
	"Student" should "be able to submit assignment after validation errors without re-uploading file" in {
		withAssignment("xxx101", "Fully featured assignment") { assignmentId =>
			as(P.Student1) {
				click on linkText("Fully featured assignment")		
				currentUrl should endWith(assignmentId + "/")
				
				click on (getInputByLabel("File").orNull)
				pressKeys(getClass.getResource("/file1.txt").getFile)
				
				new TextField(getInputByLabel("Word count").orNull).value = "1000"
					
				// Don't click the plagiarism detection button yet
				submit()
				
				pageSource contains "Thanks, we've received your submission." should be (false)
				
				id("plagiarismDeclaration.errors").webElement.isDisplayed() should be (true)
				pageSource contains "You must confirm that this submission is all your own work." should be (true)
				
				// Click the button and submit again
				checkbox("plagiarismDeclaration").select()
		
				submit()
				
				pageSource contains "Thanks, we've received your submission." should be (true)
				
				linkText("file1.txt").webElement.isDisplayed() should be (true)
			}
		}
	}
	
	"Student" should "be able to submit assignment" in {
		withAssignment("xxx101", "Fully featured assignment") { assignmentId =>
			submitAssignment(P.Student1, "xxx101", "Fully featured assignment", assignmentId, "/file1.txt")
		}
	}
	
	"Student" should "not be able to submit if not enrolled" in {
		withAssignment("xxx101", "Fully featured assignment") { assignmentId =>
			// Student1 and Student2 are enrolled by default
			as(P.Student3) {
				// Not on the coursework homepage
				linkText("Fully featured assignment").findElement should be (None)
				
				// Use the assignment ID to mock up a URL
				go to Path("/coursework/module/xxx101/" + assignmentId + "/")
				
				pageSource contains ("You're not enrolled") should be (true)
				
				// Let's request enrolment
				click on className("btn")
				
				pageSource contains "Thanks, we've sent a message to a department administrator" should be (true)
				className("btn").webElement.getAttribute("class") contains "disabled" should be (true)
			}
		}
	}
	
	"Student" should "be able to submit without being enrolled if the assignment accepts it" in {
		def assignmentSettings(members: Seq[String]) = {
			allFeatures(members)
			
			radioButtonGroup("restrictSubmissions").value = "false"
		}
		
		withAssignment("xxx101", "Fully featured assignment", assignmentSettings) { assignmentId =>
			// Student1 is enrolled
			submitAssignment(P.Student1, "xxx101", "Fully featured assignment", assignmentId, "/file1.txt", true)
			
			// Student 3 is not enrolled but can submit anyway
			submitAssignment(P.Student3, "xxx101", "Fully featured assignment", assignmentId, "/file2.txt", false)
		}
	}

}