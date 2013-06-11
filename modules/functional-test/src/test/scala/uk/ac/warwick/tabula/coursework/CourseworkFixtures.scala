package uk.ac.warwick.tabula.coursework

import scala.collection.JavaConverters._
import org.joda.time.DateTime
import org.openqa.selenium.By
import org.openqa.selenium.WebDriver
import org.openqa.selenium.WebElement
import org.scalatest.selenium.WebBrowser.Element
import uk.ac.warwick.tabula.BrowserTest
import uk.ac.warwick.tabula.LoginDetails
import org.joda.time.format.DateTimeFormat
import org.openqa.selenium.htmlunit.HtmlUnitWebElement
import org.openqa.selenium.JavascriptExecutor
import org.openqa.selenium.Keys
import org.openqa.selenium.internal.seleniumemulation.FireEvent

trait CourseworkFixtures extends BrowserTest {
	
	before {
		go to (Path("/scheduling/fixtures/setup"))
	}
	
	def as[T](user: LoginDetails)(fn: => T) = {
		signIn as(user) to (Path("/coursework"))
		
		fn
	}
	
	/* Runs callback with assignment ID */
	def withAssignment(
			moduleCode: String, 
			assignmentName: String, 
			settings: Seq[String] => Unit = allFeatures,
			members: Seq[String] = Seq(P.Student1.usercode, P.Student2.usercode))(callback: String => Unit) = as(P.Admin1) {
		click on linkText("Go to the Test Services admin page")
		click on linkText("Show")
		
		// Add a module manager for moduleCode
		val info = getModuleInfo(moduleCode)

		click on (info.findElement(By.className("module-manage-button")).findElement(By.partialLinkText("Manage")))
		
		val addAssignment = info.findElement(By.partialLinkText("Add assignment"))
		eventually {
			addAssignment.isDisplayed should be (true)
		}

		click on (addAssignment)
		
		textField("name").value = assignmentName
		settings(members)

		submit()

		// Ensure that we've been redirected back
		withClue(pageSource) {
			currentUrl should endWith ("/department/xxx/#module-" + moduleCode.toLowerCase)
		}
		
		// NOTE: This assumes no duplicate assignment names!
		val assignmentInfo = getAssignmentInfo(moduleCode, assignmentName)
		
		val copyableUrl = assignmentInfo.findElement(By.className("linkForStudents")).getAttribute("href")
		val assignmentId = copyableUrl.substring(copyableUrl.lastIndexOf('/') + 1)
			
		callback(assignmentId)
	}
	
	def submitAssignment(user: LoginDetails, moduleCode: String, assignmentName: String, assignmentId: String, file: String, mustBeEnrolled: Boolean = true) = as(user) {
		if (mustBeEnrolled) {
			linkText(assignmentName).findElement should be ('defined)
			
			click on linkText(assignmentName)
				
			currentUrl should endWith(assignmentId + "/")
		} else {
			// Just go straight to the submission URL
			go to Path("/coursework/module/" + moduleCode.toLowerCase + "/" + assignmentId + "/")
		}
		
		// The assignment submission page uses FormFields which don't have readily memorable names, so we need to get fields by their label
		click on (getInputByLabel("File").orNull)
		pressKeys(getClass.getResource(file).getFile)
		
		new TextField(getInputByLabel("Word count").orNull).value = "1000"
			
		checkbox("plagiarismDeclaration").select()
		
		submit()
		
		pageSource contains "Thanks, we've received your submission." should be (true)
	}
		
	def allFeatures(members: Seq[String]) {
		// Change the open date to yesterday, else this test will fail in the morning
		executeScript(
			"""window.document.getElementsByName('openDate')[0].setAttribute('value', arguments[0]);""", 
			DateTime.now.minusDays(1).toString("dd-MMM-yyyy HH:mm:ss")
		)
		
		// TODO Can't test link to SITS for our fixture department
		// Don't bother messing around with assigning students, let's just assume students will magically find the submit page
		executeScript("jQuery('#assignmentEnrolmentFields details > *:not(summary)').show();") // expand expander
		className("show-adder").findElement map { _.underlying.isDisplayed } should be (Some(true))
		
		// Make sure JS is working
		id("js-hint").findElement should be ('empty)
		
		click on linkText("Add students manually")	
		eventually { textArea("massAddUsers").isDisplayed should be (true) }
		
		textArea("massAddUsers").value = members.mkString("\n")
		click on className("add-students")
		
		// Eventually, a Jax!
		eventuallyAjax { textArea("massAddUsers").isDisplayed should be (false) }
		
		pageSource contains(members.size + " manually enrolled") should be (true)
		
		checkbox("collectSubmissions").select()
		
		eventually { 
			find("submission-options") map { _.isDisplayed } should be (Some(true)) 
		}
		
		// Turn everything on
		checkbox("collectMarks").select()
		checkbox("displayPlagiarismNotice").select()
		radioButtonGroup("restrictSubmissions").value = "true"
		checkbox("allowResubmission").select()
		checkbox("allowExtensions").select()
		
		// Type the file types in so that the javascript understands
		find("fileExtensionList").get.underlying.findElement(By.tagName("input")).sendKeys("docx txt pdf")
		
		textArea("assignmentComment").value =
			"""Hello my special friends.
			
			Here is another paragraph"""
			
		textField("wordCountMin").value = "1"
		textField("wordCountMax").value = "10000"
	}
	
	def getModuleInfo(moduleCode: String) =
		findAll(className("module-info")).filter(_.underlying.findElement(By.className("mod-code")).getText == moduleCode.toUpperCase).next.underlying
	
	def getAssignmentInfo(moduleCode: String, assignmentName: String) =
		getModuleInfo(moduleCode).findElements(By.className("assignment-info")).asScala.filter(_.findElement(By.className("name")).getText.trim == assignmentName).head
	
	def getInputByLabel(label: String) =
		findAll(tagName("label")).find(_.underlying.getText.trim == label) map { _.underlying.getAttribute("for") } map { id(_).webElement }
	
}