package uk.ac.warwick.tabula.groups

import uk.ac.warwick.tabula.{LoginDetails, BrowserTest}
import uk.ac.warwick.tabula.home.FixturesDriver

trait SmallGroupsFixture extends BrowserTest with FixturesDriver {

  before{
    go to (Path("/scheduling/fixtures/setup"))
  }

  def as[T](user: LoginDetails)(fn: =>T) = {
		currentUser = user
    signIn as(user) to (Path("/groups"))
    fn
  }



/*	def navigateToEditGroupsetPage(moduleCode:String, groupSetName:String)={
		as(P.Admin1){
			click on linkText("Go to the Test Services admin page")
			breadCrumbsMatch(Seq("Small Group Teaching"))
			goToEditPropertiesOf(moduleCode, groupSetName)

			val content = find(cssSelector("#main-content")).get.underlying
			val heading =content.findElement(By.tagName("h1"))
			heading.getText should startWith ("Create small groups for")
			content
		}}*/
}
