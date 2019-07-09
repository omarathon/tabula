package uk.ac.warwick.tabula.cm2

import org.openqa.selenium.By
import uk.ac.warwick.tabula.BrowserTest

class MinimumAttachmentsTest extends BrowserTest with CourseworkFixtures {

  private def openEditOptionsScreen(): Unit = {

    When("I expand module XXX02")
    val moduleBlock = eventually {
      id("main").webElement.findElements(By.cssSelector("h4.with-button")).get(1)
    }
    val arrow = moduleBlock.findElement(By.cssSelector(".fa-chevron-right"))
    click on arrow

    Then("The  module should expand")
    eventually {
      And("I should find a button to edit the CM2 assignment")
      println(id("main").webElement.findElements(By.xpath("//*[contains(text(),'Edit assignment')]")).size())
      val editAssignmentBtn = id("main").webElement.findElements(By.xpath("//*[contains(text(),'Edit assignment')]")).get(1)
      editAssignmentBtn.isDisplayed should be(true)
      editAssignmentBtn.isEnabled should be(true)
      click on editAssignmentBtn
    }

    eventually {
      And("I should be able to select the Options tab")
      val optionsLink = id("main").webElement.findElements(By.xpath("//*[contains(text(),'Options')]")).get(0)
      click on optionsLink
    }

  }

  private def changeAttachmentsNo(): Unit = {
    click on id("minimumFileAttachmentLimit")
    click on id("main").webElement.findElements(By.xpath("//option[@value='2']")).get(0)

    id("minimumFileAttachmentLimit").webElement.getAttribute("value") should be("2")

    click on id("fileAttachmentLimit")
    click on id("main").webElement.findElements(By.xpath("//option[@value='5']")).get(1)

    id("fileAttachmentLimit").webElement.getAttribute("value") should be("5")

    val saveAndExitBtn = id("main").webElement.findElements(By.cssSelector(".btn-primary")).get(1)
    click on saveAndExitBtn

    eventually {
      currentUrl should include("/summary")
    }
  }

  private def addAttachments(): Unit = {

    click on linkText("Coursework Management")
    currentUrl should include("/coursework/")

    val submitLink = eventually {
      id("main").webElement.findElement(By.xpath("//*[contains(text(),'Premarked assignment CM2')]"))
        .findElement(By.xpath("../../../../div[contains(@class, 'item-info')]")).findElements(By.cssSelector(".btn-primary")).get(0)
    }
    click on submitLink

    When("I upload a file for submission")
    click on find(cssSelector("input[type=file]")).get
    pressKeys(getClass.getResource("/file1.txt").getFile)

    And("press submit")
    click on id("main").webElement.findElements(By.cssSelector(".btn-primary")).get(0)

    eventually {
      Then("an error around file numbers will be shown")
      val errorMsg = cssSelector(".has-error").webElement.findElements(By.xpath("//*[contains(text(),'You need to at least submit 2 files.')]")).get(0)
      errorMsg.isDisplayed should be(true)
    }

    When("I upload another file")
    click on find(cssSelector("input[type=file]")).get
    pressKeys(getClass.getResource("/file2.txt").getFile)

    And("press submit")
    click on id("main").webElement.findElements(By.cssSelector(".btn-primary")).get(0)

    eventually {
      Then("an error around file numbers is no longer shown")
      val fileCountErrorMsg = cssSelector(".has-error").webElement.findElements(By.xpath("//*[contains(text(),'You need to at least submit 2 files.')]")).size()
      fileCountErrorMsg should be(0)
    }


  }

  "Department admin" should "be able to be able to specify the minimum number of attachments for an assignment" in as(P.Admin1) {
    openAdminPage()
    loadCurrentAcademicYearTab()
    openEditOptionsScreen()
    changeAttachmentsNo()

    as(P.Student1) {
      addAttachments()
    }
  }

}

