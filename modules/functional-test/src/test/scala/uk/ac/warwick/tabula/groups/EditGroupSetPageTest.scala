package uk.ac.warwick.tabula.groups

import org.scalatest.GivenWhenThen
import org.openqa.selenium.{WebDriver, By}
import uk.ac.warwick.tabula.home.FeaturesDriver._
import uk.ac.warwick.tabula.groups.pages.SmallGroupTeachingPage
import org.scalatest.selenium.WebBrowser

class EditGroupSetPageTest  extends SmallGroupsFixture  with GivenWhenThen{

	"Department Admin" should "be able to view the Edit page for an existing group" in {
		Given("I am logged in as admin")
   		signIn as(P.Admin1)  to (Path("/groups"))
		And("I view the small group page for test services")
			val groupsPage = new SmallGroupTeachingPage("xxx")
			go to groupsPage.url

		When("I click on the 'Actions' dropdown for module xxx01/Test Lab")
		  val editPage = groupsPage.getGroupsetInfo("xxx101", "Test Lab").get.goToEditProperties

		Then("The page is the edit properties page")
			editPage.isCurrentPage("xxx101")

		}

	"Department Admin" should "be able to view and set information visibility options" in {
		val groupsetSummaryPage = new SmallGroupTeachingPage("xxx")

		Given("The smallGroupTeachingStudentSignUp feature is enabled")
			enableFeature("smallGroupTeachingStudentSignUp")

		When("I log in as admin")
   		signIn as(P.Admin1)  to (Path("/groups"))

		And(" I view the edit properties page for xxx101/Test Lab")
		  go to groupsetSummaryPage.url
		  val editProperties = groupsetSummaryPage.getGroupsetInfo("xxx101", "Test Lab").get.goToEditProperties

		Then("I should see the options to show/hide tutor name")
			checkbox("studentsCanSeeTutorName") should not be (null)
		  checkbox("studentsCanSeeOtherMembers") should not be (null)

		When("I check the checkboxes and click Save")
			checkbox("studentsCanSeeTutorName").select()
		  checkbox("studentsCanSeeOtherMembers").select()
			editProperties.submit()

		Then("The page is the groupset summary page")
		  groupsetSummaryPage should be('currentPage)

		When("I navigate to the edit properties page again")
		  groupsetSummaryPage.getGroupsetInfo("xxx101", "Test Lab").get.goToEditProperties

		Then("The checkboxes should still be checked")
		  checkbox("studentsCanSeeTutorName").value should be("true")
		  checkbox("studentsCanSeeOtherMembers").value should be("true")

	}

	"Department Admin" should "be able to set a default maximum group size" in {
		val groupsetSummaryPage = new SmallGroupTeachingPage("xxx")

		When("I log in as admin")
		  signIn as(P.Admin1)  to (Path("/groups"))

		And("I view the edit properties page for xxx101/Test Lab")
		  go to groupsetSummaryPage.url
		  val editProperties = groupsetSummaryPage.getGroupsetInfo("xxx101", "Test Lab").get.goToEditProperties

		Then("I should see the options to set a default maximum group size")
		  checkbox("defaultMaxGroupSizeEnabled") should not be (null)
		  find("defaultMaxGroupSizeeee") should not be (null)

		When("I check the checkbox, select a default value, and click Save")
		  checkbox("defaultMaxGroupSizeEnabled").select()
		  id("defaultMaxGroupSize").webElement.clear()
		  id("defaultMaxGroupSize").webElement.sendKeys("12")
		  editProperties.submit()

		Then("The page is the groupset summary page")
		  groupsetSummaryPage should be('currentPage)

		When("I navigate to the edit properties page again")
		  groupsetSummaryPage.getGroupsetInfo("xxx101", "Test Lab").get.goToEditProperties

		Then("The checkbox should still be checked")
		  checkbox("defaultMaxGroupSizeEnabled").isSelected should be(true)

		And("The default value should be enabled and should have been saved")
		  id("defaultMaxGroupSize").webElement.isEnabled should be(true)
		  id("defaultMaxGroupSize").webElement.getAttribute("value") should be ("12")

		When("I uncheck the checkbox and click Save")
		  checkbox("defaultMaxGroupSizeEnabled").clear()
		  editProperties.submit()

		Then("The page is the groupset summary page")
		  groupsetSummaryPage should be('currentPage)

		When("I navigate to the edit properties page again")
		  groupsetSummaryPage.getGroupsetInfo("xxx101", "Test Lab").get.goToEditProperties

		Then("The checkbox should be unchecked")
		  checkbox("defaultMaxGroupSizeEnabled").isSelected should be(false)

		And("The default value should remain the same but the field disabled")
		  id("defaultMaxGroupSize").webElement.isEnabled should be(false)
		  id("defaultMaxGroupSize").webElement.getAttribute("value") should be ("12")

	}

}