package uk.ac.warwick.tabula.groups

import uk.ac.warwick.tabula.{LoginDetails, BrowserTest}
import org.openqa.selenium.By
import scala.collection.JavaConverters._

trait SmallGroupsFixture extends BrowserTest{

  before{
    go to (Path("/scheduling/fixtures/setup"))
  }

  def as[T](user: LoginDetails)(fn: =>T) = {
    signIn as(user) to (Path("/groups"))
    fn
  }

	def getModuleInfo(moduleCode: String) =
		findAll(className("module-info")).filter(_.underlying.findElement(By.className("mod-code")).getText == moduleCode.toUpperCase).next.underlying

	def getGroupsetInfo(moduleCode: String, groupsetName: String) =
		getModuleInfo(moduleCode).findElements(By.className("item-info")).asScala.filter(_.findElement(By.className("name")).getText.trim == groupsetName).head

	def navigateToEditGroupsetPage(moduleName:String, groupSetName:String)={
		as(P.Admin1){
			click on linkText("Go to the Test Services admin page")
			val groupsetElement = getGroupsetInfo(moduleName,groupSetName)
			click on (groupsetElement.findElement(By.partialLinkText("Actions")))
			val editGroupset = groupsetElement.findElement(By.partialLinkText("Edit properties"))
			eventually {
				editGroupset.isDisplayed should be (true)
			}
			click on(editGroupset)

			val content = find(cssSelector("#main-content")).get.underlying
			val heading =content.findElement(By.tagName("h1"))
			heading.getText should startWith ("Create small groups for")
			content
		}}
}
