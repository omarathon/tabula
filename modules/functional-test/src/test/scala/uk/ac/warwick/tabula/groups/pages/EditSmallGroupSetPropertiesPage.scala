package uk.ac.warwick.tabula.groups.pages

import org.scalatest.selenium.WebBrowser
import org.openqa.selenium.WebDriver
import uk.ac.warwick.tabula.BreadcrumbsMatcher


class EditSmallGroupSetPropertiesPage (implicit val webDriver:WebDriver) extends WebBrowser with BreadcrumbsMatcher{

	def isCurrentPage(moduleName:String){
		breadCrumbsMatch(Seq("Small Group Teaching","Test Services",moduleName.toUpperCase()))
		var heading =find(cssSelector("#main-content h1")).get
		heading.text should startWith ("Create small groups for")

	}

	def submit(){
		find(cssSelector("input.btn-primary")).get.underlying.click()
	}
}
