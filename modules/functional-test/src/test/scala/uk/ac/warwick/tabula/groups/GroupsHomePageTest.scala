package uk.ac.warwick.tabula.groups

import org.scalatest.GivenWhenThen


class GroupsHomePageTest extends SmallGroupsFixture with GivenWhenThen{

  "Department Admin" should "be offered a link to the department's group pages" in {


		Given("the administrator is logged in and viewing the groups home page")
			as(P.Admin1){
			pageTitle should be ("Tabula - Small Group Teaching")

	  When("the administrator clicks to view the admin page")
				click on linkText("Go to the Test Services admin page")

		Then("The page should display at least one module")
			findAll(className("module-info")).toList should not be (Nil)

		And("Some modules should be hidden")
			val allDisplayed = findAll(className("module-info")).forall(_.isDisplayed)
			allDisplayed should be (false)

		When("The administrator clicks the 'show' link")
			click on (linkText("Show"))

		Then("All modules should be displayed")
			for (info <- findAll(className("module-info")))
				info.isDisplayed should be (true)
		}
	}
}
