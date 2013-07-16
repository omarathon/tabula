package uk.ac.warwick.tabula.groups

import org.scalatest.GivenWhenThen
import org.openqa.selenium.By
import uk.ac.warwick.tabula.home.FeaturesDriver._

class EditGroupSetPageTest  extends SmallGroupsFixture with GivenWhenThen{

	"Department Admin" should "be able to view the Edit page for an existing group" in {
		Given("I am logged in as admin and viewing the small group page for test services")
		  as(P.Admin1){
			click on linkText("Go to the Test Services admin page")


		When("I click on the 'Actions' dropdown for module xxx01/Test Lab")
			val groupsetElement = getGroupsetInfo("xxx101", "Test Lab")
			click on (groupsetElement.findElement(By.partialLinkText("Actions")))
			val editGroupset = groupsetElement.findElement(By.partialLinkText("Edit properties"))
			eventually {
				editGroupset.isDisplayed should be (true)
			}

		And("Click on the 'Edit Properties' link ")
			click on(editGroupset)

		Then("The page is the edit properties page")
			var heading =find(cssSelector("#main-content h1")).get
			heading.text should startWith ("Create small groups for")
			}
		}

	"Department Admin" should "be able to view and set information visibility" in {
		Given("The smallGroupTeachingStudentSignUp feature is enabled")
			enableFeature("smallGroupTeachingStudentSignUp")

		And("I am logged in as admin and viewing the edit properties page for xxx101/Test Lab")
			navigateToEditGroupsetPage("xxx101", "Test Lab")

		Then("I should see the options to show/hide tutor name")
			checkbox("studentsCanSeeTutorName") should not be (null)
		  checkbox("studentsCanSeeOtherMembers") should not be (null)

	}

}