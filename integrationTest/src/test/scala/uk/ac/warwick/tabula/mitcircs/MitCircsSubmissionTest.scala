package uk.ac.warwick.tabula.mitcircs

import org.joda.time.DateTimeConstants
import org.openqa.selenium.support.ui.Select
import org.scalatest.GivenWhenThen
import uk.ac.warwick.tabula.{AcademicYear, BrowserTest, DateFormats}

class MitCircsSubmissionTest extends BrowserTest with GivenWhenThen with MitCircsFixture {

  before {
    setupTestData()

    Given("Mitigating circumstances is enabled on the department")
    enableMitCircsAndSetUpMCO()
  }

  "A student" should "be able to create a mitigating circumstances submission" in as(P.Student1) {
    When("A student visits their profile")
    go to Path("/profiles") // Student should be redirected to their profile
    currentUrl should endWith(s"/profiles/view/${P.Student1.warwickId}")

    And("They view the Personal Circumstances page")
    click on linkText("Personal circumstances")
    currentUrl should endWith("/personalcircs")

    And("They click on the link to declare mitigating circumstances")
    click on linkText("Declare mitigating circumstances")
    currentUrl should endWith("/mitcircs/new")

    And("I fill in some issue types")
    click on checkbox(xpath("//input[@type='checkbox'][@name='issueTypes'][@value='SeriousAccident']"))
    click on checkbox(xpath("//input[@type='checkbox'][@name='issueTypes'][@value='SeriousMedicalOther']"))
    click on checkbox(xpath("//input[@type='checkbox'][@name='issueTypes'][@value='Other']"))
    textField("issueTypeDetails").value = "The Night Heron calls me"

    And("I fill in affected dates")
    textField("startDate").value = DateFormats.DatePickerFormatter.print(AcademicYear.now().firstDay)
    textField("endDate").value = DateFormats.DatePickerFormatter.print(AcademicYear.now().lastDay)

    Then("The affected assessments table should load")
    eventually {
      xpath("//tbody[@id='assessment-table-assignments']/tr").findAllElements.size should be (1)
    }

    When("I select the affected assessment and fill in a deadline for Thursday of week 9")
    click on checkbox(xpath("//tbody[@id='assessment-table-assignments']/tr/td[@class='mitcircs-form__fields__section__assessments-table__checkbox']/input[@type='checkbox']"))
    textField(xpath("//tbody[@id='assessment-table-assignments']/tr/td[@class='mitcircs-form__fields__section__assessments-table__deadline']//input")).value = DateFormats.DatePickerFormatter.print(AcademicYear.now().weeks(9).firstDay.withDayOfWeek(DateTimeConstants.THURSDAY))

    And("I add an exam on Monday of week 40")
    click on linkText("Exams")
    eventually {
      find(id("assessment-table-exams")).get.isDisplayed should be (true)
    }

    new Select(find(id("new-assessment-module")).get.underlying).selectByVisibleText("XXX01 Test Module 1")
    textField(id("new-assessment-name")).value = "Summer exam"
    textField(id("new-assessment-deadline")).value = DateFormats.DatePickerFormatter.print(AcademicYear.now().weeks(40).firstDay)
    click on xpath("//table[contains(@class, ' mitcircs-form__fields__section__assessments-table ')]/tfoot/tr/td/button[contains(text(), 'Add')]")

    eventually {
      xpath("//tbody[@id='assessment-table-exams']/tr").findAllElements.size should be (1)
    }

    And("I add an 'Other' assessment")
    click on linkText("Other")
    eventually {
      find(id("assessment-table-other")).get.isDisplayed should be (true)
    }

    new Select(find(id("new-assessment-module")).get.underlying).selectByVisibleText("Other")
    textField(id("new-assessment-name")).value = "Night Heron worship"
    click on xpath("//table[contains(@class, ' mitcircs-form__fields__section__assessments-table ')]/tfoot/tr/td/button[contains(text(), 'Add')]")

    eventually {
      xpath("//tbody[@id='assessment-table-other']/tr").findAllElements.size should be (1)
    }

    And("I declare that I've discussed this with my doctor and the Night Heron")
    click on radioButton(xpath("//input[@type='radio'][@name='contacted'][@value='true']"))
    click on checkbox(xpath("//input[@type='checkbox'][@name='contacts'][@value='Doctor']"))
    click on checkbox(xpath("//input[@type='checkbox'][@name='contacts'][@value='Other']"))
    textField("contactOther").value = "The Night Heron"

    And("I provide some details")
    textArea("reason").value =
      """I was called by the Night Heron to a meet of the society.
        |
        |While we were travelling, we were involved in an accident and myself and my mother swapped bodies.""".stripMargin

    And("I provide evidence")
    find(cssSelector("input[type=file]")).get.underlying.sendKeys(getClass.getResource("/file2.txt").getFile)

    And("I save the submission as a draft")
    click on xpath("//button[@type='submit'][contains(text(), 'Save draft')")

    Then("I should be redirected to the mit circs details page")
    val heading = find(cssSelector(".id7-main-content h1")).get
    heading.text should startWith("Mitigating circumstances submission MIT-")

    val submissionKey = heading.text.trim().substring("Mitigating circumstances submission MIT-".length)
    currentUrl should endWith(s"personalcircs/mitcircs/view/$submissionKey")

    fail("Is this thing on?")
  }

}
