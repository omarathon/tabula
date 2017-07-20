package uk.ac.warwick.tabula.cm2

import uk.ac.warwick.tabula.BrowserTest

class CourseworkAdminTest extends BrowserTest with CourseworkFixtures {

	"Department admin" should "be offered a link to their department" in as(P.Admin1) {
		pageTitle should be ("Tabula - CM2")
		pageSource contains "My department-wide responsibilities" should be {true}

		click on linkText("Test Services")

		// check that we can see some modules on the page.

		id("module-xxx01").webElement.isDisplayed should be {true}
		id("module-xxx02").webElement.isDisplayed should be {true}
		id("module-xxx03").webElement.isDisplayed should be {true}

		// check that we can hide modules
		click on linkText("All modules")
		eventually({
			val hide = cssSelector("input[name=showEmptyModules]").webElement
			hide.isDisplayed should be {true}
			click on hide
		})

		eventuallyAjax({
			find(id("module-xxx01")).isDefined should be {false}
			id("module-xxx02").webElement.isDisplayed should be {true}
			find(id("module-xxx03")).isDefined should be {false}
		})
	}
}