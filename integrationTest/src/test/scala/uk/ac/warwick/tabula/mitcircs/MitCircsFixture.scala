package uk.ac.warwick.tabula.mitcircs

import org.openqa.selenium.By
import org.scalatest.GivenWhenThen
import uk.ac.warwick.tabula.web.FixturesDriver
import uk.ac.warwick.tabula.{AcademicYear, BrowserTest, LoginDetails}

trait MitCircsFixture extends FixturesDriver with GivenWhenThen {
  self: BrowserTest =>

  val TEST_ROUTE_CODE = "xx123"
  val TEST_DEPARTMENT_CODE = "xxx"
  val TEST_COURSE_CODE = "Ux123"

  def setupTestData(): Unit = {
    Given("The test department exists")
    go to Path("/fixtures/setup")
    pageSource should include("Fixture setup successful")

    And("student1 has a membership record")
    createRoute(TEST_ROUTE_CODE, TEST_DEPARTMENT_CODE, "Test Route")
    createCourse(TEST_COURSE_CODE, "Test Course")
    createStudentMember(
      P.Student1.usercode,
      routeCode = TEST_ROUTE_CODE,
      courseCode = TEST_COURSE_CODE,
      deptCode = TEST_DEPARTMENT_CODE,
      academicYear = AcademicYear.now().startYear.toString
    )

    And("There is an assessment component for module xxx01")
    createAssessmentComponent("XXX", "XXX01-16", "Cool essay")

    And("There is an upstream assessment group for xxx01 with students1-4 in it")
    createUpstreamAssessmentGroup("XXX01-16", Seq(P.Student1.warwickId, P.Student2.warwickId, P.Student3.warwickId, P.Student4.warwickId))

    And("There is a module registration for xxx01")
    registerStudentsOnModule(Seq(P.Student1, P.Student2, P.Student3, P.Student4), "xxx01", Some(AcademicYear.now().startYear.toString))
  }

  def enableMitCircsAndSetUpMCO(): Unit = as(P.Admin1) {
    // Enable mit circs on the department with some basic guidance
    openDepartmentSettings()

    When("I enable mit circs and provide guidance")
    click on checkbox("enableMitCircs")
    textArea("mitCircsGuidance").value = "Please be truthful in everything you do"
    submit()

    Then("I should be redirected back to the admin page")
    currentUrl should endWith ("/department/xxx")

    // Make extman1 an MCO
    openDepartmentPermissionsPage()

    Then("There should be no existing MCOs")
    findAll(cssSelector(".mco-table .row .very-subtle")).toSeq should be (empty)

    When("I enter a usercode in the picker")
    click on cssSelector(".mco-table .pickedUser")
    enter(P.ExtensionManager1.usercode)

    Then("I should get a result back")
    val typeahead = cssSelector(".typeahead .active a")
    eventually {
      find(typeahead) should not be None
    }

    When("I pick the matching user")
    click on typeahead

    When("I submit the form")
    find(cssSelector(".mco-table form.add-permissions")).get.underlying.submit()

    Then("The permissions should be saved")
    findAll(cssSelector(".mco-table .row .very-subtle")).toSeq.size should be (1)
  }

  private def openAdminPage(): Unit = {
    When("I go the admin page")
    go to Path("/admin") // Will redirect to single department admin page

    // Just in case we weren't redirected
    if (!currentUrl.contains("/department/xxx")) {
      click on partialLinkText("Test Services")
    }
  }

  def openDepartmentSettings(): Unit = {
    openAdminPage()

    Then("I should be able to click on the Manage dropdown")
    val toolbar = findAll(className("dept-toolbar")).next().underlying
    click on toolbar.findElement(By.partialLinkText("Manage"))

    And("I should see the department settings menu option")
    val departmentLink = toolbar.findElement(By.partialLinkText("Department settings"))
    eventually {
      departmentLink.isDisplayed should be (true)
    }

    When("I click the department setting link")
    click on departmentLink

    Then("I should reach the department settings page")
    currentUrl should include("/display")
  }

  private def openDepartmentPermissionsPage(): Unit = {
    openAdminPage()

    Then("I should be able to click on the Manage dropdown")
    val toolbar = findAll(className("dept-toolbar")).next().underlying
    click on toolbar.findElement(By.partialLinkText("Manage"))

    And("I should see the edit departmental permissions menu option")
    val permissionsLink = toolbar.findElement(By.partialLinkText("Edit departmental permissions"))
    eventually {
      permissionsLink.isDisplayed should be (true)
    }

    When("I click the edit departmental permissions link")
    click on permissionsLink

    Then("I should reach the department permissions page")
    currentUrl should include("/permissions")
  }

  def as[A](user: LoginDetails)(fn: => A): A = {
    currentUser = user
    signIn as user to Path("/")
    fn
  }
}
