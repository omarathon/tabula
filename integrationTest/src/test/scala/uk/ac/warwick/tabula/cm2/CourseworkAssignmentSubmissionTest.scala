package uk.ac.warwick.tabula.cm2

import org.openqa.selenium.By
import uk.ac.warwick.tabula.BrowserTest

class CourseworkAssignmentSubmissionTest extends BrowserTest with CourseworkFixtures {

  def options(): Unit = {
    singleSel("minimumFileAttachmentLimit").value = "2"
    singleSel("fileAttachmentLimit").value = "3"
  }

  // TAB-413, TAB-415
  "Student" should "be able to submit assignment after validation errors without re-uploading file" in {

    withAssignment("xxx01", "Min 2 attachments", optionSettings = options) { assignmentId =>

      as(P.Student1) {
        val submitLink = eventually {
          id("main").webElement.findElement(By.xpath("//*[contains(text(),'Min 2 attachments')]"))
            .findElement(By.xpath("../../../../div[contains(@class, 'item-info')]")).findElement(By.linkText("Submit assignment"))
        }
        click on submitLink

        currentUrl should endWith(assignmentId)

        find(cssSelector("input[type=file]")).get.underlying.sendKeys(getClass.getResource("/file1.txt").getFile)
        // Don't upload the second file yet
        click on cssSelector(".btn-primary")

        eventually {
          pageSource contains "Thanks, we've received your submission." should be(false)
          pageSource contains "You need to at least submit 2 files" should be (true)

          find(cssSelector("input[type=file]")).get.underlying.sendKeys(getClass.getResource("/file2.txt").getFile)
          click on cssSelector(".btn-primary")

          eventually {
            pageSource contains "Thanks, we've received your submission." should be(true)

            linkText("file1.txt").webElement.isDisplayed should be(true)
          }
        }
      }
    }
  }

  "Student" should "be able to submit assignment" in {
    withAssignment("xxx01", "Fully featured assignment") { assignmentId =>
      submitAssignment(P.Student1, "Fully featured assignment", assignmentId, "/file1.txt")
      verifyPageLoaded(pageSource contains "Thanks, we've received your submission." should be (true))
    }
  }

  "Student" should "see a validation error when submitting less than the minimum number of files" in {

    withAssignment("xxx01", "Min 2 attachments", optionSettings = options) { assignmentId =>
      submitAssignment(P.Student1, "Min 2 attachments", assignmentId, "/file1.txt")

      eventually {
        pageSource contains "You need to at least submit 2 files" should be (true)
      }
    }
  }

}
