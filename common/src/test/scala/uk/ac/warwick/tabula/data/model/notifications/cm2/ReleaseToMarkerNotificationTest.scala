package uk.ac.warwick.tabula.data.model.notifications.cm2

import java.io.{ByteArrayOutputStream, OutputStreamWriter}

import org.junit.Before
import uk.ac.warwick.tabula.data.model._
import uk.ac.warwick.tabula.data.model.markingworkflow.MarkingWorkflowStage.{DblFinalMarker, DblFirstMarker, DblSecondMarker}
import uk.ac.warwick.tabula.services.CM2MarkingWorkflowService
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
		assignment.collectSubmissions = false

		feedback = Fixtures.assignmentFeedback("9999991", "9999992")
		feedback.assignment = assignment
		markerFeedback = Fixtures.markerFeedback(feedback)
	}

	@Test
	def rendersWhenNotCollectSubmissions(): Unit = {

		val output = new ByteArrayOutputStream
		val writer = new OutputStreamWriter(output)
		val configuration = newFreemarkerConfiguration()
		val template = configuration.getTemplate(ReleaseToMarkerNotification.templateLocation)
		template.process(ReleaseToMarkerNotification.renderNoCollectingSubmissions(
			assignment = assignment,
			numReleasedFeedbacks = 10,
			workflowVerb = "The_Verb"
		), writer)
		writer.flush()
		val renderedResult = output.toString
		renderedResult should be("For the Tabula audit of user access permissions, we have not yet received confirmations from all the User Access Managers (UAMs).\n\nIf you have not yet done so, to satisfy data audit requirements, please complete the Tabula User Audit form here:\n\nhttps://warwick.ac.uk/tabulaaudit\n\nHere is a list of departments and sub-departments that you should check:\n\ncomputer science - https://tabula.warwick.ac.uk/admin/permissions/department/cs/tree \n\ncomputer science for human - https://tabula.warwick.ac.uk/admin/permissions/department/csh/tree \n\n- Ensure that staff in your department have the appropriate permission levels.\n- Ensure that only those staff necessary have permission to view students’ personal information.\n- In accepting the UAM role, you agree that you are responsible for the accuracy of these permissions - and will monitor permissions periodically. If you are unable to monitor permissions in the future, you should request that the UAM role is assigned to another person within your department.\n\nFor audit purposes, this must be done by 26 September 2021.\n\nPlease be aware that, should we not receive a response, due to the audit implications we will need to remove your User Access Manager permissions and ask the Head of Department to select a new User Access Manager.\n\nThanks for your assistance in this matter.")

	}

	@Test
	def renderWhenCollectingSubmissions: Unit = {
		val output = new ByteArrayOutputStream
		val writer = new OutputStreamWriter(output)
		val configuration = newFreemarkerConfiguration()
		val template = configuration.getTemplate(ReleaseToMarkerNotification.templateLocation)
		template.process(ReleaseToMarkerNotification.renderCollectSubmissions(
			assignment = assignment,
			numAllocated = 12,
			studentsAtStagesCount = Seq(
				StudentAtStagesCount(DblFirstMarker.description, 2),
				StudentAtStagesCount(DblSecondMarker.description, 1),
				StudentAtStagesCount(DblFinalMarker.description, 2)
			),
			numReleasedFeedbacks = 12,
			numReleasedSubmissionsFeedbacks = 12,
			numReleasedNoSubmissionsFeedbacks = 12,
			workflowVerb = "the_verb"
		), writer)
		writer.flush()
		val renderedResult = output.toString
		renderedResult should be("For the Tabula audit of user access permissions, we have not yet received confirmations from all the User Access Managers (UAMs).\n\nIf you have not yet done so, to satisfy data audit requirements, please complete the Tabula User Audit form here:\n\nhttps://warwick.ac.uk/tabulaaudit\n\nHere is a list of departments and sub-departments that you should check:\n\ncomputer science - https://tabula.warwick.ac.uk/admin/permissions/department/cs/tree \n\ncomputer science for human - https://tabula.warwick.ac.uk/admin/permissions/department/csh/tree \n\n- Ensure that staff in your department have the appropriate permission levels.\n- Ensure that only those staff necessary have permission to view students’ personal information.\n- In accepting the UAM role, you agree that you are responsible for the accuracy of these permissions - and will monitor permissions periodically. If you are unable to monitor permissions in the future, you should request that the UAM role is assigned to another person within your department.\n\nFor audit purposes, this must be done by 26 September 2021.\n\nPlease be aware that, should we not receive a response, due to the audit implications we will need to remove your User Access Manager permissions and ask the Head of Department to select a new User Access Manager.\n\nThanks for your assistance in this matter.")

	}
}
