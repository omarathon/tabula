package uk.ac.warwick.tabula.commands.coursework.feedback

import uk.ac.warwick.spring.Wire
import uk.ac.warwick.tabula.commands.{Command, Description, ReadOnly}
import uk.ac.warwick.tabula.data.model.{Assignment, Feedback, MarkerFeedback, Module}
import uk.ac.warwick.tabula.permissions._
import uk.ac.warwick.tabula.services.ZipService
import uk.ac.warwick.tabula.services.fileserver.{RenderableAttachment, RenderableFile}

import scala.concurrent.Await
import scala.concurrent.duration.Duration

class OldAdminGetSingleFeedbackCommand(module: Module, assignment: Assignment, feedback: Feedback) extends Command[RenderableFile] with ReadOnly {
  mustBeLinked(assignment, module)
  PermissionCheck(Permissions.AssignmentFeedback.Read, feedback)

  var zipService: ZipService = Wire.auto[ZipService]

  override def applyInternal(): RenderableFile = Await.result(zipService.getFeedbackZip(feedback), Duration.Inf)

  override def describe(d: Description): Unit =
    d.feedback(feedback)
     .properties("attachmentCount" -> feedback.attachments.size)
}

class OldAdminGetSingleFeedbackFileCommand(module: Module, assignment: Assignment, feedback: Feedback) extends Command[Option[RenderableFile]] with ReadOnly {
  mustBeLinked(assignment, module)
  PermissionCheck(Permissions.AssignmentFeedback.Read, feedback)

  var filename: String = _

  private var fileFound: Boolean = _

  def applyInternal(): Option[RenderableAttachment] = {
    val attachment = Option(new RenderableAttachment(feedback.attachments.iterator().next()))
    fileFound = attachment.isDefined
    attachment
  }

  override def describe(d: Description): Unit =
    d.assignment(assignment)
     .property("filename", filename)

  override def describeResult(d: Description, result: Option[RenderableFile]): Unit =
    d.property("fileFound", result.isDefined)

}


class AdminGetSingleMarkerFeedbackCommand(module: Module, assignment: Assignment, markerFeedback: MarkerFeedback) extends Command[RenderableFile] with ReadOnly {

  mustBeLinked(assignment, module)
  PermissionCheck(Permissions.AssignmentMarkerFeedback.Manage, assignment)

  var zipService: ZipService = Wire.auto[ZipService]

  override def applyInternal(): RenderableFile = Await.result(zipService.getSomeMarkerFeedbacksZip(Seq(markerFeedback)), Duration.Inf)

  override def describe(d: Description): Unit = d.feedback(markerFeedback.feedback).properties(
    "studentId" -> markerFeedback.feedback.studentIdentifier,
    "attachmentCount" -> markerFeedback.attachments.size)
}
