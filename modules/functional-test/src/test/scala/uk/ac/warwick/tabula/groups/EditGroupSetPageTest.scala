package uk.ac.warwick.tabula.groups

import org.scalatest.GivenWhenThen
import uk.ac.warwick.tabula.groups.pages.SmallGroupTeachingPage

class EditGroupSetPageTest  extends SmallGroupsFixture  with GivenWhenThen{

	"Department Admin" should "be able to view the Edit page for an existing group" in {
		Given("I am logged in as admin")
			signIn as(P.Admin1)  to (Path("/groups/"))
		And("I view the small group page for test services")
			val groupsPage = new SmallGroupTeachingPage("xxx")
			go to groupsPage.url

		When("I click on the 'Actions' dropdown for module xxx01/Test Lab")
			val editPage = groupsPage.getGroupsetInfo("xxx01", "Test Lab").get.goToEditProperties

		Then("The page is the edit properties page")
			editPage.isCurrentPage("xxx01")

	}

	"Department Admin" should "be able to view and set information visibility options" in {
		val groupsetSummaryPage = new SmallGroupTeachingPage("xxx")

		Given("The smallGroupTeachingStudentSignUp feature is enabled")
			enableFeature("smallGroupTeachingStudentSignUp")

		When("I log in as admin")
			signIn as(P.Admin1)  to (Path("/groups/"))

		And(" I view the edit properties page for xxx01/Test Lab")
			go to groupsetSummaryPage.url
			val editProperties = groupsetSummaryPage.getGroupsetInfo("xxx01", "Test Lab").get.goToEditProperties

		Then("I should see the options to show/hide tutor name")
			checkbox("studentsCanSeeTutorName") should not be (null)
			checkbox("studentsCanSeeOtherMembers") should not be (null)

		When("I check the checkboxes and click Save")
			checkbox("studentsCanSeeTutorName").select()
			checkbox("studentsCanSeeOtherMembers").select()
			editProperties.submitAndExit()

		Then("The page is the groupset summary page")
			groupsetSummaryPage should be('currentPage)

		When("I navigate to the edit properties page again")
			groupsetSummaryPage.getGroupsetInfo("xxx01", "Test Lab").get.goToEditProperties

		Then("The checkboxes should still be checked")
			checkbox("studentsCanSeeTutorName").value should be("true")
			checkbox("studentsCanSeeOtherMembers").value should be("true")

	}

	"Department Admin" should "be able to set a default maximum group size" in {
		val groupsetSummaryPage = new SmallGroupTeachingPage("xxx")

		When("I log in as admin")
			signIn as(P.Admin1)  to (Path("/groups/"))

		And("I view the edit groups page for xxx01/Test Lab")
			go to groupsetSummaryPage.url
			val editGroups = groupsetSummaryPage.getGroupsetInfo("xxx01", "Test Lab").get.goToEditGroups

		Then("I should see the options to set a default maximum group size")
			radioButton("defaultMaxGroupSizeEnabled") should not be (null)
			find("defaultMaxGroupSizeeee") should not be (null)

		When("I set group size to be limited, select a default value, and click Save")
				radioButtonGroup("defaultMaxGroupSizeEnabled").value = "true"
				id("defaultMaxGroupSize").webElement.clear()
				id("defaultMaxGroupSize").webElement.sendKeys("12")
				editGroups.submitAndExit()

		Then("The page is the groupset summary page")
			groupsetSummaryPage should be('currentPage)

		When("I navigate to the edit groups page again")
		  groupsetSummaryPage.getGroupsetInfo("xxx01", "Test Lab").get.goToEditGroups

		Then("Group size should still be limited")
			radioButtonGroup("defaultMaxGroupSizeEnabled").value should be("true")

		And("The default value should be enabled and should have been saved")
			id("defaultMaxGroupSize").webElement.isEnabled should be(true)
			id("defaultMaxGroupSize").webElement.getAttribute("value") should be ("12")

		When("I set group size to be unlimited")
			radioButtonGroup("defaultMaxGroupSizeEnabled").value = "false"
			editGroups.submitAndExit()

		Then("The page is the groupset summary page")
			groupsetSummaryPage should be('currentPage)

		When("I navigate to the edit groups page again")
			groupsetSummaryPage.getGroupsetInfo("xxx01", "Test Lab").get.goToEditGroups

		Then("Group size should be unlimited")
		radioButtonGroup("defaultMaxGroupSizeEnabled").value should be("false")

		And("The default value should remain the same but the field disabled")
			id("defaultMaxGroupSize").webElement.isEnabled should be(false)
			id("defaultMaxGroupSize").webElement.getAttribute("value") should be ("12")

	}

}