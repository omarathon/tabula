package uk.ac.warwick.tabula.home

import uk.ac.warwick.tabula.BrowserTest
import org.openqa.selenium.By
import org.scalatest.GivenWhenThen
import uk.ac.warwick.tabula.admin.AdminFixtures

class SysadminDepartmentPermissionsTest extends BrowserTest with SysadminFixtures with GivenWhenThen {

	def withRoleInElement[T](permittedUser: String)(fn: => T) =
		as(P.Sysadmin) {

			def usercodes = findAll(cssSelector(".deptadmin-table .user .muted")).toList.map(_.underlying.getText.trim)

			def normalAdmins = {
				usercodes should contain (P.Admin1.usercode)
				usercodes should contain (P.Admin2.usercode)
			}
			def onlyNormalAdmins = {
				usercodes.size should be (2)
				normalAdmins
			}

			When("I go the department listing")
				click on linkText("List all departments in the system")

			Then("I should see the test department")
				val testDeptLink = find(linkText("Test Services"))
				testDeptLink should not be (None)

			When("I click on the test department")
				click on testDeptLink.get

			Then("I should see a link to admins")
				val adminsLink = find(linkText("View department admins"))
				adminsLink should not be (None)

			When("I click the permissions link")
				click on adminsLink.get

			Then("I should reach the permissions page")
				currentUrl should include("/permissions")

			And("I should see only the two default admin users")
				onlyNormalAdmins

			When("I enter a usercode in the picker")
				click on cssSelector(".deptadmin-table .pickedUser")
				enter(permittedUser)

			Then("I should get a result back")
				val typeahead = cssSelector(".deptadmin-table .typeahead .active a")
				eventuallyAjax {
					find(typeahead) should not be (None)
				}

			And("The picker result should match the entry")
				textField(cssSelector(".deptadmin-table .pickedUser")).value should be (permittedUser)

			When("I pick the matching user")
				click on typeahead

			Then("It should stay in the picker (confirming HTMLUnit hasn't introduced a regression)")
				textField(cssSelector(".deptadmin-table .pickedUser")).value should be (permittedUser)

			And("The usercode should be injected into the form correctly")
				val injected = find(cssSelector(".deptadmin-table .add-permissions [name=usercodes]"))
				injected should not be (None)
				injected.get.underlying.getAttribute("value").trim should be (permittedUser)

			When("I submit the form")
				find(cssSelector(".deptadmin-table form.add-permissions")).get.underlying.submit()

			Then("I should see the old and new entries")
				usercodes.size should be (3)
				normalAdmins
				usercodes should contain (permittedUser)

			When("I remove the new entry")
				val removable = find(cssSelector(s".deptadmin-table .remove-permissions [name=usercodes][value=${permittedUser}]"))
				removable should not be (None)
				removable.get.underlying.submit()

			Then("There should only be the normal admins left")
				onlyNormalAdmins

			fn
		}

	"Sysadmin" should "be able to add departmental admins" in {
		withRoleInElement(P.Marker1.usercode) {
			// Nothing more to do, the with...() tests enough
		}
	}
}