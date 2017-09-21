package uk.ac.warwick.tabula.cm2

import org.joda.time.DateTime
import org.openqa.selenium.By
import uk.ac.warwick.tabula.data.model.markingworkflow.MarkingWorkflowType.SingleMarking
import uk.ac.warwick.tabula.{AcademicYear, BrowserTest}

class ProxyAsMarkerTest extends BrowserTest with CourseworkFixtures {

	private val currentYear = AcademicYear.guessSITSAcademicYearByDate(DateTime.now)

	private def openProxyMarkingScreen(): Unit = {

		When("I go the admin page")
		if (!currentUrl.contains("/department/xxx/20")) {
			click on linkText("Test Services")
		}

		loadCurrentAcademicYearTab()

		val testModulerow = id("main").webElement.findElements(By.cssSelector("span.mod-code")).get(0)
		click on testModulerow

		val moderatedMarkingAssignment = id("main").webElement.findElements(By.cssSelector(".striped-section-contents span div h5 a")).get(0)
		moderatedMarkingAssignment.getText should startWith("Single marking - single use")
		click on moderatedMarkingAssignment

		currentUrl.contains("/summary") should be (true)

		val expandAssignmentUser = id("main").webElement.findElements(By.cssSelector(".student")).get(0) //tabula-functest-student1
		click on expandAssignmentUser

		val proxyLink = id("main").webElement.findElement(By.partialLinkText("Proxy"))
		click on proxyLink

		currentUrl.contains("#single-marker-tabula-functest-student1") should be (true)

	}

	private def modifyMarksFeedback(): Unit = {

		eventuallyAjax {
			When("I add feedback, marks and grade")
			val feedback = id("main").webElement.findElement(By.tagName("textarea"))
			feedback.sendKeys("Well written essay")

			id("mark").webElement.sendKeys("71")

			id("grade").webElement.sendKeys("2.1")

			And("I upload a valid file")
			val uploadFile = id("main").webElement.findElement(By.name("file.upload"))
			click on uploadFile
			pressKeys(getClass.getResource("/adjustments.xlsx").getFile)

			And("submit the form")
			click on cssSelector(".btn-primary")

			Then("Results are saved and hidden away")
			feedback.isDisplayed should be (false)

			And("The Confirm and send to admin button is greyed out")
			cssSelector(".must-have-selected").webElement.isEnabled should be (false)
		}

		When("I select the student with feedback")
		cssSelector(".collection-checkbox").webElement.click()

		Then("Both the all results checkbox and the individual record checkbox are selected")
		cssSelector(".collection-checkbox").webElement.isSelected should be (true)
		cssSelector(".collection-check-all").webElement.isSelected should be (true)

		And("The Confirm and send to admin button is greyed out")
		cssSelector(".must-have-selected").webElement.isEnabled should be (true)

		When("I confirm and send to admin")
		cssSelector(".must-have-selected").webElement.click()

		Then("The marking should be complete")
		currentUrl should include("1/marking-completed")

	}


	"Department admin" should "be able to proxy as marker" in as(P.Admin1) {

		withAssignmentWithWorkflow(SingleMarking, Seq(P.Marker1, P.Marker2)) { id =>

			submitAssignment(P.Student1, "Single marking - single use", id, "/file1.txt", false)
			submitAssignment(P.Student2, "Single marking - single use", id, "/file2.txt", false)

			as(P.Admin1) {
				click on linkText("Coursework Management")
				currentUrl.contains("/coursework/") should be(true)

				click on linkText("Test Services")

				loadCurrentAcademicYearTab()
				currentUrl.contains("/department/xxx") should be (true)

				click on cssSelector("span.mod-code")

				eventuallyAjax {
					releaseForMarking(id)
				}

				openProxyMarkingScreen()
				modifyMarksFeedback()

			}
		}
	}
}