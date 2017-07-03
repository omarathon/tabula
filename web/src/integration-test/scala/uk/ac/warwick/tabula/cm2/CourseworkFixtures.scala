package uk.ac.warwick.tabula.cm2

import org.openqa.selenium.{By, WebElement}
import org.scalatest.GivenWhenThen
import org.scalatest.exceptions.TestFailedException
import uk.ac.warwick.tabula.data.model.WorkflowCategory
import uk.ac.warwick.tabula.web.{FeaturesDriver, FixturesDriver}
import uk.ac.warwick.tabula.{BrowserTest, FunctionalTestAcademicYear, LoginDetails}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent._
import scala.concurrent.duration.Duration

trait CourseworkFixtures extends BrowserTest with FeaturesDriver with FixturesDriver with GivenWhenThen {

	val TEST_MODULE_CODE = "xxx02"
	val TEST_ROUTE_CODE = "xx456"
	val TEST_DEPARTMENT_CODE = "xxx"
	val TEST_COURSE_CODE = "Ux456"

	before {
		Given("The test department exists")
		go to Path("/fixtures/setup")
		pageSource should include("Fixture setup successful")

		createPremarkedAssignment(TEST_MODULE_CODE)
		createPremarkedCM2Assignment(TEST_MODULE_CODE)

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
			Future {
				createStudentMember(
					P.Student1.usercode,
					routeCode = TEST_ROUTE_CODE,
					courseCode = TEST_COURSE_CODE,
					deptCode = TEST_DEPARTMENT_CODE,
					academicYear = FunctionalTestAcademicYear.current.startYear.toString
				)
			},
			Future {
				createStudentMember(
					P.Student2.usercode,
					routeCode = TEST_ROUTE_CODE,
					courseCode = TEST_COURSE_CODE,
					deptCode = TEST_DEPARTMENT_CODE,
					academicYear = FunctionalTestAcademicYear.current.startYear.toString
				)
			},
			Future {
				createStudentMember(
					P.Student3.usercode,
					routeCode = TEST_ROUTE_CODE,
					courseCode = TEST_COURSE_CODE,
					deptCode = TEST_DEPARTMENT_CODE,
					academicYear = FunctionalTestAcademicYear.current.startYear.toString
				)
			},
			Future {
				createStudentMember(
					P.Student4.usercode,
					routeCode = TEST_ROUTE_CODE,
					courseCode = TEST_COURSE_CODE,
					deptCode = TEST_DEPARTMENT_CODE,
					academicYear = FunctionalTestAcademicYear.current.startYear.toString
				)
			}
		)

		// Wait to complete
		for {job <- concurrentJobs} Await.ready(job, Duration(60, duration.SECONDS))
	}

	/* Runs callback with assignment ID */
	def withAssignment(
		moduleCode: String,
		assignmentName: String,
		settings: Seq[String] => Unit = Nil => (),
		students: Seq[String] = Seq(P.Student1.usercode, P.Student2.usercode))(callback: String => Unit): Unit = as(P.Admin1) {

		click on linkText("Test Services")
		verifyPageLoaded {
			// wait for the page to load
			find(cssSelector("div.deptheader")) should be('defined)
		}

		val module = getModule(moduleCode)
		click on module.findElement(By.partialLinkText("Manage this module"))

		val addAssignment = module.findElement(By.partialLinkText("Create new assignment"))
		eventually {
			addAssignment.isDisplayed should be {
				true
			}
		}
		click on addAssignment

		val prefilledReset = linkText("Don't do this")
		if (prefilledReset.findElement.isDefined) {
			click on prefilledReset
		}

		textField("name").value = assignmentName
		singleSel("workflowCategory").value = WorkflowCategory.NoneUse.code

		cssSelector(s"input[name=createAndAddDetails]").webElement.click()

		// Ensure that we've been redirected back
		withClue(pageSource) {
			currentUrl should endWith("/summary")
		}

	}

	def as[T](user: LoginDetails)(fn: => T): T = {
		currentUser = user
		signIn as user to Path("/cm2")

		fn
	}

	def getModule(moduleCode: String): WebElement = {
		val matchingModule = find(id(s"module-$moduleCode"))
		if (matchingModule.isEmpty)
			throw new TestFailedException(s"No module found for ${moduleCode.toUpperCase}", 0)
		matchingModule.head.underlying
	}

	def openAdminPage(): Unit = {
		When("I go the admin page")
		click on linkText("Test Services")

		Then("I should reach the admin coursework page")
		currentUrl should endWith("/department/xxx")

		//FIXME - Add additional methods based on new functional tests

	}
}