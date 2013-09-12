package uk.ac.warwick.tabula.profiles.pages

import org.openqa.selenium.{By, WebDriver, WebElement}

class TimetablePane(val containerSection:WebElement)(implicit driver:WebDriver) {

	def isShowingCalendar:Boolean = {
		val contentDivs = containerSection.findElements(By.cssSelector("div.fc-content"))
		contentDivs.size ==1
	}
}
