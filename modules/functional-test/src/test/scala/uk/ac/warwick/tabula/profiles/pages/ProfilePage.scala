package uk.ac.warwick.tabula.profiles.pages

import org.openqa.selenium.WebDriver
import org.scalatest.selenium.WebBrowser
import uk.ac.warwick.tabula.BreadcrumbsMatcher

class ProfilePage (implicit val webDriver:WebDriver)  extends WebBrowser with	BreadcrumbsMatcher{

	def isCurrentPage():Boolean = {
		// ideally, we'd check the URL looked like /profiles/view/{warwickID}, but unfortunately the
		// logindetails object doesn't include warwickId, so, we'll try and use the breadcrumbs and page title instead
		breadCrumbsMatch(Seq("Student Profiles","Your profile"))
		pageTitle == ("Tabula - Student Profiles - Your profile")
	}

	def timetablePane():Option[TimetablePane] = {
		val timetableSection:Option[Element] = find(cssSelector("#timetable-details"))
		timetableSection.map(e=>new TimetablePane(e.underlying))
	}
}

