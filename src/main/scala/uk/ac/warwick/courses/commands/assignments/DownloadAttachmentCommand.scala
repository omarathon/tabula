package uk.ac.warwick.courses.commands.assignments

import scala.reflect.BeanProperty
import scala.collection.JavaConversions.asScalaBuffer
import org.springframework.beans.factory.annotation.Autowired
import uk.ac.warwick.courses.CurrentUser
import uk.ac.warwick.courses.commands.Command
import uk.ac.warwick.courses.commands.Description
import uk.ac.warwick.courses.commands.ReadOnly
import uk.ac.warwick.courses.data.model.Assignment
import uk.ac.warwick.courses.data.model.Module
import uk.ac.warwick.courses.data.model.Submission
import uk.ac.warwick.courses.helpers.StringUtils.StringToSuperString
import uk.ac.warwick.courses.services.fileserver.RenderableAttachment
import uk.ac.warwick.courses.services.fileserver.RenderableFile
import uk.ac.warwick.courses.services.AssignmentService
import scala.None
import org.springframework.beans.factory.annotation.Configurable

@Configurable
class DownloadAttachmentCommand(user: CurrentUser) extends Command[Option[RenderableFile]] with ReadOnly {
  
  @BeanProperty var module: Module = _
  @BeanProperty var assignment: Assignment = _
  @BeanProperty var filename: String = _
  @Autowired var assignmentService:AssignmentService = _
  
  private var fileFound: Boolean = _
  var callback: (RenderableFile) => Unit = _

  def apply() = {
    val submission = assignmentService.getSubmissionByUniId(assignment, user.universityId);
    val allAttachments = submission map {_.allAttachments} getOrElse Nil
    val attachment = allAttachments find (_.name == filename) map (a => new RenderableAttachment(a))

    fileFound = attachment.isDefined
    if (callback != null) {
      attachment.map { callback(_) }
    }
    attachment
  }

  override def describe(d: Description) = {
    d.assignment(assignment)
    d.property("filename", filename)
  }

  override def describeResult(d: Description) {
    d.property("fileFound", fileFound)
  }

}