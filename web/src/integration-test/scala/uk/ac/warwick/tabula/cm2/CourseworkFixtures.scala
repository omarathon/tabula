package uk.ac.warwick.tabula.cm2

import org.scalatest.GivenWhenThen
import uk.ac.warwick.tabula.web.{FeaturesDriver, FixturesDriver}
import uk.ac.warwick.tabula.{BrowserTest, FunctionalTestAcademicYear, LoginDetails}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent._
import scala.concurrent.duration.Duration

trait CourseworkFixtures extends BrowserTest with FeaturesDriver with FixturesDriver with GivenWhenThen {

	val TEST_MODULE_CODE = "xxx02"
	val TEST_ROUTE_CODE="xx456"
	val TEST_DEPARTMENT_CODE="xxx"
	val TEST_COURSE_CODE="Ux456"

	before {
		Given("The test department exists")
		go to Path("/fixtures/setup")
		pageSource should include("Fixture setup successful")

		createPremarkedAssignment(TEST_MODULE_CODE)

		val assessmentFuture = Future {
			And("There is an assessment component for module xxx01")
			createAssessmentComponent("XXX", "XXX01-16", "Cool essay")

			And("There is an upstream assessment group for xxx01 with students1-4 in it")
			createUpstreamAssessmentGroup("XXX01-16", Seq(P.Student1.warwickId, P.Student2.warwickId, P.Student3.warwickId, P.Student4.warwickId))
		}

		And("student1-4 are members")
		createRoute(TEST_ROUTE_CODE, TEST_DEPARTMENT_CODE, "Test Route")
		createCourse(TEST_COURSE_CODE, "Test Course")
		createStudentMember(
			P.Student1.usercode,
			routeCode = TEST_ROUTE_CODE,
			courseCode = TEST_COURSE_CODE,
			deptCode = TEST_DEPARTMENT_CODE,
			academicYear = FunctionalTestAcademicYear.current.startYear.toString
		)
		// Make them at the same time.
		val concurrentJobs = Seq(
			assessmentFuture,
			Future { createStudentMember(
				P.Student1.usercode,
				routeCode = TEST_ROUTE_CODE,
				courseCode = TEST_COURSE_CODE,
				deptCode = TEST_DEPARTMENT_CODE,
				academicYear = FunctionalTestAcademicYear.current.startYear.toString
			) },
			Future { createStudentMember(
				P.Student2.usercode,
				routeCode = TEST_ROUTE_CODE,
				courseCode = TEST_COURSE_CODE,
				deptCode = TEST_DEPARTMENT_CODE,
				academicYear = FunctionalTestAcademicYear.current.startYear.toString
			) },
			Future { createStudentMember(
				P.Student3.usercode,
				routeCode = TEST_ROUTE_CODE,
				courseCode = TEST_COURSE_CODE,
				deptCode = TEST_DEPARTMENT_CODE,
				academicYear = FunctionalTestAcademicYear.current.startYear.toString
			) },
			Future { createStudentMember(
				P.Student4.usercode,
				routeCode = TEST_ROUTE_CODE,
				courseCode = TEST_COURSE_CODE,
				deptCode = TEST_DEPARTMENT_CODE,
				academicYear = FunctionalTestAcademicYear.current.startYear.toString
			) }
		)

		// Wait to complete
		for { job <- concurrentJobs } Await.ready(job, Duration(60, duration.SECONDS))
	}


	def as[T](user: LoginDetails)(fn: => T): T = {
		currentUser = user
		signIn as user to Path("/cm2")

		fn
	}

//FIXME - Add additional methods based on new functional tests

}