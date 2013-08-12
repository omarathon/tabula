package uk.ac.warwick.tabula.coursework

import org.scalatest.GivenWhenThen
import uk.ac.warwick.tabula.BrowserTest
import org.openqa.selenium.By

class CourseworkModuleManagerTest extends BrowserTest with CourseworkFixtures with GivenWhenThen {

	def withRoleInElement[T](moduleCode: String, parentElement: String, managers: Seq[String] = Seq(P.ModuleManager1.usercode, P.ModuleManager2.usercode))(fn: => T) =
		as(P.Admin1) {

			def onlyMe() = {
				val users = findAll(cssSelector(s"${parentElement} .user .muted")).toList
				users.size should be (1)
				users.apply(0).underlying.getText should be (currentUser.usercode)
			}

			def nowhereElse() = {
				// doesn't like CSS :not() selector, so have to get all permission-lists and filter out the current one by scala text-mungery
				val allLists = findAll(cssSelector(".permission-list")).toList.filterNot(_.underlying.getAttribute("class").contains(parentElement))
				// then delve further to get the usercodes included
				val userCodes = allLists map (list => list.underlying.findElement(By.cssSelector(".user .muted")).getText.trim)
				userCodes should contain (currentUser.usercode)
				userCodes.exists(u => managers.contains(u)) should be (false)
			}

			When("I go the admin page, and expand the module list")
			click on linkText("Go to the Test Services admin page")
			click on linkText("Show")
			findAll(className("module-info")).size should be (3)

			Then("I should be able to click on the Manage button")
			val info = "fucked"
			val modInfo = findAll(className("module-info")).filter(_.underlying.findElement(By.className("mod-code")).getText == "XXX101").next.underlying
			click on (modInfo.findElement(By.partialLinkText("Manage")))

			And("I should see the permissions menu option")
			val managersLink = modInfo.findElement(By.partialLinkText("Edit module permissions"))
			eventually {
				managersLink.isDisplayed should be (true)
			}

			When("I click the permissions link")
			click on managersLink

			Then("I should reach the permissions page")
			currentUrl should include("/permissions")

			And("I should see myself with the role")
			onlyMe()

			And("I should not see anyone else with any other roles")
			nowhereElse()

			When("I enter a usercode in the tutor picker")
			click on cssSelector(s"${parentElement} .pickedUser")
			enter(managers.head)

			Then("I should get a result back")
			val typeahead = cssSelector(s"${parentElement} .typeahead .active a")
			eventuallyAjax {
				find(typeahead) should not be (None)
			}

			And("The picker result should match the entry")
			textField(cssSelector(s"${parentElement} .pickedUser")).value should be (managers.head)

			When("I pick the matching user")
			click on typeahead

			Then("It should stay in the picker (confirming HTMLUnit hasn't introduced a regression)")
			textField(cssSelector(s"${parentElement} .pickedUser")).value should be (managers.head)

			And("The usercode should be injected into the form correctly")
			({
				val user = cssSelector(s"${parentElement} .add-permissions [name=usercodes]")
				find(user) should not be (None)
				find(user).get.underlying.getAttribute("value").trim should be (managers.head)
			})

			When("I submit the form")
			find(cssSelector(s"${parentElement} form.add-permissions")).get.underlying.submit()

			Then("I should see myself and the new entry")
			({
				val users = findAll(cssSelector(s"${parentElement} .user .muted")).toList
				users.size should be (2)
				val userCodes = users.map(u => u.underlying.getText.trim)
				userCodes should contain (currentUser.usercode)
				userCodes should contain (managers.head)
			})

			And("I should not see anyone else with any other roles")
			nowhereElse()

			When("I add another entry")
			({
				click on cssSelector(s"${parentElement} .pickedUser")
				enter(managers.last)
				val typeahead = cssSelector(s"${parentElement} .typeahead .active a")
				eventuallyAjax {
					find(typeahead) should not be (None)
				}
				click on typeahead
				find(cssSelector(s"${parentElement} form.add-permissions")).get.underlying.submit()
			})

			Then("I should see three entries saved")
			({
				val users = findAll(cssSelector(s"${parentElement} .user .muted")).toList
				users.size should be (3)
			})

			When("I remove the first entry")
			({
				val removable = find(cssSelector(s"${parentElement} .remove-permissions [name=usercodes][value=${managers.head}]"))
				removable should not be (None)
				removable.get.underlying.submit()
			})

			Then("There should be two left")
			({
				val users = findAll(cssSelector(s"${parentElement} .user .muted")).toList
				users.size should be (2)
			})

			When("I remove the second entry")
			({
				val removable = find(cssSelector(s"${parentElement} .remove-permissions [name=usercodes][value=${managers.last}]"))
				removable should not be (None)
				removable.get.underlying.submit()
			})

			Then("There should only be me left")
			onlyMe()

			And("I should not see anyone else with any other roles")
			nowhereElse()

			fn
	}

	"Department admin" should "be able to add and remove module managers" in {
		withRoleInElement("xxx101", "manager-table") {
			// Nothing to do, the with() tests enough
		}
	}

	"Module manager" should "be able to see only modules they can manage" in {
		withRoleInElement("xxx101", "manager-table") {
			as(P.ModuleManager1) {
				// the "show" modules with no assignments link is only visible to dept admins, so can't be clicked
				click on linkText("Go to the Test Services admin page")

				findAll(className("module-info")).size should be (1)

			}
		}
	}

}