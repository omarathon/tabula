package uk.ac.warwick.courses.commands.assignments

import uk.ac.warwick.courses.commands.Command
import uk.ac.warwick.courses.commands.Unaudited
import uk.ac.warwick.courses.data.model._
import scala.reflect.BeanProperty
import scala.collection.JavaConversions._
import uk.ac.warwick.courses.helpers.DateTimeOrdering._

class ListSubmissionsCommand extends Command[Seq[Submission]] with Unaudited {
	
	@BeanProperty var assignment:Assignment =_
	@BeanProperty var module:Module =_
	
	def apply = assignment.submissions sortBy { _.submittedDate } reverse
	
}