package uk.ac.warwick.tabula.admin

import scala.collection.JavaConverters._
import uk.ac.warwick.tabula.BrowserTest
import org.openqa.selenium.By
import uk.ac.warwick.tabula.coursework.CourseworkFixtures

class DepartmentPermissionsTest extends BrowserTest with AdminFixtures {

	def withSeniorTutors[T](tutors: Seq[String] = Seq(P.Marker1.usercode, P.Marker1.usercode))(fn: => T) =
		as(P.Admin1) {
			System.out.println(pageSource)

			click on linkText("Go to the Test Services admin page")

			val toolbar = findAll(className("dept-toolbar")).next.underlying
			click on (toolbar.findElement(By.partialLinkText("Manage")))
			val managersLink = toolbar.findElement(By.partialLinkText("Edit departmental permissions"))
			eventually {
				managersLink.isDisplayed should be (true)
			}
			click on (managersLink)

			val tutorForm = findAll(className("tutor-form")).next.underlying
			val usercodes = tutorForm.findElement(By.name("usercodes")) // textField("usercodes")
			usercodes.sendKeys(P.Marker1.usercode)
			usercodes.submit

			usercodes.sendKeys(P.Marker2.usercode)
			usercodes.submit

			fn
		}

	"Department admin" should "be able to add senior tutors" in {
		withSeniorTutors() {
			// Nothing to do, the with() tests enough
		}
	}

//	"Department admin" should "be able to remove module managers" in {
//		withModuleManagers("xxx101") {
//			// With ends on the manage page, so we can go straight into removing it again
//
//			// Remove module manager 2
//			className("permission-list").webElement.findElements(By.tagName("tr")).size should be (2)
//
//			val row = className("permission-list").webElement.findElements(By.tagName("tr")).asScala.find({ _.findElement(By.className("user-id")).getText == "(" + P.ModuleManager2.usercode + ")"  })
//			click on (row.get.findElement(By.className("btn")))
//
//			// The HtmlUnit driver doesn't support Javascript alerts, so this just works
//			className("permission-list").webElement.findElements(By.tagName("tr")).size should be (1)
//		}
//	}
//
//	"Module manager" should "be able to see only modules they can manage" in {
//		withModuleManagers("xxx101") { as(P.ModuleManager1) {
//			// the "show" modules with no assignments link is only visible to dept admins, so can't be clicked
//			click on linkText("Go to the Test Services admin page")
//
//			findAll(className("module-info")).size should be (1)
//
//		} }
//	}

}