package uk.ac.warwick.courses.commands.assignments

import scala.reflect.BeanProperty
import scala.collection.JavaConversions.asScalaBuffer

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

class DownloadAttachmentCommand(user: CurrentUser) extends Command[Option[RenderableFile]] with ReadOnly {

  @BeanProperty var module: Module = _
  @BeanProperty var assignment: Assignment = _
  @BeanProperty var filename: String = _

  private var fileFound: Boolean = _
  var callback: (RenderableFile) => Unit = _

  def apply() = {

    // get the currentUsers submissions
    var submissions:Seq[Submission] = assignment.getSubmissions
    
    
    submissions.filter(name => name == user.universityId)
    
    // TODO - maybe multiple submission leads to more than one submission - if so get the latest
    // should only have one submission per assignment so take the first 
    var submission = submissions.head      //may explode if empty
    var attatchment = submission.allAttachments.filter(attatchment => attatchment.name == filename).head; // find instead
    val result = Option(new RenderableAttachment(attatchment))
     
    fileFound = result.isDefined
    if (callback != null) {
      result.map { callback(_) }
    }
    result
  }

  override def describe(d: Description) = {
    d.assignment(assignment)
    d.property("filename", filename)
  }

  override def describeResult(d: Description) {
    d.property("fileFound", fileFound)
  }

}