package uk.ac.warwick.tabula.coursework

import scala.collection.JavaConverters._
import org.scalatest.BeforeAndAfter
import uk.ac.warwick.tabula.BrowserTest
import org.scalatest.BeforeAndAfterAll
import org.openqa.selenium.By

class CourseworkMarkingWorkflowTest extends BrowserTest with CourseworkFixtures {
		
	"Department admin" should "be able to manage marking workflow" in as(P.Admin1) {
		click on linkText("Go to the Test Services admin page")
		
		def openMarkingWorkflowSettings() = {
			click on (cssSelector(".dept-settings a.dropdown-toggle"))
			
			val markingWorkflowsLink = cssSelector(".dept-settings .dropdown-menu").webElement.findElement(By.partialLinkText("Marking workflows"))
			eventually {
				markingWorkflowsLink.isDisplayed should be (true)
			}
			click on (markingWorkflowsLink)
		}
		
		openMarkingWorkflowSettings()
		
		pageSource.contains("No marking workflows have been created yet") should be (true)
		
		click on (partialLinkText("Create"))
		
		textField("name").value = "Marking workflow 1"

		singleSel("markingMethod").value = "StudentsChooseMarker"

		textField("firstMarkers").value = P.Marker1.usercode
		
		// Ensure that another marker field has magically appeared
		eventually {
			findAll(cssSelector(".user-code-picker")).size should be (2)
		}
		
		new TextField(findAll(cssSelector(".user-code-picker")).toList(1).underlying).value = P.Marker2.usercode
		
		submit()
		
		// Ensure that we've been redirected back to the marking workflow page
		currentUrl should endWith ("/markingworkflows")
		
		// Create another marking workflow
		
		click on (partialLinkText("Create"))
		
		textField("name").value = "Marking workflow 2"
		textField("firstMarkers").value = P.Marker3.usercode
		
		submit()
		
		// Ensure that we now have two marking workflows on the page.
		className("marking-workflows").webElement.findElement(By.tagName("tbody")).findElements(By.tagName("tr")).size should be (2)
		
		// Edit marking workflow 1
		{
			val tbody = className("marking-workflows").webElement.findElement(By.tagName("tbody"))
			val row = tbody.findElements(By.tagName("tr")).asScala.find({ _.findElement(By.tagName("td")).getText == "Marking workflow 1" })
			row should be ('defined)
		
			click on (row.get.findElement(By.partialLinkText("Modify")))
		}
		
		textField("name").value should be ("Marking workflow 1")
		textField("firstMarkers").value should be (P.Marker1.usercode)
		
		// That's fine, let's cancel
		click on (linkText("Cancel"))
		
		// Delete marking workflow 2
		{
			val tbody = className("mark-schemes").webElement.findElement(By.tagName("tbody"))
			val row = tbody.findElements(By.tagName("tr")).asScala.find({ _.findElement(By.tagName("td")).getText == "Marking workflow 2" })
			row should be ('defined)
					
			click on (row.get.findElement(By.partialLinkText("Delete")))
		}
		
		eventually {
			find("marking-workflow-modal") map { _.isDisplayed } should be (Some(true))
		}
		
		eventuallyAjax {
			Option(id("marking-workflow-modal").webElement.findElement(By.className("btn-danger"))) should be ('defined)
		}
		
		click on (id("marking-workflow-modal").webElement.findElement(By.className("btn-danger")))
		
		eventually {
			// Should only have one marking workflow left
			className("marking-workflows").webElement.findElement(By.tagName("tbody")).findElements(By.tagName("tr")).size should be (1)
		}
	}
	
}