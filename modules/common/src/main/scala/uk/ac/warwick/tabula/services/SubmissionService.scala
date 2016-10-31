package uk.ac.warwick.tabula.services


import org.springframework.stereotype.Service

import uk.ac.warwick.tabula.data.Daoisms
import uk.ac.warwick.tabula.data.model._
import uk.ac.warwick.tabula.helpers.Logging
import uk.ac.warwick.spring.Wire
import scala.collection.JavaConverters._

trait SubmissionService {
	def saveSubmission(submission: Submission)
	def getSubmissionByUniId(assignment: Assignment, uniId: String): Option[Submission]
	def getSubmissionsByAssignment(assignment: Assignment): Seq[Submission]
	def getSubmission(id: String): Option[Submission]

	def delete(submission: Submission): Unit
}

trait OriginalityReportService {
	def getOriginalityReportByFileId(fileId: String): Option[OriginalityReport]
	def deleteOriginalityReport(attachment: FileAttachment): Unit
	def saveOriginalityReport(attachment: FileAttachment): Unit
	def saveOrUpdate(report: OriginalityReport): Unit
	def refresh(report: OriginalityReport): Unit
}

abstract class AbstractSubmissionService extends SubmissionService with Daoisms with Logging {

	self: OriginalityReportServiceComponent =>

	def saveSubmission(submission: Submission) = {
		session.saveOrUpdate(submission)
		session.flush()
	}

	def getSubmissionByUniId(assignment: Assignment, uniId: String) = {
		session.newCriteria[Submission]
			.add(is("assignment", assignment))
			.add(is("universityId", uniId))
			.uniqueResult
	}

	def getSubmissionsByAssignment(assignment: Assignment) : Seq[Submission] = {
		session.newCriteria[Submission]
			.add(is("assignment", assignment)).seq
	}

	def getSubmission(id: String) = getById[Submission](id)

	def delete(submission: Submission) {
		submission.assignment.submissions.remove(submission)
		// TAB-4564 delete the originality report; needs to be done manually because we don't cascade the delete through FileAttachment
		submission.valuesWithAttachments.flatMap(_.attachments.asScala).foreach(originalityReportService.deleteOriginalityReport)
		session.delete(submission)
		// force delete now, just for the cases where we re-insert in the same session
		// (i.e. when a student is resubmitting work). [HFC-385#comments]
		session.flush()
	}
}

@Service(value = "submissionService")
class SubmissionServiceImpl
	extends AbstractSubmissionService
		with AutowiringOriginalityReportServiceComponent

trait SubmissionServiceComponent {
	def submissionService: SubmissionService
}

trait AutowiringSubmissionServiceComponent extends SubmissionServiceComponent {
	var submissionService = Wire[SubmissionService]
}



@Service(value = "originalityReportService")
class OriginalityReportServiceImpl extends OriginalityReportService with Daoisms with Logging {

	/**
	 * Deletes the OriginalityReport attached to this Submission if one
	 * exists. It flushes the session straight away because otherwise deletes
	 * don't always happen until after some insert operation that assumes
	 * we've deleted it.
	 */
	def deleteOriginalityReport(attachment: FileAttachment) {
		if (attachment.originalityReport != null) {
			val report = attachment.originalityReport
			attachment.originalityReport = null
			session.delete(report)
			session.flush()
		}
	}

	def saveOriginalityReport(attachment: FileAttachment) {
		attachment.originalityReport.attachment = attachment
		session.saveOrUpdate(attachment.originalityReport)
	}

	def getOriginalityReportByFileId(fileId: String): Option[OriginalityReport] = {
		session.newCriteria[OriginalityReport]
			.createAlias("attachment", "attachment")
			.add(is("attachment.id", fileId))
			.seq.headOption
	}

	def saveOrUpdate(report: OriginalityReport): Unit = session.saveOrUpdate(report)

	def refresh(report: OriginalityReport): Unit = session.refresh(report)
}

trait OriginalityReportServiceComponent {
	def originalityReportService: OriginalityReportService
}

trait AutowiringOriginalityReportServiceComponent extends OriginalityReportServiceComponent {
	var originalityReportService = Wire[OriginalityReportService]
}