package uk.ac.warwick.tabula.attendance

import uk.ac.warwick.tabula.web.{FeaturesDriver, FixturesDriver}
import uk.ac.warwick.tabula.BrowserTest

class AttendanceFixture extends BrowserTest with FeaturesDriver with FixturesDriver {

	val TEST_DEPARTMENT_CODE="xxx"
	val TEST_UG_ROUTE_CODE="xx123"
	val TEST_PG_ROUTE_CODE="xx234"
	val TEST_UNDERGRAD_COURSE_CODE="Ux123"
	val TEST_POSTGRAD_COURSE_CODE="Px123"

	before {
		go to Path("/scheduling/fixtures/setup")

		createRoute(TEST_UG_ROUTE_CODE, TEST_DEPARTMENT_CODE, "UG Route")
		createRoute(TEST_PG_ROUTE_CODE, TEST_DEPARTMENT_CODE, "PG Route")
		createCourse(TEST_UNDERGRAD_COURSE_CODE,"Test UG Course")
		createCourse(TEST_POSTGRAD_COURSE_CODE,"Test PG Course")
		createStudentMember(P.Student1.usercode,routeCode=TEST_UG_ROUTE_CODE, courseCode=TEST_UNDERGRAD_COURSE_CODE,deptCode = TEST_DEPARTMENT_CODE, yearOfStudy = 1,
			academicYear = "2013")
		createStudentMember(P.Student2.usercode,routeCode=TEST_PG_ROUTE_CODE, courseCode=TEST_POSTGRAD_COURSE_CODE,deptCode = TEST_DEPARTMENT_CODE, yearOfStudy = 1)
		createStudentMember(P.Student3.usercode,routeCode=TEST_UG_ROUTE_CODE, courseCode=TEST_UNDERGRAD_COURSE_CODE,deptCode = TEST_DEPARTMENT_CODE, yearOfStudy = 1,
			academicYear = "2014")
		createStaffMember(P.Marker1.usercode, deptCode = TEST_DEPARTMENT_CODE)
		createMonitoringPointSet(TEST_UG_ROUTE_CODE, 3, "2013", Option(1))

		// new AM
		createAttendanceMonitoringScheme(TEST_DEPARTMENT_CODE, 3, "2014", P.Student3.warwickId)

	}

}
