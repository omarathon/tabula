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

@Configurable
class DownloadFeedbackCommand(user:CurrentUser) extends Command[Option[RenderableFile]] {
	@Autowired var zip:ZipService =_
	@Autowired var feedbackDao:FeedbackDao =_
	
	@BeanProperty var module:Module =_
	@BeanProperty var assignment:Assignment =_
	
	def apply() = 
	  feedbackDao.getFeedbackByUniId(assignment, user.universityId) match {
		 case Some(feedback) => Some(new RenderableZip( zip.getFeedbackZip(feedback) ))
		 case None => None
	  }
	
	def describe(d:Description) = d.assignment(assignment)
	
}