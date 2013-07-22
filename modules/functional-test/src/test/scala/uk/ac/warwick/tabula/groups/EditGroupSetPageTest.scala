package uk.ac.warwick.tabula.groups

import org.scalatest.GivenWhenThen
import org.openqa.selenium.{WebDriver, By}
import uk.ac.warwick.tabula.home.FeaturesDriver._
import uk.ac.warwick.tabula.groups.pages.SmallGroupTeachingPage

class EditGroupSetPageTest  extends SmallGroupsFixture  with GivenWhenThen{

	"Department Admin" should "be able to view the Edit page for an existing group" in {
		Given("I am logged in as admin")
   		signIn as(P.Admin1)  to (Path("/groups"))
		And("I view the small group page for test services")
			val groupsPage = new SmallGroupTeachingPage("xxx")
			go to groupsPage.url

		When("I click on the 'Actions' dropdown for module xxx01/Test Lab")
			val editPage = groupsPage.getGroupsetInfo("xxx101", "Test Lab").goToEditProperties

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
		  val editProperties = groupsetSummaryPage.getGroupsetInfo("xxx101", "Test Lab").goToEditProperties

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
		  groupsetSummaryPage.getGroupsetInfo("xxx101", "Test Lab").goToEditProperties

		Then("The checkboxes should still be checked")
		  checkbox("studentsCanSeeTutorName").value should be("true")
		  checkbox("studentsCanSeeOtherMembers").value should be("true")

	}

}