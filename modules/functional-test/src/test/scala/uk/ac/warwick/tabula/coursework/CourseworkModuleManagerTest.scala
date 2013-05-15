package uk.ac.warwick.tabula.coursework

import scala.collection.JavaConverters._
import org.scalatest.BeforeAndAfter
import uk.ac.warwick.tabula.BrowserTest
import org.scalatest.BeforeAndAfterAll
import org.openqa.selenium.By
import org.openqa.selenium.remote.server.handler.AcceptAlert

class CourseworkModuleManagerTest extends BrowserTest with CourseworkFixtures {
	
	def withModuleManagers[T](moduleCode: String, managers: Seq[String] = Seq(P.ModuleManager1.usercode, P.ModuleManager2.usercode))(fn: => T) = 
		as(P.Admin1) {
			click on linkText("Go to the Test Services admin page")
			click on linkText("Show")
			
			findAll(className("module-info")).size should be (3)
			
			// Add a module manager for moduleCode
			val info = findAll(className("module-info")).filter(_.underlying.findElement(By.className("mod-code")).getText == "XXX101").next.underlying
			
			click on (info.findElement(By.partialLinkText("Manage")))
			val managersLink = info.findElement(By.partialLinkText("Edit module permissions"))
			eventually {
				managersLink.isDisplayed should be (true)
			}
			click on (managersLink)
			
			textField("usercodes").value = P.ModuleManager1.usercode
			submit()		
	
			textField("usercodes").value = P.ModuleManager2.usercode
			submit()
			
			fn
		}

	"Department admin" should "be able to add module managers" in {
		withModuleManagers("xxx101") {
			// Nothing to do, the with() tests enough
		}
	}
	
	"Department admin" should "be able to remove module managers" in {
		withModuleManagers("xxx101") {
			// With ends on the manage page, so we can go straight into removing it again
			
			// Remove module manager 2
			className("permission-list").webElement.findElements(By.tagName("tr")).size should be (2)
			
			val row = className("permission-list").webElement.findElements(By.tagName("tr")).asScala.find({ _.findElement(By.tagName("td")).getText == "(" + P.ModuleManager2.usercode + ")"  })
			click on (row.get.findElement(By.className("btn")))
			
			// The HtmlUnit driver doesn't support Javascript alerts, so this just works
			className("permission-list").webElement.findElements(By.tagName("tr")).size should be (1)
		}
	}
	
	"Module manager" should "be able to see only modules they can manage" in {
		withModuleManagers("xxx101") { as(P.ModuleManager1) {
			// the "show" modules with no assignments link is only visible to dept admins, so can't be clicked
			click on linkText("Go to the Test Services admin page")
			
			findAll(className("module-info")).size should be (1)
			
		} }
	}
	
}