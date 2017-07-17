package uk.ac.warwick.tabula.cm2

import org.openqa.selenium.support.ui.Select
import org.openqa.selenium.{By, WebElement}
import org.scalatest.GivenWhenThen
import org.scalatest.exceptions.TestFailedException
import uk.ac.warwick.tabula.data.model.WorkflowCategory
import uk.ac.warwick.tabula.data.model.markingworkflow.MarkingWorkflowType
import uk.ac.warwick.tabula.web.{FeaturesDriver, FixturesDriver}
import uk.ac.warwick.tabula.{BrowserTest, FunctionalTestAcademicYear, LoginDetails}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent._
import scala.concurrent.duration.Duration
import scala.collection.JavaConverters._

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

	def addModuleManagers(
		moduleCode: String,
		managers: Seq[String] = Seq(),
		assistants: Seq[String] = Seq()): Unit = as(P.Admin1) {
		click on linkText("Test Services")
		verifyPageLoaded {
			// wait for the page to load
			find(cssSelector("div.deptheader")) should be('defined)
		}
		if ((assistants ++ managers).nonEmpty) {
			val module = getModule(moduleCode)
			click on module.findElement(By.partialLinkText("Manage this module"))
			val editPerms = module.findElement(By.partialLinkText("Module permissions"))
			eventually(editPerms.isDisplayed should be {
				true
			})
			click on editPerms

			def pick(table: String, usercodes: Seq[String]) {
				verifyPageLoaded {
					find(cssSelector(s"$table .pickedUser")) should be('defined)
				}
				usercodes.foreach { u =>
					click on cssSelector(s"$table .pickedUser")
					enter(u)
					val typeahead = cssSelector(".typeahead .active a")
					eventuallyAjax {
						find(typeahead) should not be None
					}
					click on typeahead
					find(cssSelector(s"$table form.add-permissions")).get.underlying.submit()
				}
			}
			pick(".modulemanager-table", managers)
			pick(".moduleassistant-table", assistants)
		}
	}

	/* Runs callback with assignment ID */
	def withAssignment(
		moduleCode: String,
		assignmentName: String,
		studentSettings: Seq[String] => Unit = Nil => (),
		submissionSettings: () => Unit = () => (),
		students: Seq[String] = Seq(P.Student1.usercode, P.Student2.usercode),
		loggedUser: LoginDetails = P.Admin1)(callback: String => Unit): Unit = as(loggedUser) {

		click on linkText("Test Services")
		verifyPageLoaded {
			// wait for the page to load
			find(cssSelector("div.deptheader")) should be('defined)
		}

		val module = getModule(moduleCode)
		click on module.findElement(By.partialLinkText("Manage this module"))

		val addAssignment = module.findElement(By.partialLinkText("Create new assignment"))
		eventually(addAssignment.isDisplayed should be {true})
		click on addAssignment

		val prefilledReset = linkText("Don't do this")
		if (prefilledReset.findElement.isDefined) {
			click on prefilledReset
		}

		textField("name").value = assignmentName
		singleSel("workflowCategory").value = WorkflowCategory.NoneUse.code

		cssSelector(s"input[name=createAndAddFeedback]").webElement.click()
		eventually(pageSource contains "Edit feedback settings" should be {true})
		click on linkText("Students").findElement.get.underlying

		eventually { textArea("massAddUsers").isDisplayed should be {true} }
		if (students.nonEmpty) {
			textArea("massAddUsers").value = students.mkString("\n")
			click on className("add-students-manually")
			eventuallyAjax(pageSource contains "Your changes will not be recorded until you save this assignment." should be {true})
			eventuallyAjax{pageSource should include(students.size + " manually enrolled")}
		}
		studentSettings(students)

		cssSelector(s"input[value='Save and continue']").webElement.click()
		submissionSettings()
		cssSelector(s"input[name=createAndAddSubmissions]").webElement.click()

		// Ensure that we've been redirected back
		withClue(pageSource) {
			currentUrl should endWith("/summary")
		}

		val pattern = """.*admin/assignments/(.*)/summary""".r
		val assignmentId = pattern.findAllIn(currentUrl).matchData.toSeq.headOption.map(_.group(1))
		assignmentId.isDefined should be { true }
		callback(assignmentId.get)
	}

	def withAssignmentWithWorkflow(workflowType: MarkingWorkflowType, markers: Seq[LoginDetails]*)(callback: String => Unit): Unit = {
		withAssignment("xxx01", s"${workflowType.description} - single use") { id =>

			When("I click on the edit button")
			click on partialLinkText("Edit assignment")
			Then("I see the edit details screen")
			eventually(pageSource contains "Edit assignment details" should be {true})

			When("I choose Single use")
			singleSel("workflowCategory").value = WorkflowCategory.SingleUse.code
			Then("I see the options for single use workflows")
			eventuallyAjax(pageSource contains "Marking workflow type" should be {true})

			When("I select single marking add a markers usercode")
			singleSel("workflowType").value = workflowType.name
			eventuallyAjax(pageSource contains "Add marker" should be {true})

			Seq("markersA" -> markers.headOption.getOrElse(Nil), "markersB" -> markers.tail.headOption.getOrElse(Nil)).foreach{case (field, m) =>
				m.zipWithIndex.foreach{ case(marker, i) =>
					new TextField(findAll(cssSelector(s".$field input.flexi-picker")).toList.apply(i).underlying).value = marker.usercode
					click on className(field).webElement.findElement(By.cssSelector("button.btn"))
					eventually {
						findAll(cssSelector(s".$field input.flexi-picker")).toList.count(_.isDisplayed) should be (i+2)
					}
				}
			}

			And("I submit the form")
			cssSelector(s"input[name=editAndEditDetails]").webElement.click()
			Then("I should be back on the summary page")
			withClue(pageSource) {
				currentUrl should endWith("/summary")
			}

			callback(id)
		}
	}

	def as[T](user: LoginDetails)(fn: => T): T = {
		currentUser = user
		signIn as user to Path("/coursework")

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
		eventually {
			currentUrl should endWith("/department/xxx")
		}

	}

	def openMarkingWorkflowSettings(): Unit = {
		When("I click on the Marking workflows link")
		click on linkText("Marking workflows")
		Then("I should reach the marking workflow page")
		eventually {
			currentUrl should include("/markingworkflows")
		}
	}

	def createMarkingWorkflow(workflowName: String, workflowType: MarkingWorkflowType, markers: Seq[LoginDetails]*): String = as(P.Admin1) {
		openAdminPage()
		openMarkingWorkflowSettings()

		When("I click create")
		click on partialLinkText("Create")
		Then("I should reach the create marking workflow page")
		currentUrl should include("/add")

		And("I enter the necessary data")
		textField("workflowName").value = workflowName
		singleSel("workflowType").value = workflowType.name
		var markerA = markers.headOption.getOrElse(Nil)
		Seq("markersA" -> markers.headOption.getOrElse(Nil), "markersB" -> markers.tail.headOption.getOrElse(Nil)).foreach{case (field, m) =>
			m.zipWithIndex.foreach{ case(marker, i) =>
				new TextField(findAll(cssSelector(s".$field input.flexi-picker")).toList.apply(i).underlying).value = marker.usercode
				click on className(field).webElement.findElement(By.cssSelector("button.btn"))
				eventually {
					findAll(cssSelector(s".$field input.flexi-picker")).toList.count(_.isDisplayed) should be (i+2)
				}
			}
		}
		And("I submit the form")
		submit()
		Then("I should be redirected back to the marking workflow page")
		eventually{
			currentUrl should endWith ("/markingworkflows")
		}
		And("Row with that workflow should be there")
		val tbody = className("table").webElement.findElement(By.tagName("tbody"))
		val row = tbody.findElements(By.tagName("tr")).asScala.find({
			_.findElement(By.tagName("td")).getText == workflowName
		})
		row should be('defined)
		var link = row.get.findElement(By.partialLinkText("Modify"))
		val url =  link.getAttribute("href")
		val pattern = """.*markingworkflows/(.*)/edit""".r
		val workflowId = pattern.findAllIn(url).matchData.toSeq.headOption.map(_.group(1))
		workflowId.get
	}


	def addMarkingWorkflow(workflowName: String, workflowType: MarkingWorkflowType)(callback: String => Unit): Unit = as(P.Admin1) {
		callback(createMarkingWorkflow(workflowName, workflowType, Seq(P.Marker1, P.Marker2), Seq(P.Marker3)))
	}

	def editAssignment(workflowId: String): Unit = {
		When("I click on the edit button")
		click on partialLinkText("Edit assignment")
		Then("I see the edit details screen")
		eventually(pageSource contains "Edit assignment details" should be {true})

		When("I choose Reusable workflow")
		singleSel("workflowCategory").value = WorkflowCategory.Reusable.code
		Then("I see the reusable workflow list")
		eventuallyAjax(pageSource contains "Marking workflow type" should be {true})

		When("I select reusable workflow")
		val select = new Select(find(cssSelector("select[name=reusableWorkflow]")).get.underlying)
		select.selectByValue(workflowId)
		And("I submit the form")
		cssSelector(s"input[name=editAndEditDetails]").webElement.click()
		Then("I should be back on the summary page")
		withClue(pageSource) {
			currentUrl should endWith("/summary")
		}
	}

	def submitAssignment(user: LoginDetails, moduleCode: String, assignmentName: String, assignmentId: String, file: String, mustBeEnrolled: Boolean = true): Unit = as(user) {
		if (mustBeEnrolled) {
			linkText(assignmentName).findElement should be ('defined)
			click on linkText(assignmentName)
			currentUrl should endWith(assignmentId)
		} else {
			// Just go straight to the submission URL
			go to Path(s"/coursework/submission/$assignmentId")
		}

		click on find(cssSelector("input[type=file]")).get
		pressKeys(getClass.getResource(file).getFile)

		checkbox("plagiarismDeclaration").select()

		submit()
		verifyPageLoaded(
			pageSource contains "Thanks, we've received your submission." should be {true}
		)
	}

	def getInputByLabel(label: String): Option[WebElement] =
		findAll(tagName("label")).find(_.underlying.getText.trim == label) map { _.underlying.getAttribute("for") } map { id(_).webElement }
}