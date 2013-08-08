package uk.ac.warwick.tabula.groups

import org.scalatest.GivenWhenThen
import uk.ac.warwick.tabula.BreadcrumbsMatcher
import uk.ac.warwick.tabula.groups.pages.{BatchOpenPage, SmallGroupTeachingPage}
import uk.ac.warwick.tabula.home.FeaturesDriver._


class GroupsHomePageTest extends SmallGroupsFixture with GivenWhenThen with BreadcrumbsMatcher{


		"Department Admin" should "be offered a link to the department's group pages" in {
		Given("the administrator is logged in and viewing the groups home page")
			signIn as(P.Admin1)  to (Path("/groups"))
			pageTitle should be ("Tabula - Small Group Teaching")

	  When("the administrator clicks to view the admin page")
				click on linkText("Go to the Test Services admin page")

		Then("The page should be the small group teaching page")
				breadCrumbsMatch(Seq("Small Group Teaching"))

		And("The page should display at least one module")
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

	it should "be able to open batches of groups" in {
		val groupsetSummaryPage = new SmallGroupTeachingPage("xxx")
		val TEST_MODULE_CODE = "xxx999"
		val TEST_GROUPSET_NAME="Test Tutorial"

		Given("The smallGroupTeachingStudentSignUp feature is enabled")
   		enableFeature("smallGroupTeachingStudentSignUp")

		And("There is a a groupset with an allocation method of StudentSignUp")
			createModule("xxx",TEST_MODULE_CODE,"Batch Opening Groupsets test")
		  createSmallGroupSet(TEST_MODULE_CODE,TEST_GROUPSET_NAME,allocationMethodName = "StudentSignUp", openForSignups = false)

		And("The administrator is logged in and viewing the groups home page")
		  signIn as(P.Admin1)  to groupsetSummaryPage.url

		  groupsetSummaryPage.getGroupsetInfo(TEST_MODULE_CODE, TEST_GROUPSET_NAME) should be ('defined)
		  val setInfo = groupsetSummaryPage.getGroupsetInfo(TEST_MODULE_CODE, TEST_GROUPSET_NAME).get

		Then("The Bulk Open Groups menu button is enabled")
		  groupsetSummaryPage.getBatchOpenButton() should be ('enabled)

		And("The open individual group button is enabled for the specified groupset")
      setInfo.getOpenButton() should be ('enabled)

		When("I click the batch open button")
		  groupsetSummaryPage.getBatchOpenButton().click()

		Then("The open page is displayed")
      val batchOpen = new BatchOpenPage("xxx")
		  batchOpen should be ('currentPage)

		When("I check the checkbox next to the groupset")
		  batchOpen.checkboxForGroupSet(setInfo) should be('enabled)
		  batchOpen.checkboxForGroupSet(setInfo).click()

		And("I click 'Open'")
			batchOpen.submit()

		Then("The open page is displayed again")
  		batchOpen should be ('currentPage)

		And("The checkbox to open the groupset is disabled")
  		batchOpen.checkboxForGroupSet(setInfo) should not be('enabled)

		When("I go back to the groups home page")
		val updatedSummary = new SmallGroupTeachingPage("xxx")
		  go to updatedSummary.url

		Then("The option to open the groupset is absent")
			updatedSummary.getGroupsetInfo(TEST_MODULE_CODE, TEST_GROUPSET_NAME).get should not be ('showingOpenButton)
	}

}
