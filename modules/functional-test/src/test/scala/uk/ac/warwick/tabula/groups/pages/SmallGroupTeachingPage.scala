package uk.ac.warwick.tabula.groups.pages

import org.openqa.selenium.{WebElement, WebDriver, By}
import uk.ac.warwick.tabula.{FunctionalTestProperties, BreadcrumbsMatcher}
import org.scalatest.selenium.WebBrowser.Page
import org.scalatest.selenium.WebBrowser
import scala.collection.JavaConverters._
import org.scalatest.concurrent.Eventually
import org.scalatest.matchers.ShouldMatchers


class SmallGroupTeachingPage(val departmentCode:String)(implicit val webDriver:WebDriver) extends Page with WebBrowser with	BreadcrumbsMatcher with Eventually with ShouldMatchers   {

	val url: String = FunctionalTestProperties.SiteRoot + "/groups/admin/department/" + departmentCode

	private def getModuleInfo(moduleCode: String):WebElement =
		findAll(className("module-info")).filter(_.underlying.findElement(By.className("mod-code")).getText == moduleCode.toUpperCase).next().underlying

	def isCurrentPage() {
		currentUrl should include ("/groups/admin/department/" + departmentCode)
		find(cssSelector("h1")).get.text should be("Tabula » Small Group Teaching")
	}

	def getGroupsetInfo(moduleCode: String, groupsetName: String) =
		getModuleInfo(moduleCode).  findElements(By.className("item-info")).asScala.filter(_.findElement(By.className("name")).getText.trim == groupsetName).head

	def goToEditPropertiesOf(moduleCode:String, groupSetName:String):EditSmallGroupSetPropertiesPage =  {
		val groupsetElement = getGroupsetInfo(moduleCode,groupSetName)
		groupsetElement.findElement(By.partialLinkText("Actions")).click()
		val editGroupset = groupsetElement.findElement(By.partialLinkText("Edit properties"))
		eventually {
			editGroupset.isDisplayed should be (true)
		}
		editGroupset.click()
		val propsPage = new EditSmallGroupSetPropertiesPage()
		// HACK: the module name is the module code in uppercase in the test data. Should really pass it around separately
		propsPage.isCurrentPage(moduleCode.toUpperCase)
		propsPage
	}
}

