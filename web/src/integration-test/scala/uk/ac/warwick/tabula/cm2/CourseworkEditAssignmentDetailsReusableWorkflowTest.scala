package uk.ac.warwick.tabula.cm2

import org.openqa.selenium.support.ui.Select
import org.openqa.selenium.{By, WebElement}
import org.scalatest.GivenWhenThen
import uk.ac.warwick.tabula.data.model.WorkflowCategory
import uk.ac.warwick.tabula.data.model.markingworkflow.MarkingWorkflowType
import uk.ac.warwick.tabula.data.model.markingworkflow.MarkingWorkflowType.{DoubleMarking, ModeratedMarking}
import uk.ac.warwick.tabula.{BrowserTest, LoginDetails}

import scala.collection.JavaConverters._
import scala.collection.mutable


class CourseworkEditAssignmentDetailsReusableWorkflowTest extends BrowserTest with CourseworkFixtures with GivenWhenThen {

	"Department admin" should "be able to edit reusable Double marker workflow and change to Moderated" in {
		var moderatedWorkflowName = "Moderated Marking Test"
		var doubleMarkingWorkflowName = "Double Marking Test"
		var studentList = Seq("tabula-functest-student1", "tabula-functest-student2", "tabula-functest-student3")
		var modifiedAssignmentTitle = "Double reusable marker workflow assignment modified to Moderated"

		var doubleWorkflowId = createMarkingWorkflow(doubleMarkingWorkflowName, DoubleMarking, Seq(P.Marker1, P.Marker2), Seq(P.Marker3))
		var moderatedworkflowId = createMarkingWorkflow(moderatedWorkflowName, ModeratedMarking, Seq(P.Marker1, P.Marker2), Seq(P.Marker3))

		withAssignment("xxx02", "Double marking") { assignmentId =>
			editAssignment(doubleWorkflowId)
			var checkboxFeedbackFieldDetails: Seq[(String, Boolean)] = Seq(("automaticallyReleaseToMarkers", false), ("collectMarks", true), ("dissertation", true))

			amendAssignmentDetails(modifiedAssignmentTitle,moderatedworkflowId)
			amendAssignmentFeedback(checkboxFeedbackFieldDetails)
			assigmentStudentDetails(studentList)

			assigmentMarkerDetails(studentList.size, ModeratedMarking)

			var checkboxSubmissionFieldDetails: Seq[(String, Boolean)] = Seq(("collectSubmissions", true), ("automaticallySubmitToTurnitin", false), ("allowLateSubmissions", false))
			var radioButtonSubmissionFieldDetails: Seq[(String, String)] = Seq(("restrictSubmissions", "true"))
			assignmentSubmissionDetails(checkboxSubmissionFieldDetails, radioButtonSubmissionFieldDetails)

			var textFieldOptionFieldDetails: Seq[(String, String)] = Seq(("individualFileSizeLimit", "4"), ("wordCountMin", "200"), ("wordCountMax", "600"))
			var textAreaFieldOptionFieldDetails: Seq[(String, String)] = Seq(("wordCountConventions", "Exclude any bibliography"), ("assignmentComment", "Important assignment"))
			var singleSelOptionFieldDetails: Seq[(String, String)] = Seq(("minimumFileAttachmentLimit", "1"), ("fileAttachmentLimit", "3"))

			var fileTypes = Seq("pdf", "doc")
			assigmentOptions(textFieldOptionFieldDetails, textAreaFieldOptionFieldDetails, singleSelOptionFieldDetails, fileTypes)

			var allFields: Map[String, String] = (checkboxSubmissionFieldDetails.map { case (field, value) => field -> value.toString }
				++ textFieldOptionFieldDetails
				++ textAreaFieldOptionFieldDetails
				++ singleSelOptionFieldDetails
				++ checkboxFeedbackFieldDetails.map { case (field, value) => field -> value.toString }
				++ Seq(("title", modifiedAssignmentTitle), ("workflowName", moderatedWorkflowName), ("workflowType", "Reusable"))
				++ Seq(("fileTypes",fileTypes.mkString(", ").toUpperCase))
				++ Seq(("studentList",studentList.size.toString))
				).toMap

			assigmentReview(Some(ModeratedMarking), allFields)
		}

	}


	"Department admin" should "be able to edit  reusable moderated marker workflow and change to double" in {
		var moderatedWorkflowName = "Moderated Marking Test"
		var doubleMarkingWorkflowName = "Double Marking Test"
		var studentList = Seq("tabula-functest-student1", "tabula-functest-student2")
		var modifiedAssignmentTitle = "Moderated reusable marker workflow assignment modified to Double"

		var doubleWorkflowId = createMarkingWorkflow(doubleMarkingWorkflowName, DoubleMarking, Seq(P.Marker1, P.Marker2), Seq(P.Marker3))
		var moderatedworkflowId = createMarkingWorkflow(moderatedWorkflowName, ModeratedMarking, Seq(P.Marker1), Seq(P.Marker2, P.Marker3))

		withAssignment("xxx02", "Moderated Assignment") { assignmentId =>
			editAssignment(moderatedworkflowId)
			var checkboxFeedbackFieldDetails: Seq[(String, Boolean)] = Seq(("automaticallyReleaseToMarkers", true), ("collectMarks", false), ("dissertation", false))

			amendAssignmentDetails(modifiedAssignmentTitle, doubleWorkflowId)
			amendAssignmentFeedback(checkboxFeedbackFieldDetails)
			assigmentStudentDetails(studentList)

			assigmentMarkerDetails(studentList.size, DoubleMarking)

			var checkboxSubmissionFieldDetails: Seq[(String, Boolean)] = Seq(("collectSubmissions", false), ("automaticallySubmitToTurnitin", false), ("allowLateSubmissions", false))
			var radioButtonSubmissionFieldDetails: Seq[(String, String)] = Seq(("restrictSubmissions", "false"))
			assignmentSubmissionDetails(checkboxSubmissionFieldDetails, radioButtonSubmissionFieldDetails)

			var textFieldOptionFieldDetails: Seq[(String, String)] = Seq(("individualFileSizeLimit", "2"), ("wordCountMin", "100"), ("wordCountMax", "500"))
			var textAreaFieldOptionFieldDetails: Seq[(String, String)] = Seq(("wordCountConventions", "Exclude any bibliography"), ("assignmentComment", "Important assignment XX"))
			var singleSelOptionFieldDetails: Seq[(String, String)] = Seq(("minimumFileAttachmentLimit", "1"), ("fileAttachmentLimit", "2"))

			var fileTypes = Seq("pdf", "txt")
			assigmentOptions(textFieldOptionFieldDetails, textAreaFieldOptionFieldDetails, singleSelOptionFieldDetails, fileTypes)

			var allFields: Map[String, String] = (checkboxSubmissionFieldDetails.map { case (field, value) => field -> value.toString }
				++ textFieldOptionFieldDetails
				++ textAreaFieldOptionFieldDetails
				++ singleSelOptionFieldDetails
				++ checkboxFeedbackFieldDetails.map { case (field, value) => field -> value.toString }
				++ Seq(("title", modifiedAssignmentTitle), ("workflowName", moderatedWorkflowName), ("workflowType", "Reusable"))
				++ Seq(("fileTypes",fileTypes.mkString(", ").toUpperCase))
				++ Seq(("studentList",studentList.size.toString))
				).toMap

			assigmentReview(Some(DoubleMarking), allFields)
		}

	}


	"Department admin" should "be able to edit reusable moderated marker workflow and change to workflowless" in {
		var moderatedWorkflowName = "Moderated Marking Test"
	//	var doubleMarkingWorkflowName = "Double Marking Test"
		var studentList = Seq("tabula-functest-student1", "tabula-functest-student2")
		var modifiedAssignmentTitle = "Moderated reusable marker workflow assignment modified to Double"

	//	var doubleWorkflowId = createMarkingWorkflow(doubleMarkingWorkflowName, DoubleMarking, Seq(P.Marker1, P.Marker2), Seq(P.Marker3))
		var moderatedworkflowId = createMarkingWorkflow(moderatedWorkflowName, ModeratedMarking, Seq(P.Marker1), Seq(P.Marker2, P.Marker3))

		withAssignment("xxx02", "Reusable Double marking") { assignmentId =>
			editAssignment(moderatedworkflowId)
			var checkboxFeedbackFieldDetails: Seq[(String, Boolean)] = Seq(("automaticallyReleaseToMarkers", true), ("collectMarks", false), ("dissertation", false))

			amendAssignmentDetails(modifiedAssignmentTitle, "")
			amendAssignmentFeedback(checkboxFeedbackFieldDetails)
			assigmentStudentDetails(studentList)

//			assigmentMarkerDetails(studentList.size, DoubleMarking)

			var checkboxSubmissionFieldDetails: Seq[(String, Boolean)] = Seq(("collectSubmissions", false), ("automaticallySubmitToTurnitin", false), ("allowLateSubmissions", false))
			var radioButtonSubmissionFieldDetails: Seq[(String, String)] = Seq(("restrictSubmissions", "false"))
			assignmentSubmissionDetails(checkboxSubmissionFieldDetails, radioButtonSubmissionFieldDetails)

			var textFieldOptionFieldDetails: Seq[(String, String)] = Seq(("individualFileSizeLimit", "2"), ("wordCountMin", "100"), ("wordCountMax", "500"))
			var textAreaFieldOptionFieldDetails: Seq[(String, String)] = Seq(("wordCountConventions", "Exclude any bibliography"), ("assignmentComment", "Important assignment XX"))
			var singleSelOptionFieldDetails: Seq[(String, String)] = Seq(("minimumFileAttachmentLimit", "1"), ("fileAttachmentLimit", "2"))

			var fileTypes = Seq("pdf", "txt")
			assigmentOptions(textFieldOptionFieldDetails, textAreaFieldOptionFieldDetails, singleSelOptionFieldDetails, fileTypes)

			var allFields: Map[String, String] = (checkboxSubmissionFieldDetails.map { case (field, value) => field -> value.toString }
				++ textFieldOptionFieldDetails
				++ textAreaFieldOptionFieldDetails
				++ singleSelOptionFieldDetails
				++ checkboxFeedbackFieldDetails.map { case (field, value) => field -> value.toString }
				++ Seq(("title", modifiedAssignmentTitle), ("workflowName", moderatedWorkflowName), ("workflowType", "Reusable"))
				++ Seq(("fileTypes",fileTypes.mkString(", ").toUpperCase))
				++ Seq(("studentList",studentList.size.toString))
				).toMap

			assigmentReview(None, allFields)
		}

	}

	def submitAndContinueClick(): Unit = {
		Then("I click submit button")
		val button = webDriver.findElement(By.id("command")).findElement(By.cssSelector("input[value='Save and continue']"))
		button.click()
	}

	def amendAssignmentDetails(newTitle: String, newWorkflowId: String): Unit = {
		When("I click on the edit button again")
		click on partialLinkText("Edit assignment")
		Then("I see the edit details screen")
		eventually(pageSource contains "Edit assignment details" should be {
			true
		})
		Then("I change assignment Title")
		textField("name").value = newTitle

		if (newWorkflowId.length > 0) {
			And("I change  workflow type")
			val select = new Select(find(cssSelector("select[name=reusableWorkflow]")).get.underlying)
			select.selectByValue(newWorkflowId)
		} else {
			singleSel("workflowCategory").value = WorkflowCategory.NoneUse.code
		}
		submitAndContinueClick()

	}

	def amendAssignmentFeedback(checkboxFieldDetails: Seq[(String, Boolean)]): Unit = {
		When("I go to feedback assignemnt page")
		currentUrl should include("/feedback")
		checkboxFieldDetails.foreach { case (fieldName, checked) =>
			And(s"I amend checkbox ${fieldName} on feedback details form")
			if (checked) {
				checkbox(fieldName).select()
			} else {
				checkbox(fieldName).clear()
			}
		}
		submitAndContinueClick()
	}

	def assigmentStudentDetails(users: Seq[String]): Unit = {
		When("I go to student  assignment students page")
		eventually {
			currentUrl should include("/students")
		}

		And("I add some manual students")
		textArea("massAddUsers").value = users.mkString(" ")
		And("I click Add manual button ")
		val form = webDriver.findElement(By.id("command"))
		val manualAdd = webDriver.findElement(By.id("command")).findElement(By.className("add-students-manually"))
		manualAdd.click()

		Then("I should see manually enrolled students")
		eventually {
			val enrolledCount = form.findElement(By.className("enrolledCount"))
			val enrolledCountText = enrolledCount.getText()
			enrolledCountText should include(s"${users.size} manually enrolled")
		}
		submitAndContinueClick()
	}

	def assignmentSubmissionDetails(checkboxFieldDetails: Seq[(String, Boolean)], radioButtonFieldDetails: Seq[(String, String)]): Unit = {
		When("I go to assignment submissions assignemnt page")
		currentUrl should include("/submissions")
		checkboxFieldDetails.foreach { case (fieldName, checked) =>
			And(s"I amend checkbox ${fieldName} on submission details form to ${checked}")
			if (checked) {
				checkbox(fieldName).select()
			} else {
				checkbox(fieldName).clear()
			}
		}
		radioButtonFieldDetails.foreach { case (fieldName, fieldValue) =>
			And(s"I amend radio button ${fieldName} on submission details form to ${fieldValue}")
			radioButtonGroup(fieldName).value = fieldValue
		}
		submitAndContinueClick()
	}

	def assigmentOptions(textFieldDetails: Seq[(String, String)], textAreaFieldDetails: Seq[(String, String)], singleSelFieldDetails: Seq[(String, String)], fileExtList: Seq[String]): Unit = {
		When("I go to assignment options page")
		currentUrl should include("/options")

		singleSelFieldDetails.foreach { case (fieldName, fieldValue) =>
			And(s"I amend  ${fieldName} on options details form to ${fieldValue}")
			singleSel(fieldName).value = fieldValue
		}
		if (fileExtList.nonEmpty) {
			And("I enter data in file extension field")
			var fieldExt = webDriver.findElement(By.id("fileExtensionList")).findElement(By.cssSelector("input.text"))
			click on fieldExt
			enter(fileExtList.mkString(" "))
		}


		And("Enter some more data")
		textFieldDetails.foreach { case (fieldName, fieldValue) =>
			And(s"I amend  ${fieldName} on options details form to ${fieldValue}")
			textField(fieldName).value = fieldValue
		}
		textAreaFieldDetails.foreach { case (fieldName, fieldValue) =>
			And(s"I amend  ${fieldName} on options details form to ${fieldValue}")
			textArea(fieldName).value = fieldValue
		}
		submitAndContinueClick()
	}

	def assigmentMarkerDetails(studentCount: Int, workflowType: MarkingWorkflowType): Unit = {
		val form = webDriver.findElement(By.id("command"))
		workflowType match {
			case ModeratedMarking => {
				checkUnallocatedStudents(studentCount, Seq("markerStudentsList", "moderatorStudentsList"))
				checkMarkerStudents(studentCount, Seq("markerMarkerList", "moderatorMarkerList"))
			}
			case DoubleMarking => {
				checkUnallocatedStudents(studentCount, Seq("markerStudentsList", "secondmarkerStudentsList"))
				checkMarkerStudents(studentCount, Seq("markerMarkerList", "secondmarkerMarkerList"))
			}
			case _ => {
				checkUnallocatedStudents(studentCount, Seq("markerStudentsList"))
				checkMarkerStudents(studentCount, Seq("markerMarkerList"))
			}

		}
		submitAndContinueClick()
	}

	def checkUnallocatedStudents(studentCount: Int, studentListId: Seq[String]): Unit = {
		When("I go to marker assignemnt page")
		currentUrl should include("/markers")
		val form = webDriver.findElement(By.id("command"))
		And("I check unallocated student list")

		studentListId.foreach { id =>
			val studentUnallocatedList = form.findElement(By.id(id))
			studentUnallocatedList.findElements(By.cssSelector("div.student-list li.student")).size() should be(studentCount)
		}
		And("I randomly allocate students")
		var allocate = form.findElements(By.partialLinkText("Randomly allocate"))
		allocate.asScala.foreach(_.click())

		Then("Unallocated student list becomes 0")
		studentListId.foreach { id =>
			val studentUnallocatedList = form.findElement(By.id(id))
			studentUnallocatedList.findElements(By.cssSelector("div.student-list li.student")).size() should be(0)
		}

	}

	def checkMarkerStudents(studentCount: Int, markerId: Seq[String]): Unit = {
		val form = webDriver.findElement(By.id("command"))
		markerId.foreach { id =>
			val markerAllocatedList = form.findElement(By.id(id))
			val markers = markerAllocatedList.findElements(By.cssSelector("div.drag-target"))
			val allocatedStudents = markers.asScala.map { webElement =>
				var test = webElement.findElement(By.className("drag-count"))
				webElement.findElement(By.className("drag-count")).getText.toInt
			}.sum
			allocatedStudents should be(studentCount)
		}
	}


	def checkReviewTabRow(labels: mutable.Buffer[WebElement], labelRow: String, fieldValue: String) = {
		var element = labels.find(_.getText.contains(labelRow)).getOrElse(fail(s"{labelRow} not found"))
		var parent = element.findElement(By.xpath(".."))
		parent.getText should include(fieldValue)
	}

	def getFieldValue(fieldName: String, fieldDetails: Map[String, String]): String = {
		var value = fieldDetails.get(fieldName).getOrElse("")
		value match {
			case "false" => "No"
			case "true" => "Yes"
			case _ =>  value
		}
	}

	def assigmentReview(workflowType: Option[MarkingWorkflowType], fieldDetails: Map[String, String]): Unit = {
		When("I go to assignment review page")
		currentUrl should include("/review")

		Then("I cross check various assignment details")
		//assignment page details
		var labels = webDriver.findElements(By.className("review-label")).asScala
		checkReviewTabRow(labels, "Assignment title", getFieldValue("title",fieldDetails))
		checkReviewTabRow(labels, "Marking workflow use", getFieldValue("workflowType",fieldDetails))
		checkReviewTabRow(labels, "Marking workflow name", getFieldValue("workflow",fieldDetails))
		workflowType match {
			case Some(wf) =>  {
				checkReviewTabRow(labels, "Marking workflow type", workflowType.get.name)
				//marker page details
				checkReviewTabRow(labels, "Marker", "tabula-functest-marker1 user (2 students)")
			}
			case None =>
		}
		//assignment feedback page details
		checkReviewTabRow(labels, "Automatically release to markers when assignment closes or after plagiarism check", getFieldValue("automaticallyReleaseToMarkers",fieldDetails))
		checkReviewTabRow(labels, "Collect marks",  getFieldValue("collectMarks",fieldDetails))

		//students page
		checkReviewTabRow(labels, "Total number of students enrolled", getFieldValue("studentList",fieldDetails))


		//submissions page details
		checkReviewTabRow(labels, "Collect submissions", getFieldValue("collectSubmissions",fieldDetails))
		checkReviewTabRow(labels, "Automatically check submissions for plagiarism", getFieldValue("automaticallySubmitToTurnitin",fieldDetails))
	//	checkReviewTabRow(labels, "Submission scope", "Only students enrolled on this assignment can submit coursework")
		checkReviewTabRow(labels, "Allow new submissions after close date", getFieldValue("allowLateSubmissions",fieldDetails))

		//Options page details
		checkReviewTabRow(labels, "Minimum attachments per submission", getFieldValue("minimumFileAttachmentLimit",fieldDetails))
		checkReviewTabRow(labels, "Maximum attachments per submission", getFieldValue("fileAttachmentLimit",fieldDetails))
		checkReviewTabRow(labels, "Accepted attachment file types", getFieldValue("fileTypes",fieldDetails))
		checkReviewTabRow(labels, "Maximum file size", getFieldValue("individualFileSizeLimit",fieldDetails))
		checkReviewTabRow(labels, "Minimum word count", getFieldValue("wordCountMin",fieldDetails))
		checkReviewTabRow(labels, "Maximum word count", getFieldValue("wordCountMax",fieldDetails))
		checkReviewTabRow(labels, "Word count conventions", getFieldValue("wordCountConventions",fieldDetails))
	}

}
