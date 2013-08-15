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

class AllocateStudentsToGroupsPage(implicit val webDriver:WebDriver)extends WebBrowser with BreadcrumbsMatcher {
	def isCurrentPage(moduleName:String){
		breadCrumbsMatch(Seq("Small Group Teaching","Test Services",moduleName.toUpperCase()))
		var heading =find(cssSelector("#main-content h1")).get
		heading.text should startWith ("Allocate students to")
	}

	def findAllUnallocatedStudents =  findAll(cssSelector("div#studentslist ul li"))

	def findFilterCheckboxes(filterAttribute:Option[String], filterValue:Option[String]):Seq[Element]={

		val allCheckboxes = findAll(cssSelector("#filter-controls input")).filter(_.underlying.getAttribute("type")=="checkbox")
		val attrFilter:Element=>Boolean = filterAttribute match {
			case Some(attr)=>element=>element.underlying.getAttribute("data-filter-attr") == "f" + attr.capitalize
			case None=>element=>true
		}
		val valueFilter:Element=>Boolean = filterValue match {
			case Some(value)=>element=>element.underlying.getAttribute("data-filter-value") == value
			case None=>element=>true
		}
		allCheckboxes.filter(attrFilter).filter(valueFilter).toSeq
	}

}
