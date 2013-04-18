package uk.ac.warwick.tabula.coursework.commands.assignments

import scala.collection.JavaConversions._
import uk.ac.warwick.tabula.data.model.{Submission, Assignment, Module}
import uk.ac.warwick.tabula.commands.{ReadOnly, Unaudited, Command}
import uk.ac.warwick.tabula.permissions.Permissions
import uk.ac.warwick.spring.Wire
import uk.ac.warwick.tabula.services.AuditEventIndexService
import uk.ac.warwick.tabula.helpers.DateTimeOrdering._

class ListSubmissionsCommand(val module: Module, val assignment: Assignment) extends Command[Seq[SubmissionListItem]] with Unaudited with ReadOnly {

	mustBeLinked(mandatory(assignment), mandatory(module))
	PermissionCheck(Permissions.Submission.Read, assignment)

	var auditIndex = Wire[AuditEventIndexService]
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

case class SubmissionListItem(submission: Submission, downloaded: Boolean)