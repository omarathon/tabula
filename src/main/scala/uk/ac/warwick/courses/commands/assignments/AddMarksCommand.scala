package uk.ac.warwick.courses.commands.assignments

import uk.ac.warwick.courses.data.Daoisms
import scala.util.matching.Regex
import uk.ac.warwick.courses.commands.Command
import org.springframework.beans.factory.annotation.Configurable
import uk.ac.warwick.courses.helpers.Logging
import uk.ac.warwick.courses.data.model.Feedback
import org.springframework.beans.factory.annotation.Autowired
import uk.ac.warwick.courses.services.UserLookupService
import org.springframework.transaction.annotation.Transactional
import uk.ac.warwick.courses.commands.UploadedFile
import uk.ac.warwick.courses.CurrentUser
import uk.ac.warwick.courses.data.model.Assignment
import uk.ac.warwick.courses.commands.Description

@Configurable
class AddMarksCommand( val assignment:Assignment, val submitter:CurrentUser ) extends Command[List[Feedback]] with Daoisms with Logging  {

  val uniNumberPattern = new Regex("""(\d{7,})""")
 
  @Autowired var userLookup:UserLookupService =_
    
    
  @Transactional
  override def apply(): List[Feedback] = {
    val a = List(new Feedback)
    a
  }
  
  def describe(d: Description) = d
    .assignment(assignment)

}