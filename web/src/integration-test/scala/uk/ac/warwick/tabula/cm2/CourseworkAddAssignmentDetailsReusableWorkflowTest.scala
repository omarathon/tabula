package uk.ac.warwick.tabula.cm2

import org.openqa.selenium.{By, WebElement}
import org.openqa.selenium.support.ui.Select
import org.scalatest.GivenWhenThen
import uk.ac.warwick.tabula.BrowserTest

import scala.collection.JavaConverters._
import scala.collection.mutable

/**
 * Test the setup-assignments form.
 */
class CourseworkAddAssignmentDetailsReusableWorkflowTest extends BrowserTest with CourseworkFixtures with GivenWhenThen {

	private def openCreateAssignmentDetails(moduleId: String): Unit = {
		When("I click manage this module drop down ")
		val manageModule = id(moduleId).webElement
		click on manageModule.findElement(By.partialLinkText("Manage this module"))
		Then("I should see the create new assignment option")
		val createAssignmentLink = manageModule.findElement(By.partialLinkText("Create new assignment"))
		eventually {
			createAssignmentLink.isDisplayed should be {
				true
			}
		}

		And("I click on create a new assignment link")
		click on linkText("Create new assignment")


	}
	private def createReusableAssigmentDetails(assignmentName: String, module: String, reusableWorkflowName: String): Unit = {
		When("I go to assignment creation page")
		currentUrl should include("/assignments/new")

		Then("I should be able to enter the necessary data")
		textField("name").value = assignmentName
		singleSel("workflowCategory").value = "R"

		val select = new Select(find(cssSelector("select[name=reusableWorkflow]")).get.underlying)
		select.selectByVisibleText(reusableWorkflowName)
		submitAndContinueClick()
	}

	private def assigmentFeedbackDetails(): Unit = {
		When("I go to feedback assignemnt page")
		currentUrl should include("/feedback")

		And("I uncheck automaticallyReleaseToMarkers on feedback details form")
		checkbox("automaticallyReleaseToMarkers").clear()

		And("I select collect marks")
		checkbox("collectMarks").select()

		submitAndContinueClick()
	}

	private def assigmentStudentDetails(): Unit = {
		When("I go to student  assignment students page")
		currentUrl should include("/students")

		And("I add some manual students")
		textArea("massAddUsers").value = "tabula-functest-student1 tabula-functest-student3"


		And("I click Add manual button ")
		val form = webDriver.findElement(By.id("command"))
		val manualAdd = webDriver.findElement(By.id("command")).findElement(By.className("add-students-manually"))
		manualAdd.click()

		Then("I should see manually enrolled students")
		eventually {
			val enrolledCount = form.findElement(By.className("enrolledCount"))
			val enrolledCountText = enrolledCount.getText()
			enrolledCountText should include("2 manually enrolled")
		}
		submitAndContinueClick()
	}


	private def assigmentMarkerDetails(): Unit = {
		When("I go to marker assignemnt page")
		currentUrl should include("/markers")

		And("I check unalloacted student list")
		val form = webDriver.findElement(By.id("command"))
		val markerStudentUnallocatedList = form.findElement(By.id("markerStudentsList"))
		markerStudentUnallocatedList.findElements(By.cssSelector("div.student-list li.student")).size() should be (2)

		And("I randomly allocate students")
		form.findElement(By.partialLinkText("Randomly allocate")).click()

		Then("Unallocated student list becomes 0")
		markerStudentUnallocatedList.findElements(By.cssSelector("div.student-list li.student")).size() should be (0)

		And("Markers get Students allocated")
		val markerStudentAllocatedList = form.findElement(By.id("markerMarkerList"))
		markerStudentAllocatedList.findElement(By.className("drag-count")).getText should be ("2")
		submitAndContinueClick()
	}


	private def assigmentSubmissionDetails(): Unit = {
		When("I go to assignment submissions assignemnt page")
		currentUrl should include("/submissions")

		And("I  check collect submissions")
		checkbox("collectSubmissions").select()

		And("I uncheck Plagiarism")
		checkbox("automaticallySubmitToTurnitin").clear()


		And("I change submission scope to restrict students")
		radioButtonGroup("restrictSubmissions").value = "true"

		And("I uncheck allowLateSubmissions")
		checkbox("allowLateSubmissions").clear()

		submitAndContinueClick()

	}

	private def assigmentOptions(): Unit = {
		When("I go to assignment options page")
		currentUrl should include("/options")

		And("Enter some data")
		singleSel("minimumFileAttachmentLimit").value = "2"
		singleSel("fileAttachmentLimit").value = "3"

		And("I enter data in file extension field")
		var fielExt = webDriver.findElement(By.id("fileExtensionList")).findElement(By.cssSelector("input.text"))
		click on fielExt
		enter("pdf txt")

		And("Enter some more data")
		textField("individualFileSizeLimit").value = "10"
		textField("wordCountMin").value = "100"
		textArea("wordCountConventions").value = "Exclude any bibliography or appendices from your word count-XXX."
		textArea("assignmentComment").value = "This assignment should not be ingored as it will impact final marks."
		submitAndContinueClick()
	}

	private def assigmentReview(assignmentName: String, reusableWorkflowName: String): Unit = {
		When("I go to assignment review page")
		currentUrl should include("/review")

		Then("I cross check various assignment details")

		var labels = webDriver.findElements(By.className("review-label")).asScala
		checkReviewTabRow(labels,"Assignment title",assignmentName)
		checkReviewTabRow(labels,"Marking workflow use", "Reusable")
		checkReviewTabRow(labels,"Marking workflow name", reusableWorkflowName)
		checkReviewTabRow(labels,"Marking workflow type", "Single marking")

		checkReviewTabRow(labels,"Automatically release to markers when assignment closes or after plagiarism check", "No")
		checkReviewTabRow(labels,"Collect marks", "Yes")

		checkReviewTabRow(labels,"Total number of students enrolled", "2")

		checkReviewTabRow(labels,"Collect submissions", "Yes")
		checkReviewTabRow(labels,"Automatically check submissions for plagiarism", "No")
		checkReviewTabRow(labels,"Submission scope", "Only students enrolled on this assignment can submit coursework")
		checkReviewTabRow(labels,"Allow new submissions after close date", "No")
	}

	private def checkReviewTabRow(labels: mutable.Buffer[WebElement], labelRow: String, fieldValue: String) = {
		var assignmentTitleElement = labels.find(_.getText.contains(labelRow)).getOrElse(fail(s"{labelRow} not found"))
		var parent = assignmentTitleElement.findElement(By.xpath(".."))
		parent.getText contains {fieldValue}
	}

	private def submitAndContinueClick(): Unit = {
		Then("I click submit button")
		val button = webDriver.findElement(By.id("command")).findElement(By.cssSelector("input[value='Save and continue']"))
		button.click()
	}


	"Department admin" should "be able to create reusable single marker workflow assignment" in  as(P.Admin1) {

		var assignmentName = "Test Module 2 Reusableworkflow Assignment"
		var reusableWorkflowName = "Single marker workflow"
		openAdminPage()
		openCreateAssignmentDetails("module-xxx02")

		createReusableAssigmentDetails(assignmentName, "module-xxx02",reusableWorkflowName)
		assigmentFeedbackDetails()
		assigmentStudentDetails()
		assigmentMarkerDetails()
		assigmentSubmissionDetails()
		assigmentOptions()
		assigmentReview(assignmentName,reusableWorkflowName)
	}

}
