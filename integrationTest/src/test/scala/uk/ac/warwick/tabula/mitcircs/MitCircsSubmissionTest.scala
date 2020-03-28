package uk.ac.warwick.tabula.mitcircs

import org.joda.time.DateTimeConstants
import org.openqa.selenium.By
import org.openqa.selenium.support.ui.Select
import org.scalatest.GivenWhenThen
import uk.ac.warwick.tabula.helpers.DateBuilder
import uk.ac.warwick.tabula.{AcademicYear, BrowserTest, DateFormats}

import scala.jdk.CollectionConverters._

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

    And("I state that this submission doesn't relate to coronavirus")
    eventually {
      find(xpath("//input[@type='radio'][@name='covid19Submission'][@value='false']")).get.isDisplayed should be (true)
    }
    click on radioButton(xpath("//input[@type='radio'][@name='covid19Submission'][@value='false']"))
    eventually {
      // the first help icon for issue types should be visible (could check any element in the form but this is the first with an ID)
      find(id("popover-SeriousAccident")).get.isDisplayed should be (true)
    }

    And("I fill in some issue types")
    click on checkbox(xpath("//input[@type='checkbox'][@name='issueTypes'][@value='SeriousAccident']"))
    click on checkbox(xpath("//input[@type='checkbox'][@name='issueTypes'][@value='SeriousMedicalOther']"))
    click on checkbox(xpath("//fieldset[contains(@class, 'mitcircs-form__fields__section--covid19--no')]//input[@type='checkbox'][@name='issueTypes'][@value='Other']"))
    textField(xpath("//fieldset[contains(@class, 'mitcircs-form__fields__section--covid19--no')]//input[@name='issueTypeDetails']")).value = "The Night Heron calls me"

    And("I fill in affected dates")
    textField("startDate").value = DateFormats.DatePickerFormatter.print(AcademicYear.now().firstDay)
    textField("endDate").value = DateFormats.DatePickerFormatter.print(AcademicYear.now().next.firstDay.minusDays(1))

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
    click on xpath("//button[@type='submit'][contains(text(), 'Save draft')]")

    Then("I should be redirected to the mit circs details page")
    val heading = find(cssSelector(".id7-main-content h1")).get
    heading.text should startWith("Mitigating circumstances submission MIT-")

    val submissionKey = heading.text.trim().substring("Mitigating circumstances submission MIT-".length)
    currentUrl.split('?').head should endWith(s"personalcircs/mitcircs/view/$submissionKey")

    def fieldValue(field: String): String =
      find(xpath("//div[contains(@class, 'row form-horizontal')]/div[contains(@class, 'control-label')][contains(text(), '" + field.replace("'", "\\'") + "')]/..//div[contains(@class, 'form-control-static')]")).get.text

    And("The submission details should be visible")
    fieldValue("State") should include ("Draft")
    fieldValue("Issue type") should (include("Serious accident") and include("Serious accident or illness of someone close") and include("Other (The Night Heron calls me)"))
    fieldValue("Start date") should be (DateBuilder.format(AcademicYear.now().firstDay.toDateTimeAtStartOfDay, includeTime = false))
    fieldValue("End date") should be (DateBuilder.format(AcademicYear.now().next.firstDay.minusDays(1).toDateTimeAtStartOfDay, includeTime = false))
    fieldValue("Discussed submission with") should (include("Doctor / NHS services") and include("Other (The Night Heron)"))
    fieldValue("Details") should include("I was called by the Night Heron to a meet of the society.")

    case class AffectedAssessment(assessmentType: String, moduleCode: String, name: String, deadline: String)
    val affectedAssessments: Seq[AffectedAssessment] =
      findAll(xpath("//div[contains(@class, 'row form-horizontal')]/div[contains(@class, 'control-label')][contains(text(), 'Affected assessments')]/..//div[contains(@class, 'form-control-static')]/table/tbody/tr")).map { element =>
        element.underlying.findElements(By.tagName("td")).asScala.toArray match {
          case Array(assType, module, name, deadline) =>
            AffectedAssessment(
              assessmentType = assType.getText.trim(),
              moduleCode = module.findElement(By.className("mod-code")).getText.trim(),
              name = name.getText.trim(),
              deadline = deadline.getText.trim()
            )
        }
      }.toSeq

    affectedAssessments should (
      contain (AffectedAssessment("Assignment", "XXX01", "Cool essay", DateBuilder.format(AcademicYear.now().weeks(9).firstDay.withDayOfWeek(DateTimeConstants.THURSDAY).toDateTimeAtStartOfDay, includeTime = false, shortMonth = true, excludeCurrentYear = true))) and
      contain (AffectedAssessment("Exam", "XXX01", "Summer exam", DateBuilder.format(AcademicYear.now().weeks(40).firstDay.toDateTimeAtStartOfDay, includeTime = false, shortMonth = true, excludeCurrentYear = true))) and
      contain (AffectedAssessment("Other", "O", "Night Heron worship", "Unknown"))
    )

    click on linkText("file2.txt")

    eventually {
      find("mitcircs-details-attachment-modal").map(_.isDisplayed) should be(Some(true))
      val ifr = find(cssSelector(".modal-body iframe"))
      ifr.map(_.isDisplayed) should be(Some(true))
    }

    val iframe = frame(find(cssSelector(".modal-body iframe")).get)
    switch to iframe
    eventually {
      // Checks it's been displayed inline
      pageSource should include ("Here is another sample text file. You can use it for whatever you want, it's a magical mystery tour.")
    }

    switch to defaultContent
    eventually {
      val closeButton = xpath("//div[@class='modal-footer']/button[contains(text(), 'Close')]")
      if (closeButton.webElement.isDisplayed) {
        click on closeButton
      }

      id("mitcircs-details-attachment-modal").webElement.isDisplayed should be (false)
    }

    When("I submit the draft")
    click on linkText("Edit & submit submission")
    click on xpath("//button[@type='button'][contains(text(), 'Submit')]")

    // Need to wait for the modal
    eventually {
      val confirmButton = xpath("//button[@type='submit'][contains(text(), 'Confirm')]")
      confirmButton.webElement.isDisplayed should be (true)
      click on confirmButton
    }

    Then("I should be redirected to the mit circs details page")
    find(cssSelector(".id7-main-content h1")).get.text should startWith(s"Mitigating circumstances submission MIT-$submissionKey")
    currentUrl.split('?').head should endWith(s"personalcircs/mitcircs/view/$submissionKey")

    fieldValue("State") should include ("Submitted")
  }

}
