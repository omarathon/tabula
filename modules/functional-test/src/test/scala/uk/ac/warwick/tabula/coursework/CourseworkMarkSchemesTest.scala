package uk.ac.warwick.tabula.coursework

import scala.collection.JavaConverters._
import org.scalatest.BeforeAndAfter
import uk.ac.warwick.tabula.BrowserTest
import org.scalatest.BeforeAndAfterAll
import org.openqa.selenium.By

class CourseworkMarkSchemesTest extends BrowserTest with CourseworkFixtures {
		
	"Department admin" should "be able to manage mark schemes" in as(P.Admin1) {
		click on linkText("Go to the Test Services admin page")
		
		def openMarkSchemesSettings() = {
			click on (cssSelector(".dept-settings a.dropdown-toggle"))
			
			val markSchemesLink = cssSelector(".dept-settings .dropdown-menu").webElement.findElement(By.partialLinkText("Mark schemes"))
			eventually {
				markSchemesLink.isDisplayed should be (true)
			}
			click on (markSchemesLink)
		}
		
		openMarkSchemesSettings()
		
		pageSource.contains("No mark schemes have been created yet") should be (true)
		
		click on (partialLinkText("Create"))
		
		textField("name").value = "Mark Scheme 1"

		singleSel("markingMethod").value = "StudentsChooseMarker"

		textField("firstMarkers").value = P.Marker1.usercode
		
		// Ensure that another marker field has magically appeared
		eventually {
			findAll(cssSelector(".user-code-picker")).size should be (2)
		}
		
		new TextField(findAll(cssSelector(".user-code-picker")).toList(1).underlying).value = P.Marker2.usercode
		
		submit()
		
		// Ensure that we've been redirected back to the mark schemes page
		currentUrl should endWith ("/markschemes")
		
		// Create another mark scheme
		
		click on (partialLinkText("Create"))
		
		textField("name").value = "Mark Scheme 2"
		textField("firstMarkers").value = P.Marker3.usercode
		
		submit()
		
		// Ensure that we now have two mark schemes on the page.
		className("mark-schemes").webElement.findElement(By.tagName("tbody")).findElements(By.tagName("tr")).size should be (2)
		
		// Edit Mark Scheme 1
		{
			val tbody = className("mark-schemes").webElement.findElement(By.tagName("tbody"))
			val row = tbody.findElements(By.tagName("tr")).asScala.find({ _.findElement(By.tagName("td")).getText == "Mark Scheme 1" })
			row should be ('defined)
		
			click on (row.get.findElement(By.partialLinkText("Modify")))
		}
		
		textField("name").value should be ("Mark Scheme 1")
		textField("firstMarkers").value should be (P.Marker1.usercode)
		
		// That's fine, let's cancel
		click on (linkText("Cancel"))
		
		// Delete Mark Scheme 2
		{
			val tbody = className("mark-schemes").webElement.findElement(By.tagName("tbody"))
			val row = tbody.findElements(By.tagName("tr")).asScala.find({ _.findElement(By.tagName("td")).getText == "Mark Scheme 2" })
			row should be ('defined)
					
			click on (row.get.findElement(By.partialLinkText("Delete")))
		}
		
		eventually {
			find("markscheme-modal") map { _.isDisplayed } should be (Some(true))
		}
		
		eventuallyAjax {
			Option(id("markscheme-modal").webElement.findElement(By.className("btn-danger"))) should be ('defined)
		}
		
		click on (id("markscheme-modal").webElement.findElement(By.className("btn-danger")))
		
		eventually {
			// Should only have one mark scheme left
			className("mark-schemes").webElement.findElement(By.tagName("tbody")).findElements(By.tagName("tr")).size should be (1)
		}
	}
	
}