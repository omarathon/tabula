package uk.ac.warwick.tabula.coursework

import scala.collection.JavaConverters._
import org.scalatest.BeforeAndAfter
import uk.ac.warwick.tabula.BrowserTest
import org.scalatest.BeforeAndAfterAll
import org.openqa.selenium.By

class CourseworkAdminTest extends BrowserTest with CourseworkFixtures {
	
	"Department admin" should "be offered a link to their department" in as(P.Admin1) {
		pageTitle should be ("Tabula - Coursework Management")
		click on linkText("Go to the Test Services admin page")
		
		// check that we can see some modules on the page.
		findAll(className("module-info")).toList should not be (Nil)
		
		// But check that they're all hidden
		for (info <- findAll(className("module-info")))
			info.isDisplayed should be (false)
		
		click on (linkText("Show all modules"))
		
		// Now all modules should be displayed
		for (info <- findAll(className("module-info")))
			info.isDisplayed should be (true)
	}
	
}