package uk.ac.warwick.courses.commands.assignments

import uk.ac.warwick.courses.commands.Command
import uk.ac.warwick.courses.commands.Unaudited
import uk.ac.warwick.courses.data.model._
import scala.reflect.BeanProperty
import scala.collection.JavaConversions._
import uk.ac.warwick.courses.helpers.DateTimeOrdering._
import uk.ac.warwick.courses.commands.ReadOnly
import uk.ac.warwick.courses.services.AuditEventIndexService
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Configurable

@Configurable
class ListSubmissionsCommand extends Command[Seq[SubmissionListItem]] with Unaudited with ReadOnly {

	@BeanProperty var assignment: Assignment = _
	@BeanProperty var module: Module = _

	@Autowired var auditIndex: AuditEventIndexService = _

	var checkIndex = true

	def work = {
		val submissions = assignment.submissions.sortBy(_.submittedDate).reverse
		val downloads =
			if (checkIndex) auditIndex.adminDownloadedSubmissions(assignment)
			else Nil
		submissions map { submission =>
			SubmissionListItem(submission, downloads.contains(submission))
		}
	}

}

case class SubmissionListItem(val submission: Submission, val downloaded: Boolean)