package uk.ac.warwick.tabula.cm2

import uk.ac.warwick.tabula.BrowserTest

class CourseworkAssignmentMembershipTest extends BrowserTest with CourseworkFixtures {

	"Student" should "not be able to submit if not enrolled" in {

		def submissionSettings() = {
			radioButtonGroup("restrictSubmissions").value = "true"
		}

		withAssignment("xxx01", "Fully featured assignment", submissionSettings=submissionSettings) { assignmentId =>
			// Student1 and Student2 are enrolled by default
			as(P.Student3) {
				// Not on the coursework homepage
				linkText("Fully featured assignment").findElement should be (None)

				// Use the assignment ID to mock up a URL
				go to Path(s"/coursework/submission/$assignmentId")

				pageSource contains "You're not enrolled" should be (true)

				// Let's request enrolment
				click on cssSelector("form .btn")

				pageSource contains "Thanks, we've sent a message to a department administrator" should be (true)
				cssSelector(".id7-main-content .btn").webElement.getAttribute("class") contains "disabled" should be (true)
			}
		}
	}

	"Student" should "be able to submit without being enrolled if the assignment accepts it" in {
		def submissionSettings() = {
			radioButtonGroup("restrictSubmissions").value = "false"
		}

		withAssignment("xxx01", "Fully featured assignment", submissionSettings=submissionSettings) { assignmentId =>
			// Student1 is enrolled
			submitAssignment(P.Student1, "Fully featured assignment", assignmentId, "/file1.txt")
			verifyPageLoaded(pageSource contains "Thanks, we've received your submission." should be {true})
			// Student 3 is not enrolled but can submit anyway
			submitAssignment(P.Student3, "Fully featured assignment", assignmentId, "/file2.txt", mustBeEnrolled = false)
			verifyPageLoaded(pageSource contains "Thanks, we've received your submission." should be {true})
		}
	}

	"Student" should "be able to submit when enrolled via SITS" in {

		def submissionSettings() = {
			radioButtonGroup("restrictSubmissions").value = "true"
		}

		def assignmentSettings(members: Seq[String]) = {

			getInputByLabel("Cool essay").get.click()

			eventually { className("link-sits").element.attribute("class").get.contains("disabled") should be (false) }
			click on className("link-sits")

			// there will be a delay between the dialog being dismissed and the source being updated by the
			// ajax response. So wait some more
			eventually { pageSource should include("4 enrolled") }
		}

		withAssignment("xxx01", "Fully featured assignment", studentSettings = assignmentSettings, submissionSettings = submissionSettings, students=Nil) { assignmentId =>
			// Student 3 is enrolled via SITS and can submit
			submitAssignment(P.Student3, "Fully featured assignment", assignmentId, "/file1.txt")
			verifyPageLoaded(pageSource contains "Thanks, we've received your submission." should be {true})

			// Student 5 isn't in the SITS group. Poor Student5
			as(P.Student5) {
				// Use the assignment ID to mock up a URL
				go to Path("/coursework/submission/" + assignmentId + "/")

				pageSource contains "You're not enrolled" should be (true)
			}
		}
	}

}
