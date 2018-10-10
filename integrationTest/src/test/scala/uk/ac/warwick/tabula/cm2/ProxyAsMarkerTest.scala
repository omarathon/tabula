package uk.ac.warwick.tabula.cm2

import org.joda.time.DateTime
import org.openqa.selenium.By
import uk.ac.warwick.tabula.data.model.markingworkflow.MarkingWorkflowType.SingleMarking
import uk.ac.warwick.tabula.{AcademicYear, BrowserTest}

class ProxyAsMarkerTest extends BrowserTest with CourseworkFixtures {

	private val currentYear = AcademicYear.now()

	private def openProxyMarkingScreen(): Unit = {

		When("I go the admin page")
		if (!currentUrl.contains("/department/xxx/20")) {
			click on linkText("Test Services")
		}

		loadCurrentAcademicYearTab()

		val testModulerow = id("main").webElement.findElements(By.cssSelector("span.mod-code")).get(0)
		click on testModulerow

		eventually {
			val moderatedMarkingAssignment = id("main").webElement.findElements(By.cssSelector(".striped-section-contents span div h5 a")).get(0)
			moderatedMarkingAssignment.getText should startWith("Single marking - single use")
			click on moderatedMarkingAssignment
		}

		currentUrl.contains("/summary") should be (true)

		val expandAssignmentUser = id("main").webElement.findElements(By.cssSelector(".student")).get(0) //tabula-functest-student1
		click on expandAssignmentUser

		val proxyLink = id("main").webElement.findElement(By.partialLinkText("Proxy"))
		click on proxyLink

		currentUrl.contains("#single-marker-tabula-functest-student1") should be (true)

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

				eventually {
					releaseForMarking(id)
				}

				openProxyMarkingScreen()

			}
		}
	}
}