package uk.ac.warwick.tabula.groups.pages

import org.scalatest.selenium.WebBrowser
import org.openqa.selenium.WebDriver
import uk.ac.warwick.tabula.BreadcrumbsMatcher
import org.openqa.selenium.support.ui.Select


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

class AllocateStudentsToGroupsPage(implicit val webDriver:WebDriver)extends WebBrowser with BreadcrumbsMatcher {
	def isCurrentPage(moduleName:String){
		breadCrumbsMatch(Seq("Small Group Teaching","Test Services",moduleName.toUpperCase()))
		var heading =find(cssSelector("#main-content h1")).get
		heading.text should startWith ("Allocate students to")
	}

	def findAllUnallocatedStudents =  {
		val s= findAll(cssSelector("div.student-list ul li"))
		s
	}

	// use native selenium select, because the scalatest SingleSel doesn't allow enumerating its values
	def findFilterDropdown(filterAttribute:String):Option[Select]={
		val filterDropdowns = findAll(cssSelector("div.filter select"))
		filterDropdowns.find(_.underlying.getAttribute("data-filter-attr") == filterAttribute).map(e=>new Select(e.underlying))
	}

}
