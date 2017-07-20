package uk.ac.warwick.tabula.commands.cm2.feedback

import uk.ac.warwick.tabula.commands._
import uk.ac.warwick.tabula.commands.cm2.feedback.DownloadFeedbackCommand._
import uk.ac.warwick.tabula.data.model._
import uk.ac.warwick.tabula.helpers.StringUtils.StringToSuperString
import uk.ac.warwick.tabula.permissions._
import uk.ac.warwick.tabula.services.fileserver._
import uk.ac.warwick.tabula.services.{AutowiringZipServiceComponent, ZipServiceComponent}
import uk.ac.warwick.tabula.system.permissions.{PermissionsChecking, PermissionsCheckingMethods, RequiresPermissionsChecking}

import scala.collection.JavaConversions.asScalaBuffer

object DownloadFeedbackCommand {
	type Result = Option[RenderableFile]
	type Command = Appliable[Result]

	val RequiredPermission = Permissions.AssignmentFeedback.Read

	def apply(assignment: Assignment, feedback: Feedback, student: Option[Member]): Command =
		new DownloadFeedbackCommandInternal(assignment, feedback, student)
			with ComposableCommand[Result]
			with AutowiringZipServiceComponent
			with DownloadFeedbackPermissions
			with DownloadFeedbackDescription
			with ReadOnly
}

trait DownloadFeedbackState {
	def assignment: Assignment
	def feedback: Feedback
	def student: Option[Member]
}

trait DownloadFeedbackRequest extends DownloadFeedbackState {
	var filename: String = _
}

class DownloadFeedbackCommandInternal(val assignment: Assignment, val feedback: Feedback, val student: Option[Member]) extends CommandInternal[Result]
	with DownloadFeedbackRequest {
	self: ZipServiceComponent =>

	/**
	 * If filename is unset, it returns a renderable Zip of all files.
	 * If filename is set, it will return a renderable attachment if found.
	 * In either case if it's not found, None is returned.
	 */
	override def applyInternal(): Result = filename match {
		case filename: String if filename.hasText =>
			feedback.attachments.find(_.name == filename).map(new RenderableAttachment(_))
		case _ => Some(zipService.getFeedbackZip(feedback))
	}
}

trait DownloadFeedbackPermissions extends RequiresPermissionsChecking with PermissionsCheckingMethods {
	self: DownloadFeedbackState =>

	override def permissionsCheck(p: PermissionsChecking): Unit = {
		notDeleted(mandatory(assignment))
		student match {
			case Some(student: StudentMember) => p.PermissionCheckAny(
				Seq(CheckablePermission(RequiredPermission, mandatory(feedback)),
					CheckablePermission(RequiredPermission, student))
			)
			case _ => p.PermissionCheck(RequiredPermission, mandatory(feedback))
		}

	}
}

trait DownloadFeedbackDescription extends Describable[Result] {
	self: DownloadFeedbackRequest =>

	override lazy val eventName: String = "DownloadFeedback"

	override def describe(d: Description): Unit =
		d.assignment(assignment)
		 .property("filename", filename)

	override def describeResult(d: Description, result: Option[RenderableFile]): Unit =
		d.property("fileFound", result.nonEmpty)
}
