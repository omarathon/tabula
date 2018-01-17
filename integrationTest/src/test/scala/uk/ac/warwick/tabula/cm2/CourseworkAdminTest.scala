package uk.ac.warwick.tabula.cm2

import uk.ac.warwick.tabula.BrowserTest

class CourseworkAdminTest extends BrowserTest with CourseworkFixtures {

	"Department admin" should "be offered a link to their department" in as(P.Admin1) {
		pageTitle should be ("Tabula - Coursework Management")
		pageSource contains "My department-wide responsibilities" should be {true}

		click on linkText("Test Services")

		// check that we can see some modules on the page.

		getModule("xxx01").get.isDisplayed should be {true}
		getModule("xxx02").get.isDisplayed should be {true}
		getModule("xxx03").get.isDisplayed should be {true}

		// check that we can hide modules
		click on linkText("All modules")
		eventually({
			val hide = cssSelector("input[name=showEmptyModules]").webElement
			hide.isDisplayed should be {true}
			click on hide
		})

		eventuallyAjax({
			getModule("xxx01").isDefined should be {false}
			getModule("xxx02").get.isDisplayed should be {true}
			getModule("xxx03").isDefined should be {false}
		})
	}
}
