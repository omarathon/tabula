package uk.ac.warwick.tabula.coursework

import org.scalatest.GivenWhenThen
import uk.ac.warwick.tabula.{Download, BrowserTest}
import org.joda.time.DateTime

class CourseworkOnlineFeedbackTest extends BrowserTest with CourseworkFixtures with GivenWhenThen {
	final val moduleCode = "XXX101"
	final val assignmentName = "Online marking for Dummies"

	private def setup(assignmentId: String) {
		submitAssignment(P.Student1, moduleCode, assignmentName, assignmentId, "/file1.txt")
		submitAssignment(P.Student2, moduleCode, assignmentName, assignmentId, "/file2.txt")
	}

	"Online marker" should "be able to see a sequence of submissions to mark" in {
		withAssignment(moduleCode = moduleCode, assignmentName = assignmentName, assistants = Seq(P.Marker1.usercode)) {
			Given("We have two students with submitted assignments")
			assignmentId => setup(assignmentId)

				as(P.Marker1) {
					When("I go to the marking page")
					go to Path("/coursework/admin/module/" + moduleCode.toLowerCase + "/assignments/" + assignmentId + "/feedback/online")

					Then("I see the table of students")
					pageSource contains (s"Online marking for ${assignmentName}") should be(true)
					findAll(cssSelector(".feedback-container")).size should be(2)
					pageSource contains (P.Student1.usercode) should be(true)
					pageSource contains (P.Student2.usercode) should be(true)
				}
		}
	}

	"Online marker" should "be able to feedback on an absent submission" in {
		withAssignment(moduleCode = moduleCode, assignmentName = assignmentName, members = Seq(P.Student3.usercode), assistants = Seq(P.Marker1.usercode)) {
			Given("We have one registered student")
			assignmentId => as(P.Marker1) {
				When("I go to the marking page")
				go to Path("/coursework/admin/module/" + moduleCode.toLowerCase + "/assignments/" + assignmentId + "/feedback/online")

				Then("I see the table of students")
				pageSource contains (s"Online marking for ${assignmentName}") should be(true)
				findAll(cssSelector(".feedback-container")).size should be(1)
				pageSource contains (P.Student3.usercode) should be(true)
			}
		}
	}

	"Student" should "be able to view feedback on their submission" in {
		withAssignment(moduleCode = moduleCode, assignmentName = assignmentName, assistants = Seq(P.Marker1.usercode)) {
			Given("We have two students with submitted assignments")
			assignmentId => setup(assignmentId)

				And("The assignment is closed for further submission")
				updateAssignment("xxx", assignmentName, Some(DateTime.now.minusDays(2)), Some(DateTime.now.minusDays(1)))

				as(P.Marker1) {
					And("I go to the marking page")
					go to Path("/coursework/admin/module/" + moduleCode.toLowerCase + "/assignments/" + assignmentId + "/feedback/online")

					And("I give feedback for student 1")
					val toggleLink = findAll(cssSelector("h6.toggle-icon")).filter(e=>e.text.contains(P.Student1.usercode)).next()
					toggleLink.underlying.click()
					eventually(getInputByLabel("Feedback") should be ('defined))

					textArea(cssSelector(s"#content-${P.Student1.warwickId} textarea")).value = "That was RUBBISH"
					textField(cssSelector(s"#content-${P.Student1.warwickId} input#mark")).value="12"
					textField(cssSelector(s"#content-${P.Student1.warwickId} input#grade")).value="F"
					find(cssSelector(s"#content-${P.Student1.warwickId} input.btn-primary")).get.underlying.click()
				}
			  as(P.Admin1) {
					And("I publish feedback")
					go to Path("/coursework/admin/module/" + moduleCode.toLowerCase + "/assignments/" + assignmentId + "/publish")
					checkbox(cssSelector("#confirmCheck")).select()
					eventually(
						find(cssSelector("#publish-submit")).get.underlying should be ('enabled)
					)
					find(cssSelector("#publish-submit")).get.underlying.click()
				}

				When("I log in as student1 and go to the feedback page")
			  as(P.Student1){
					go to Path(s"/coursework/module/${moduleCode.toLowerCase}/${assignmentId}")
					Then("I see the feedback")
					pageSource should include("That was RUBBISH")
				}
				And("I can download the results as a PDF")

				val pdfDownload = Download(Path(s"/coursework/module/${moduleCode.toLowerCase}/${assignmentId}/feedback.pdf")).as(P.Student1)
				pdfDownload should be ('successful)
				pdfDownload.contentAsString should include("PDF")
		}
	}


}