package uk.ac.warwick.tabula.coursework.pages

import scala.collection.JavaConverters._
import org.openqa.selenium.{Keys, By, WebDriver}
import org.scalatest.selenium.WebBrowser
import org.scalatest.matchers.{Matcher, ShouldMatchers}
import uk.ac.warwick.tabula.{FunctionalTestAcademicYear, FunctionalTestProperties}
import org.scalatest.selenium.WebBrowser.Element
import org.scalatest.concurrent.{IntegrationPatience, Eventually}
import org.joda.time.DateTime


class SetupAssignmentsPage(val departmentCode: String)(implicit driver: WebDriver) extends WebBrowser with ShouldMatchers with Eventually with IntegrationPatience {
	val thisYear = new FunctionalTestAcademicYear(new DateTime().getYear)
	val url = s"${FunctionalTestProperties.SiteRoot}/coursework/admin/department/$departmentCode/setup-assignments?academicYear=${thisYear}"

	def shouldBeCurrentPage() {
		currentUrl should include(s"/coursework/admin/department/$departmentCode/setup-assignments")
		cssSelector("#main-content h1").element.text should be ("Setup assignments")
	}

	def itemRows = cssSelector("tr.itemContainer").findAllElements.toSeq

	def getCheckboxForRow(row: Element) = {
		row.underlying.findElement(By.cssSelector("input[type=checkbox]"))
	}

	def getOptionIdForRow(row: Element): Option[String] = {
		row.underlying.findElements(By.cssSelector(".options-id-label .label")).asScala.headOption.map { _.getText() }
	}

	def setTitleForRow(row: Element, title: String) {
		row.underlying.findElement(By.cssSelector("a.name-edit-link")).click()
		val textField = row.underlying.findElement(By.cssSelector(".editable-inline input[type=text]"))
		textField.clear()
		textField.sendKeys(title)
		textField.sendKeys(Keys.ENTER)

		val span = row.underlying.findElement(By.cssSelector(".editable-name"))
		eventually { span.getText should be (title) }
	}

	def itemCount = itemRows.size

	def clickNext() = clickButtonWithAction("options")
	def clickBack() = clickButtonWithAction("refresh-select")
	def clickSubmit() = clickButtonWithAction("submit")

	// All the navigation buttons have an "action" data attribute.
	private def clickButtonWithAction(action: String) {
		cssSelector(s"button[data-action=${action}]").webElement.click()
	}

}
