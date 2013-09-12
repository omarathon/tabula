package uk.ac.warwick.tabula

import org.openqa.selenium.{WebDriver, By}
import org.scalatest.selenium.WebBrowser
import org.scalatest.matchers.ShouldMatchers

trait BreadcrumbsMatcher extends ShouldMatchers {

	this:WebBrowser=>

	def breadCrumbsMatch(crumbsToMatch:Seq[String])(implicit webDriver:WebDriver){
		val crumbs = findAll(cssSelector("ul#primary-navigation li")).toSeq
		crumbs.size should be (crumbsToMatch.size)
		val crumbText = crumbs.map(e=>e.underlying.findElement(By.tagName("a")).getText)
		val x = crumbText.mkString(",")
		crumbText should be (crumbsToMatch)
	}
}
