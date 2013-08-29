package uk.ac.warwick.tabula.groups

import org.scalatest.GivenWhenThen
import uk.ac.warwick.tabula.groups.pages.SmallGroupTeachingPage

class AllocateStudentsToGroupsPageTest extends SmallGroupsFixture with GivenWhenThen {

	val TEST_MODULE_CODE = "xxx987"
	val TEST_GROUPSET_NAME = "Allocation Test Groupset"

	"Department admin" should "Be able to filter students" in {
		Given("A small group set exists with 1 small group")
		createModule("xxx", TEST_MODULE_CODE, "Allocation Module")
		val setId = createSmallGroupSet(TEST_MODULE_CODE, TEST_GROUPSET_NAME)

		And("Two routes exist")
		createRoute("xx123", "xxx", "Allocation Test Route")
		createRoute("xx456", "xxx", "Allocation Test Route 2")

		And("Five students, of varying gender, year and route, are members of the groupset")

		for ((studentId, gender, year, route) <- Seq(
			(P.Student1.usercode, "F", 1, "xx123"),
			(P.Student2.usercode, "M", 1, "xx123"),
			(P.Student3.usercode, "F", 1, "xx456"),
			(P.Student4.usercode, "M", 2, "xx123"),
			(P.Student5.usercode, "F", 3, "xx123"))) {
			createStudentMember(studentId, gender, route, year)
			addStudentToGroupSet(studentId, setId)
		}

		And("The smallGroupAllocationFiltering feature flag is set")
		enableFeature("smallGroupAllocationFiltering")

		When("I log in as a departmental administrator and visit the department groups page")
		signIn as (P.Admin1) to (Path("/groups"))

		And(" I view the 'allocate students' page for xxx987/Allocation Test Groupset")
		val groupsetSummaryPage = new SmallGroupTeachingPage("xxx")
		go to groupsetSummaryPage.url
		val allocatePage = groupsetSummaryPage.getGroupsetInfo(TEST_MODULE_CODE, TEST_GROUPSET_NAME).get.goToAllocate

		Then("I can see the list of students with all 5 students visible")

		val x = allocatePage.findAllUnallocatedStudents
		println(pageSource)
		allocatePage.findAllUnallocatedStudents.filter(_.underlying.isDisplayed).size should be(5)
		And("I can see the checkboxes to filter by gender, year, and course")

		find(cssSelector("#filter-controls")) should be('defined)
		// the checkboxes are determined by the test data we created when we set up the users
		allocatePage.findFilterCheckboxes(Some("gender"), None).size should be(2) //M,F
		allocatePage.findFilterCheckboxes(Some("year"), None).size should be(3) //1,2,3
		allocatePage.findFilterCheckboxes(Some("route"), None).size should be(2) //xx123,xx456

		When("I uncheck the 'male' checkbox")
		val maleCheckbox = allocatePage.findFilterCheckboxes(Some("gender"), Some("M")).head
		maleCheckbox.underlying.click()

		Then("Only 3 students should be shown")
		val unallocated = allocatePage.findAllUnallocatedStudents.map(e=>(e.underlying,e.underlying.isDisplayed)).toList
		allocatePage.findAllUnallocatedStudents.filter(_.underlying.isDisplayed).size should be(3)

		When("I uncheck the 'year 1' checkbox")
		val year1Checkbox = allocatePage.findFilterCheckboxes(Some("year"), Some("1")).head
		year1Checkbox.underlying.click()

		Then("Only 1 student1 should be shown")
		allocatePage.findAllUnallocatedStudents.filter(_.underlying.isDisplayed).size should be(1)

	}
}
