package uk.ac.warwick.tabula.cm2

import org.openqa.selenium.By
import uk.ac.warwick.tabula.data.model.markingworkflow.MarkingWorkflowType.SingleMarking
import uk.ac.warwick.tabula.{AcademicYear, BrowserTest}

class ProxyAsMarkerTest extends BrowserTest with CourseworkFixtures {

 // private val currentYear = AcademicYear.now()

  private def openProxyMarkingScreen(): Unit = {

    When("I go the admin page")
    if (!currentUrl.contains("/department/xxx/20")) {
      click on linkText("Test Services")
    }

    loadCurrentAcademicYearTab()

    eventually {
      val testModulerow = id("main").webElement.findElement(By.partialLinkText("XXX01"))
      click on testModulerow
    }

    eventually {
      pageSource contains "Single marking - single use" should be (true)
      When("I click on the Single marking - single use assignment link")
      click on id("main").webElement.findElement(By.partialLinkText("Single marking - single use"))
    }
    Then("I see the summary assignment  screen")
    currentUrl.contains("/summary") should be(true)

    val expandAssignmentUser = eventually(id("main").webElement.findElements(By.className("student")).get(0)) //tabula-functest-student1
    click on expandAssignmentUser

    val proxyLink = id("main").webElement.findElement(By.partialLinkText("Proxy"))
    click on proxyLink

    currentUrl.contains("#single-marker-tabula-functest-student1") should be(true)

  }

  "Department admin" should "be able to proxy as marker" in as(P.Admin1) {
    //xxx01 module related assignment
    withAssignmentWithWorkflow(SingleMarking, Seq(P.Marker1, P.Marker2)) { assignmentId =>

      submitAssignment(P.Student1, "Single marking - single use", assignmentId, "/file1.txt", false)
      submitAssignment(P.Student2, "Single marking - single use", assignmentId, "/file2.txt", false)

      as(P.Admin1) {
        click on linkText("Coursework Management")
        currentUrl.contains("/coursework/") should be(true)

        click on linkText("Test Services")

        loadCurrentAcademicYearTab()

        currentUrl.contains("/department/xxx") should be(true)

        eventually(click on id("main").webElement.findElement(By.partialLinkText("XXX01")))

        eventually(releaseForMarking(assignmentId))

        openProxyMarkingScreen()

      }
    }
  }
}