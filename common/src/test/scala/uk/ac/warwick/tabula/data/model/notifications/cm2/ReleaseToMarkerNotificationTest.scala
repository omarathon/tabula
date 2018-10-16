package uk.ac.warwick.tabula.data.model.notifications.cm2

import java.io.{ByteArrayOutputStream, OutputStreamWriter}

import org.junit.Before
import uk.ac.warwick.tabula.data.model._
import uk.ac.warwick.tabula.data.model.markingworkflow.MarkingWorkflowStage.{DblFinalMarker, DblFirstMarker, DblSecondMarker}
import uk.ac.warwick.tabula.{Fixtures, Mockito, TestBase}
import uk.ac.warwick.userlookup.User

class ReleaseToMarkerNotificationRenderingTest extends TestBase with Mockito {

	var dept: Department = _

	var assignment: Assignment = _

	var feedback: AssignmentFeedback = _
	val marker1: User = Fixtures.user("9999991", "9999991")
	val marker2: User = Fixtures.user("9999992", "9999992")

	var markerFeedback: MarkerFeedback = _


	@Before
	def prepare(): Unit = {
		dept = Fixtures.department("in")

		assignment = Fixtures.assignment("demo")

		feedback = Fixtures.assignmentFeedback("9999991", "9999992")
		feedback.assignment = assignment
		markerFeedback = Fixtures.markerFeedback(feedback)
	}

	@Test
	def rendersWhenNotCollectSubmissions(): Unit = {
		assignment.collectSubmissions = false
		val output = new ByteArrayOutputStream
		val writer = new OutputStreamWriter(output)
		val configuration = newFreemarkerConfiguration()
		val template = configuration.getTemplate(ReleaseToMarkerNotification.templateLocation)
		template.process(ReleaseToMarkerNotification.renderNoCollectingSubmissions(
			assignment = assignment,
			numReleasedFeedbacks = 10,
			workflowVerb = "The_Verb"
		).model, writer)
		writer.flush()
		val renderedResult = output.toString
		renderedResult should be(
			"""
				|
				|Note:
				|- 10 students are allocated to you for marking
				|- This assignment does not require students to submit work to Tabula
				|""".stripMargin)

	}

	@Test
	def renderWhenCollectingSubmissions(): Unit = {
		assignment.collectSubmissions = true
		val output = new ByteArrayOutputStream
		val writer = new OutputStreamWriter(output)
		val configuration = newFreemarkerConfiguration()
		val template = configuration.getTemplate(ReleaseToMarkerNotification.templateLocation)
		template.process(ReleaseToMarkerNotification.renderCollectSubmissions(
			assignment = assignment,
			numAllocated = 6,
			studentsAtStagesCount = Seq(
				StudentAtStagesCount(DblFirstMarker.description, 2),
				StudentAtStagesCount(DblSecondMarker.description, 1),
				StudentAtStagesCount(DblFinalMarker.description, 3)
			),
			numReleasedFeedbacks = 12,
			numReleasedSubmissionsFeedbacks = 13,
			numReleasedNoSubmissionsFeedbacks = 14,
			workflowVerb = "the_verb"
		).model, writer)
		writer.flush()
		val renderedResult = output.toString
		renderedResult should be(
			"""
				|
				|Note:
				|- 6 students are allocated to you for marking
				| -- First marker: 2 students
				| -- Second marker: 1 student
				| -- Final marker: 3 students
				|- 12 students allocated to you have been released for marking
				| -- 13 students have submitted work
				| -- 14 students have not submitted work
				|""".stripMargin)
	}
}
