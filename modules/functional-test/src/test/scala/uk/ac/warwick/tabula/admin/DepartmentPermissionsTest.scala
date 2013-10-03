package uk.ac.warwick.tabula.admin

import uk.ac.warwick.tabula.BrowserTest
import org.openqa.selenium.By
import org.scalatest.GivenWhenThen

// n.b. this test doesn't work with the FirefoxDriver  because the UI pops up modal dialogs which the
// test isn't expecting. HTMLUnit ignores them.
class DepartmentPermissionsTest extends BrowserTest with AdminFixtures with GivenWhenThen {

	def withRoleInElement[T](permittedUser: String, parentElement: String)(fn: => T) =
		as(P.Admin1) {

			def usercodes = findAll(cssSelector(s"${parentElement} .user .muted")).toList.map(_.underlying.getText.trim)

			def onlyMe() = {
				usercodes.size should be (1)
				usercodes.apply(0) should be (P.Admin1.usercode)
			}

			def nowhereElse() = {
				// doesn't like CSS :not() selector, so have to get all permission-lists and filter out the current one by scala text-mungery
				val allLists = findAll(cssSelector(".permission-list")).toList.filterNot(_.underlying.getAttribute("class").contains(parentElement.replace(".","")))
				// then delve further to get the usercodes included
				val filteredUsercodes = allLists map (list => list.underlying.findElement(By.cssSelector(".user .muted")).getText.trim)
				filteredUsercodes should contain (P.Admin1.usercode)
				filteredUsercodes should not contain (permittedUser)
			}

			When("I go the admin page")
				click on linkText("Go to the Test Services admin page")

			Then("I should be able to click on the Manage button")
				val toolbar = findAll(className("dept-toolbar")).next.underlying
				click on (toolbar.findElement(By.partialLinkText("Manage")))

			And("I should see the permissions menu option")
				val managersLink = toolbar.findElement(By.partialLinkText("Edit departmental permissions"))
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
				enter(permittedUser)

			Then("I should get a result back")
				val typeahead = cssSelector(s"${parentElement} .typeahead .active a")
				eventuallyAjax {
					find(typeahead) should not be (None)
				}

			And("The picker result should match the entry")
				textField(cssSelector(s"${parentElement} .pickedUser")).value should be (permittedUser)

			When("I pick the matching user")
				click on typeahead

			Then("It should stay in the picker (confirming HTMLUnit hasn't introduced a regression)")
				textField(cssSelector(s"${parentElement} .pickedUser")).value should be (permittedUser)

			And("The usercode should be injected into the form correctly")
				val injected = find(cssSelector(s"${parentElement} .add-permissions [name=usercodes]"))
				injected should not be (None)
				injected.get.underlying.getAttribute("value").trim should be (permittedUser)

			When("I submit the form")
				find(cssSelector(s"${parentElement} form.add-permissions")).get.underlying.submit()

			Then("I should see myself and the new entry")
				({
					usercodes.size should be (2)
					usercodes should contain (P.Admin1.usercode)
					usercodes should contain (permittedUser)
				})

			And("I should not see anyone else with any other roles")
				nowhereElse()

			When("I remove the new entry")
				val removable = find(cssSelector(s"${parentElement} .remove-permissions [name=usercodes][value=${permittedUser}]"))
				removable should not be (None)
				removable.get.underlying.submit()

			Then("There should only be me left")
				onlyMe()

			And("I should not see anyone else with any other roles")
				nowhereElse()

			fn
		}

	"Department admin" should "be able to add and remove senior tutors" in {
		withRoleInElement(P.Marker1.usercode, ".tutor-table") {
			// Nothing more to do, the with...() tests enough
		}
	}

	"Department admin" should "be able to add and remove senior supervisors" in {
		withRoleInElement(P.Marker1.usercode, ".supervisor-table") {
			// Nothing more to do, the with...() tests enough
		}
	}

	"Random module manager" should "not be able to add senior supervisors or tutors" in {
		as(P.ModuleManager1) {
			// no link
			findAll(linkText("Go to the Test Services admin page")).size should be (0)

			// no direct access
			go to (Path("/admin/department/xxx/permissions"))
			pageSource.contains(s"Sorry ${P.ModuleManager1.usercode}, you don't have permission to see that.") should be (true)
		}

	}
}