package uk.ac.warwick.tabula.cm2

import org.openqa.selenium.By
import uk.ac.warwick.tabula.{AcademicYear, BrowserTest}

import scala.jdk.CollectionConverters._

class CopyAssignmentsTest extends BrowserTest with CourseworkFixtures {

  private def openAssignmentsScreen(): Unit = {
    When("I go the admin page")
    if (!currentUrl.contains("/department/xxx/20")) {
      click on linkText("Test Services")
    }

    // make sure we are looking at the current academic year
    click on cssSelector(".navbar-tertiary").webElement.findElement(By.partialLinkText(s"${AcademicYear.now().toString}"))


    Then("I should be able to click on the Assignments dropdown")
    val toolbar = findAll(className("dept-toolbar")).next().underlying
    click on toolbar.findElement(By.partialLinkText("Assignments"))

    And("I should see the create assignments from previous in the assignments menu option")
    val createAssignmentsLink = toolbar.findElement(By.partialLinkText("Create assignments from previous"))
    eventually(timeout(45.seconds), interval(300.millis))({
      createAssignmentsLink.isDisplayed should be (true)
    })

    When("I click the create assignments from previous link")
    click on createAssignmentsLink

    eventually(timeout(45.seconds), interval(300.millis))({
      Then("I should reach the create assignments from previous page")
      currentUrl should include("/copy-assignments")
    })
  }

  private def copyCM2Assignment(): Unit = {

    When("I select the Premarked assignment checkbox")
    val tbody2 = id("main").webElement.findElement(By.tagName("tbody"))
    val row2 = tbody2.findElements(By.tagName("tr")).get(3)
    row2.getText should startWith("Premarked assignment CM2")

    val td2 = row2.findElements(By.tagName("td")).get(0)

    val checkbox2 = td2.findElement(By.xpath("input[@class='collection-checkbox']"))

    click on checkbox2
    checkbox2.isSelected should be(true)

    When("I choose to confirm")
    val confirmBtn = id("main").webElement.findElement(By.xpath("//input[@value='Confirm']"))
    click on confirmBtn

    When("I choose to save the changes")
    val confirmModalBtn = id("main").webElement.findElement(By.xpath("//button[@name='submit']"))
    eventually({
      confirmModalBtn.isDisplayed should be(true)
      click on confirmModalBtn
    })

    eventually(timeout(45.seconds), interval(300.millis))({
      Then("The changes are saved and I am redirected")
      currentUrl should not include "copy-assignments"
    })

    eventually {
      When("I expand Test Module 2")
      val testModule = id("main").webElement.findElements(By.className("fa-chevron-right")).get(1)
      click on testModule
    }

    eventually(timeout(45.seconds), interval(300.millis))({
      Then("Assignments should be displayed")
      val content = id("main").webElement.findElement(By.xpath("//*[contains(text(),'Premarked assignment CM2')]"))
      content.isDisplayed should be(true)
    })

    Then("2 assignments should be found")
    val cm2AssignmentsCnt = id("main").webElement.findElements(By.xpath("//*[contains(text(),'Premarked assignment CM2')]")).size()
    cm2AssignmentsCnt should be(2)

  }

  "Department admin" should "be able to copy assignments" in as(P.Admin1) {
    openAssignmentsScreen()
    copyCM2Assignment()

  }
}
