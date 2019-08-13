package uk.ac.warwick.tabula.exams

import org.openqa.selenium.{By, WebElement}
import org.scalatest.GivenWhenThen
import org.scalatest.exceptions.TestFailedException
import uk.ac.warwick.tabula.AcademicYear

class AdministerExamsTest extends ExamFixtures
  with GivenWhenThen {

  val year: Int = AcademicYear.now().startYear

  "Department admin" should "not be able to create exams via Exam Management component" in as(P.Admin1) {
    Given("The exams home page")
    pageTitle should be("Tabula - Exam Management")

    Then("The user should be shown a link to their department exams")
    click on linkText("Go to the Test Services admin page")
    pageSource contains "Test Services" should be (true)

    And("By default none will be shown until clicking the 'Show' button")
    Then("I click on the show button")
    click on linkText("Show")

    Then("The user should be able to select the 'Create new exam' option from the 'Manage' dropdown")
    var info = eventually {
      getModuleInfo("XXX01")
    }
    val manageButton = info.findElement(By.className("module-manage-button")).findElement(By.linkText("Manage"))
    eventually {
      manageButton.isDisplayed should be(true)
    }
    click on manageButton

    And("I click on the 'Create new exam' link")
    eventually {
      val createNewExam = info.findElement(By.partialLinkText("Create new exam"))
      createNewExam.isDisplayed should be(true)
      click on createNewExam
    }

    Then("This should show the create exam page")
    currentUrl should include(s"/admin/module/xxx01/$year/exams/new")
    pageSource contains "It's no longer possible to create new exams in Exam Management." should be (true)
  }

  def getModuleInfo(moduleCode: String): WebElement = {
    val moduleInfoBlocks = findAll(className("module-info"))

    if (moduleInfoBlocks.isEmpty)
      throw new TestFailedException("No module-info blocks found on this page. Check it's the right page and that the user has permission.", 0)

    val matchingModule = moduleInfoBlocks.filter(_.underlying.findElement(By.className("mod-code")).getText == moduleCode.toUpperCase)

    if (matchingModule.isEmpty)
      throw new TestFailedException(s"No module-info found for ${moduleCode.toUpperCase}", 0)

    matchingModule.next().underlying
  }
}
