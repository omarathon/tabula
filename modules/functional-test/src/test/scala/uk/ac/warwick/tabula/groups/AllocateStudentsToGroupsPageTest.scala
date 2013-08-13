package uk.ac.warwick.tabula.groups

import org.scalatest.GivenWhenThen
import uk.ac.warwick.tabula.groups.pages.SmallGroupTeachingPage
import org.openqa.selenium.By

class AllocateStudentsToGroupsPageTest extends SmallGroupsFixture with GivenWhenThen {

	val TEST_MODULE_CODE = "xxx987"
	val TEST_GROUPSET_NAME = "Allocation Test Groupset"

	"Department admin" should "Be able to allocate students" in {
		Given("A small group set exists with 1 small group")
		createModule("xxx", TEST_MODULE_CODE, "Allocation Module")
		createRoute("xx123", "xxx", "Allocation Test Route")

		val setId = createSmallGroupSet(TEST_MODULE_CODE, TEST_GROUPSET_NAME)

		And("Five students are a member of the groupset")

		for ((studentId, gender, year) <- Seq(
			(P.Student1.usercode, "F", 1),
			(P.Student2.usercode, "M", 1),
			(P.Student3.usercode, "F", 2),
			(P.Student4.usercode, "M", 2),
			(P.Student5.usercode, "F", 3))) {
			createStudentMember(studentId, gender, "xx123", year)
			addStudentToGroupSet(studentId, setId)
		}

		When("I log in as a departmental administrator and visit the department groups page")
		signIn as (P.Admin1) to (Path("/groups"))

		And(" I view the 'allocate students' page for xxx987/Allocation Test Groupset")
		val groupsetSummaryPage = new SmallGroupTeachingPage("xxx")
		go to groupsetSummaryPage.url
		groupsetSummaryPage.getGroupsetInfo(TEST_MODULE_CODE, TEST_GROUPSET_NAME).get.goToAllocate

		Then("I can see the list of students")
		val students = findAll(cssSelector("div#studentslist ul li"))
		students.hasNext should be(true)

		And("Student1 should have the expected data attributes")
		val student1 = students.filter(
			ele => ele.underlying.findElement(By.cssSelector(".name h6"))
				.getText.startsWith(P.Student1.usercode)
		).next
		student1.attribute("data-gender").get should be("F")
		student1.attribute("data-year").get should be("1")
		student1.attribute("data-route").get should be("xx123")

	}

}
