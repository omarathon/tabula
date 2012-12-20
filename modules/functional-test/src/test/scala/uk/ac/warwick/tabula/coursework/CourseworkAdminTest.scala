package uk.ac.warwick.tabula.coursework

import uk.ac.warwick.tabula.BrowserTest

class CourseworkAdminTest extends BrowserTest {

	"Coursework admin" should "be offered a link to their department" in {
		signIn as(P.Admin1) to (Path("/coursework"))
		pageTitle should be ("Tabula - Coursework Management")
		click on linkText("Go to the IT Services admin page")
		
		// check that we can see some modules on the page.
		findAll(className("module-info")).toList should not be (Nil)
	}
	
}