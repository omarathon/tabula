package uk.ac.warwick.tabula.groups.pages

import org.openqa.selenium.{WebElement, WebDriver, By}
import uk.ac.warwick.tabula.{FunctionalTestProperties, BreadcrumbsMatcher}
import org.scalatest.selenium.WebBrowser.Page
import org.scalatest.selenium.WebBrowser
import scala.collection.JavaConverters._
import org.scalatest.concurrent.Eventually
import org.scalatest.matchers.ShouldMatchers


class SmallGroupTeachingPage(val departmentCode:String)(implicit val webDriver:WebDriver) extends Page with WebBrowser with	BreadcrumbsMatcher with Eventually with ShouldMatchers  with ModuleAndGroupSetList {

	val url = FunctionalTestProperties.SiteRoot + "/groups/admin/department/" + departmentCode

	def isCurrentPage(): Boolean =  {
		currentUrl should include ("/groups/admin/department/" + departmentCode)
		find(cssSelector("h1")).get.text == ("Tabula » Small Group Teaching")
	}

	def getBatchOpenButton() = {
		val manageButton = find(linkText("Manage")).get.underlying
		manageButton.click()
		val manageDropdownContainer = find(cssSelector("div.dept-toolbar")).get.underlying
		val openButton = manageDropdownContainer.findElement(By.partialLinkText("Open"))
		eventually{
			openButton.isDisplayed should be (true)
		}
		openButton
	}
}

class GroupSetInfoSummarySection(val underlying: WebElement, val moduleCode: String)(implicit webDriver: WebDriver) extends Eventually with ShouldMatchers {

	val groupsetId = {
		val classes = underlying.findElement(By.cssSelector("div.item-info")).getAttribute("class").split(" ")
		classes.find(_.startsWith("groupset-")).get.replaceFirst("groupset-","")
	}

	def goToEditProperties:EditSmallGroupSetPropertiesPage =  {
		//val groupsetElement = getGroupsetInfo(moduleCode,groupSetName)
		underlying.findElement(By.partialLinkText("Actions")).click()
		val editGroupset = underlying.findElement(By.partialLinkText("Edit properties"))
		eventually {
			editGroupset.isDisplayed should be (true)
		}
		editGroupset.click()
		val propsPage = new EditSmallGroupSetPropertiesPage()
		// HACK: the module name is the module code in uppercase in the test data. Should really pass it around separately
		propsPage.isCurrentPage(moduleCode.toUpperCase)
		propsPage
	}

	def isShowingOpenButton() = {
		underlying.findElement(By.partialLinkText("Actions")).click()
		!underlying.findElements(By.partialLinkText("Open")).isEmpty
	}

	def getOpenButton() = {
		underlying.findElement(By.partialLinkText("Actions")).click()
		underlying.findElement(By.partialLinkText("Open"))
	}

	def getSignupButton() = {
		underlying.findElement(By.className("sign-up-button"))
	}


	def findLeaveButtonFor(groupName:String) = {
		underlying.findElements(By.tagName("h4")).asScala.filter(e=>e.getText.trim.startsWith(groupName + " ")).headOption.map(

			groupNameHeading=>{
				val parent = groupNameHeading.findElement(By.xpath(".."))
				//groupNameHeading.findElement(By.xpath("../form/input[@type='submit']"))}
				parent.findElement(By.cssSelector("form input.btn"))
			}
		)
	}

	def showsGroup(groupName:String) = {
		underlying.findElements(By.tagName("h4")).asScala.filter(e=>e.getText.trim.startsWith(groupName + " ")).headOption.isDefined
	}

	def findSelectGroupCheckboxFor(groupName:String ) = {
		val groupNameHeading = underlying.findElements(By.tagName("h4")).asScala.filter(e=>e.getText.trim.startsWith(groupName + " ")).head
    // ugh. Might be worth investigating ways of using JQuery selector/traversals in selenium instead of this horror:
		groupNameHeading.findElement(By.xpath("../../div[contains(@class,'span1')]/input"))
	}



}

class BatchOpenPage(val departmentCode: String)(implicit webDriver: WebDriver) extends Page with WebBrowser with Eventually with ShouldMatchers {
	val url= FunctionalTestProperties.SiteRoot + s"/groups/admin/department/${departmentCode}/groups/open"

	def isCurrentPage(): Boolean =  {
		currentUrl should include (s"/groups/admin/department/${departmentCode}/groups/selfsignup/open")
		find(cssSelector("#main-content h1")).get.text.startsWith("Open groups in ")
	}

	def checkboxForGroupSet(groupset: GroupSetInfoSummarySection) = {
		findAll(tagName("input")).filter(_.underlying.getAttribute("value") == groupset.groupsetId).next.underlying
	}

	def submit(){
		findAll(tagName("input")).filter(_.underlying.getAttribute("type") == "submit").next.underlying.click()
	}
}

trait ModuleAndGroupSetList {
	this:WebBrowser =>
	private def getModuleInfo(moduleCode: String)(implicit webdriver:WebDriver ):Option[WebElement] ={
		val moduleInfoElements = findAll(className("module-info")).filter(_.underlying.findElement(By.className("mod-code")).getText == moduleCode.toUpperCase)
		if (moduleInfoElements.isEmpty){
			None
		}	else{
			Some(moduleInfoElements.next().underlying)
		}

	}

	def getGroupsetInfo(moduleCode: String, groupsetName: String)(implicit webdriver:WebDriver ):Option[GroupSetInfoSummarySection] = {
		for( module  <-  getModuleInfo(moduleCode);
		     groupSet <- module.findElements(By.className("item-info")).asScala.find(_.findElement(By.className("name")).getText.trim == groupsetName))
			yield new GroupSetInfoSummarySection(groupSet,moduleCode)
	}



}

