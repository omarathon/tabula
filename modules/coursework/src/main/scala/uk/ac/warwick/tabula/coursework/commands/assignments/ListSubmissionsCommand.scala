package uk.ac.warwick.tabula.coursework.commands.assignments

import uk.ac.warwick.tabula.commands.Command
import uk.ac.warwick.tabula.commands.Unaudited
import uk.ac.warwick.tabula.data.model._
import uk.ac.warwick.tabula.data.model.{Assignment, Module, Submission}
import scala.reflect.BeanProperty
import scala.collection.JavaConversions._
import uk.ac.warwick.tabula.helpers.DateTimeOrdering._
import uk.ac.warwick.tabula.commands.ReadOnly
import uk.ac.warwick.tabula.services.AuditEventIndexService
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Configurable
import uk.ac.warwick.spring.Wire

class ListSubmissionsCommand extends Command[Seq[SubmissionListItem]] with Unaudited with ReadOnly {
  var auditIndex = Wire.auto[AuditEventIndexService]

	@BeanProperty var assignment: Assignment = _
	@BeanProperty var module: Module = _

	var checkIndex = true

	def applyInternal() = {
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