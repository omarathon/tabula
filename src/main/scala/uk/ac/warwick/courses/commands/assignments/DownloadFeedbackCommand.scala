package uk.ac.warwick.courses.commands.assignments
import uk.ac.warwick.courses.data.model.Feedback
import org.springframework.transaction.annotation.Transactional
import uk.ac.warwick.courses.UniversityId
import uk.ac.warwick.courses.commands.Command
import uk.ac.warwick.courses.data.Daoisms
import uk.ac.warwick.courses.commands.UploadedFile
import org.springframework.validation.Errors
import scala.reflect.BeanProperty
import uk.ac.warwick.courses.commands.Description
import org.hibernate.validator.constraints.NotEmpty
import uk.ac.warwick.util.core.StringUtils

//
//class DownloadFeedbackCommand( val feedback:Feedback ) extends Command[Feedback] with Daoisms {
//	
//	def apply()
//
//}