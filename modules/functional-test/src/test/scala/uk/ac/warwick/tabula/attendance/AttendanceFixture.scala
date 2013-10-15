package uk.ac.warwick.tabula.attendance

import uk.ac.warwick.tabula.home.{FeaturesDriver, FixturesDriver}
import uk.ac.warwick.tabula.BrowserTest

class AttendanceFixture extends BrowserTest with FeaturesDriver with FixturesDriver {

	val TEST_UG_ROUTE_CODE="xx123"
	val TEST_DEPARTMENT_CODE="xxx"
	val TEST_UNDERGRAD_COURSE_CODE="Ux123"

	before {
		go to (Path("/scheduling/fixtures/setup"))

		createRoute(TEST_UG_ROUTE_CODE, TEST_DEPARTMENT_CODE, "UG Route")
		createCourse(TEST_UNDERGRAD_COURSE_CODE,"Test UG Course")
		createStudentMember(P.Student1.usercode,routeCode=TEST_UG_ROUTE_CODE, courseCode=TEST_UNDERGRAD_COURSE_CODE,deptCode = TEST_DEPARTMENT_CODE, yearOfStudy = 1)
	}

}
