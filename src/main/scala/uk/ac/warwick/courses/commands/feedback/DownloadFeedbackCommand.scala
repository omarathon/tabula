package uk.ac.warwick.courses.commands.feedback
import uk.ac.warwick.courses.commands.Command
import uk.ac.warwick.courses.services.fileserver.RenderableFile
import uk.ac.warwick.courses.services.ZipService
import org.springframework.beans.factory.annotation.Autowired
import uk.ac.warwick.courses.data.model.Assignment
import uk.ac.warwick.courses.data.model.Module
import scala.reflect.BeanProperty
import uk.ac.warwick.courses.commands.Description
import uk.ac.warwick.courses.CurrentUser
import uk.ac.warwick.courses.data.FeedbackDao
import uk.ac.warwick.courses.services.fileserver.RenderableZip
import org.springframework.beans.factory.annotation.Configurable
import uk.ac.warwick.courses.helpers.StringUtils._
import collection.JavaConversions._
import uk.ac.warwick.courses.services.fileserver.RenderableAttachment
import uk.ac.warwick.courses.data.model.Feedback

@Configurable
class DownloadFeedbackCommand(user:CurrentUser) extends Command[Option[RenderableFile]] {
	@Autowired var zip:ZipService =_
	@Autowired var feedbackDao:FeedbackDao =_
	
	@BeanProperty var module:Module =_
	@BeanProperty var assignment:Assignment =_
	@BeanProperty var filename:String =_
	
	private val FeedbackZipName = "feedback.zip"
	
	private var filenameDownloaded:String =_
	
	/**
	 * If filename is unset, it returns a renderable Zip of all files.
	 * If filename is set, it will return a renderable attachment if found.
	 * In either case if it's not found, None is returned.
	 */
	def apply() = 
	  feedbackDao.getFeedbackByUniId(assignment, user.universityId) map { (feedback) =>
		 filename match {
			 case FeedbackZipName => zipped(feedback)
			 case filename:String if filename.hasText => {
				 feedback.attachments.find( _.name == filename ).map( new RenderableAttachment(_) ).orNull
			 }
			 case _ => zipped(feedback)
		 }
	  }
	
	private def zipped(feedback:Feedback) = new RenderableZip( zip.getFeedbackZip(feedback) )
	
	override def describe(d:Description) = { 
		d.assignment(assignment)
		
	}
	
	private def specificFilename = filename.hasText
	
	override def describeResult(d:Description) {
		
	}
	
}