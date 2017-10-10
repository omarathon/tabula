package uk.ac.warwick.tabula.cm2

import uk.ac.warwick.tabula.BrowserTest

class CourseworkAssignmentSubmissionTest extends BrowserTest with CourseworkFixtures {

	// TAB-413, TAB-415
	"Student" should "be able to submit assignment after validation errors without re-uploading file" in {
		withAssignment("xxx01", "Fully featured assignment") { assignmentId =>
			as(P.Student1) {
				click on linkText("Fully featured assignment")
				currentUrl should endWith(assignmentId)

				click on find(cssSelector("input[type=file]")).get
				pressKeys(getClass.getResource("/file1.txt").getFile)

				submit()

				pageSource contains "Thanks, we've received your submission." should be (true)

				linkText("file1.txt").webElement.isDisplayed should be (true)
			}
		}
	}

	"Student" should "be able to submit assignment" in {
		withAssignment("xxx01", "Fully featured assignment") { assignmentId =>
			submitAssignment(P.Student1, "Fully featured assignment", assignmentId, "/file1.txt")
			verifyPageLoaded(pageSource contains "Thanks, we've received your submission." should be {true})
		}
	}

	"Student" should "see a validation error when submitting less than the minimum number of files" in {

		def options() = {
			singleSel("minimumFileAttachmentLimit").value = "2"
			singleSel("fileAttachmentLimit").value = "3"
		}

		withAssignment("xxx01", "Min 2 attachments", optionSettings = options) { assignmentId =>
			submitAssignment(P.Student1, "Min 2 attachments", assignmentId, "/file1.txt")
			pageSource contains "You need to at least submit 2 files" should be {true}
		}
	}

}