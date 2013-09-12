package uk.ac.warwick.tabula.profiles

import org.scalatest.selenium.WebBrowser.go
import uk.ac.warwick.tabula.BrowserTest
import uk.ac.warwick.tabula.home.FeaturesDriver
import org.scalatest.GivenWhenThen


trait TimetablingFixture extends BrowserTest with TimetableDriver  with FeaturesDriver with GivenWhenThen{

	val TEST_MODULE_CODE = "xxx654"
	val TEST_GROUPSET_NAME = "Timetable Test Groupset"
  val TEST_ROUTE_CODE="xx123"
	val TEST_DEPARTMENT_CODE="xxx"
	val TEST_COURSE_CODE="Ux123"

	var testGroupSetId:String=_

	before{
		Given("The test department exists")
		go to (Path("/scheduling/fixtures/setup"))

		And("the personal timetables feature is enabled")
		enableFeature("personalTimetables")

		And("student1 has a membership record")
		createRoute(TEST_ROUTE_CODE, TEST_DEPARTMENT_CODE, "TimetableTest Route")
		createCourse(TEST_COURSE_CODE,"TimetableTest Course")
		createStudentMember(P.Student1.usercode,routeCode=TEST_ROUTE_CODE, courseCode=TEST_COURSE_CODE,deptCode = TEST_DEPARTMENT_CODE)

		And("a module exists with a related SmallGroupSet")
		createModule(TEST_DEPARTMENT_CODE, TEST_MODULE_CODE, "Timetabling Module")
		testGroupSetId = createSmallGroupSet(TEST_MODULE_CODE, TEST_GROUPSET_NAME)

	}

}
