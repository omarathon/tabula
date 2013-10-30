package uk.ac.warwick.tabula.profiles

import uk.ac.warwick.tabula.BrowserTest
import org.scalatest.GivenWhenThen
import uk.ac.warwick.tabula.profiles.pages.ProfilePage
import uk.ac.warwick.tabula.home.FeaturesDriver

class MemberNotesTest extends BrowserTest with GivenWhenThen with FeaturesDriver {

	"A student" should "be able to view their member notes" in {

		Given("The profilesMemberNotes feature is enabled")
		enableFeature("profilesMemberNotes")

		When("Student1 views their profile")
		signIn as(P.Student1) to (Path("/profiles"))
		val profilePage = new ProfilePage()
		profilePage should be('currentPage)

		val memberNoteSection: Option[Element] = find(cssSelector("#membernote-details"))
		memberNoteSection should be ('defined)

	}

}
