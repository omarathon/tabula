package uk.ac.warwick.tabula.commands

import uk.ac.warwick.tabula.{Fixtures, Mockito, TestBase}
import uk.ac.warwick.tabula.data.model.{StudentCourseDetails, StudentMember}

class MemberCollectionHelperTest extends TestBase with Mockito {

		@Test def testStudentWithNullRoute() {

			val student = Fixtures.student(universityId="0123456")
			val studentWithRoute = Fixtures.student(universityId="87654321")

			val courseDetails = new StudentCourseDetails(student, "student")
			courseDetails.mostSignificant = true
			courseDetails.route = null
			student.mostSignificantCourse = courseDetails

			val route = Fixtures.route("routeCode", "description")
			val courseDetailsWithRoute = new StudentCourseDetails(studentWithRoute, "student")
			courseDetailsWithRoute.mostSignificant = true
			courseDetailsWithRoute.route = route
			studentWithRoute.mostSignificantCourse = courseDetailsWithRoute

			val helper = new MemberCollectionHelper {}
			helper.allMembersRoutesSorted(List(student, studentWithRoute)).size should be (1)
		}

}
