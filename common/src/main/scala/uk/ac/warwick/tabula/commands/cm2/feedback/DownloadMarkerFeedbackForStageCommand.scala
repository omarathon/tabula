package uk.ac.warwick.tabula.commands.cm2.feedback

import uk.ac.warwick.tabula.CurrentUser
import uk.ac.warwick.tabula.JavaImports._
import uk.ac.warwick.tabula.commands._
import uk.ac.warwick.tabula.commands.cm2.feedback.DownloadMarkerFeedbackForStageCommand._
import uk.ac.warwick.tabula.data.model.markingworkflow.MarkingWorkflowStage
import uk.ac.warwick.tabula.data.model.{Assignment, AssignmentFeedback, MarkerFeedback}
import uk.ac.warwick.tabula.permissions.Permissions
import uk.ac.warwick.tabula.services.fileserver.RenderableFile
import uk.ac.warwick.tabula.services.{AutowiringZipServiceComponent, ZipServiceComponent}
import uk.ac.warwick.tabula.system.permissions.{PermissionsChecking, PermissionsCheckingMethods, RequiresPermissionsChecking}
import uk.ac.warwick.userlookup.User

import scala.collection.JavaConverters._

object DownloadMarkerFeedbackForStageCommand {
	type Result = RenderableFile
	type Command = Appliable[Result]

	def apply(assignment: Assignment, marker: User, stage: MarkingWorkflowStage, submitter: CurrentUser): Command =
		new DownloadMarkerFeedbackForStageCommandInternal(assignment, marker, stage, submitter)
			with ComposableCommand[Result]
			with AutowiringZipServiceComponent
			with DownloadMarkerFeedbackForStagePermissions
			with DownloadMarkerFeedbackForStageDescription
			with ReadOnly
}

trait DownloadMarkerFeedbackForStageState {
	def assignment: Assignment
	def marker: User
	def stage: MarkingWorkflowStage
	def submitter: CurrentUser
}

trait DownloadMarkerFeedbackForStageRequest {
	self: DownloadMarkerFeedbackForStageState =>

	def assignment: Assignment
	def marker: User
	def submitter: CurrentUser
	var markerFeedback: JList[MarkerFeedback] = JArrayList()
	def students: Seq[User] = markerFeedback.asScala.map(_.student)
	// can only download these students submissions or a subset
	def markersStudents: Seq[User] = assignment.cm2MarkerAllocations(marker).flatMap(_.students).distinct

	lazy val feedbacks: Seq[AssignmentFeedback] = {
		// filter out ones we aren't the marker for
		val studentsToDownload = students.filter(markersStudents.contains).map(_.getUserId)
		assignment.feedbacks.asScala.filter(s => studentsToDownload.contains(s.usercode))
	}

	lazy val previousFeedback: Seq[MarkerFeedback] =
		feedbacks.flatMap { feedback =>
			val currentStageIndex = feedback.currentStageIndex

			if (currentStageIndex >= stage.order) feedback.markerFeedback.asScala.find(_.stage == stage).toSeq
			else Nil
		}

}

class DownloadMarkerFeedbackForStageCommandInternal(val assignment: Assignment, val marker: User, val stage: MarkingWorkflowStage, val submitter: CurrentUser)
	extends CommandInternal[Result] with DownloadMarkerFeedbackForStageState with DownloadMarkerFeedbackForStageRequest {
	self: ZipServiceComponent =>

	override def applyInternal(): Result =
		zipService.getSomeMarkerFeedbacksZip(previousFeedback)
}

trait DownloadMarkerFeedbackForStagePermissions extends RequiresPermissionsChecking with PermissionsCheckingMethods {
	self: DownloadMarkerFeedbackForStageState =>

	override def permissionsCheck(p: PermissionsChecking): Unit = {
		p.PermissionCheck(Permissions.AssignmentFeedback.Read, mandatory(assignment))
		if (submitter.apparentUser != marker) {
			p.PermissionCheck(Permissions.Assignment.MarkOnBehalf, mandatory(assignment))
		}
	}

}

trait DownloadMarkerFeedbackForStageDescription extends Describable[Result] {
	self: DownloadMarkerFeedbackForStageRequest =>

	override lazy val eventName: String = "DownloadMarkerFeedbackForStage"

	override def describe(d: Description): Unit =
		d.assignment(assignment)
			.feedbacks(feedbacks)
			.studentIds(feedbacks.flatMap(_.universityId))
			.studentUsercodes(feedbacks.map(_.usercode))
			.properties("feedbackCount" -> previousFeedback.size)
}