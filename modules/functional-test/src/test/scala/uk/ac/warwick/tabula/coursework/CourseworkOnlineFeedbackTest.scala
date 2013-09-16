package uk.ac.warwick.tabula.coursework

import org.scalatest.GivenWhenThen
import uk.ac.warwick.tabula.BrowserTest

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
				pageSource contains (s"Online marking for ${assignmentName}") should be (true)
				findAll(cssSelector(".feedback-container")).size should be (2)
				pageSource contains (P.Student1.usercode) should be (true)
				pageSource contains (P.Student2.usercode) should be (true)
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
				pageSource contains (s"Online marking for ${assignmentName}") should be (true)
				findAll(cssSelector(".feedback-container")).size should be (1)
				pageSource contains (P.Student3.usercode) should be (true)
			}
		}
	}
}