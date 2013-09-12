package uk.ac.warwick.tabula.groups.pages

import org.openqa.selenium.WebDriver
import org.scalatest.selenium.WebBrowser
import uk.ac.warwick.tabula.FunctionalTestProperties
import org.scalatest.concurrent.Eventually
import org.scalatest.matchers.ShouldMatchers

class GroupsHomePage (implicit val webDriver:WebDriver) extends WebBrowser with Eventually with ShouldMatchers with ModuleAndGroupSetList{
	val url = FunctionalTestProperties.SiteRoot + "/groups/"

	def isCurrentPage(): Boolean =  {
		currentUrl should include("/groups/")
		find(cssSelector("h1")).get.text == ("Tabula » Small Group Teaching")
	}



}
