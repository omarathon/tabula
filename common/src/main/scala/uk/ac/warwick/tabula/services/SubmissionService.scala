package uk.ac.warwick.tabula.services

import org.hibernate.FetchMode
import org.hibernate.criterion.Order._
import org.hibernate.criterion.Restrictions
import org.hibernate.criterion.Restrictions.{ge, le}
import org.joda.time.{DateTime, LocalDate}
import org.springframework.stereotype.Service
import uk.ac.warwick.spring.Wire
import uk.ac.warwick.tabula.data.Daoisms
import uk.ac.warwick.tabula.data.Transactions.transactional
import uk.ac.warwick.tabula.data.model.{Assignment, FileAttachment, OriginalityReport, Submission}
import uk.ac.warwick.tabula.helpers.Logging
import uk.ac.warwick.userlookup.User

import scala.jdk.CollectionConverters._

trait SubmissionService {
  def saveSubmission(submission: Submission): Unit

  def getSubmissionByUsercode(assignment: Assignment, usercode: String): Option[Submission]

  def getSubmissionsByAssignment(assignment: Assignment): Seq[Submission]

  def loadSubmissionsForAssignment(assignment: Assignment): Seq[Submission]

  def getSubmission(id: String): Option[Submission]

  def getAllSubmissions(user: User): Seq[Submission]

  def getSubmissionsBetweenDates(usercode: String, startInclusive: DateTime, endInclusive: DateTime): Seq[Submission]

  def delete(submission: Submission): Unit
}

trait OriginalityReportService {
  def getOriginalityReportByFileId(fileId: String): Option[OriginalityReport]

  def getOriginalityReportByTcaSubmissionId(submissionId: String): Option[OriginalityReport]

  def getIncompleteTcaSubmissions(since: LocalDate): Seq[OriginalityReport]

  def deleteOriginalityReport(attachment: FileAttachment): Unit

  def saveOriginalityReport(attachment: FileAttachment): Unit

  def saveOrUpdate(report: OriginalityReport): Unit

  def refresh(report: OriginalityReport): Unit
}

abstract class AbstractSubmissionService extends SubmissionService with Daoisms with Logging {

  self: OriginalityReportServiceComponent =>

  def saveSubmission(submission: Submission): Unit = {
    session.saveOrUpdate(submission)
    session.flush()
  }

  def getSubmissionByUsercode(assignment: Assignment, usercode: String): Option[Submission] = {
    session.newCriteria[Submission]
      .add(is("assignment", assignment))
      .add(is("usercode", usercode))
      .uniqueResult
  }

  def getSubmissionsByAssignment(assignment: Assignment): Seq[Submission] = {
    session.newCriteria[Submission]
      .add(is("assignment", assignment)).seq
  }

  def loadSubmissionsForAssignment(assignment: Assignment): Seq[Submission] =
    session.newCriteria[Submission]
      .add(is("assignment", assignment))
      .addOrder(desc("submittedDate"))
      .setFetchMode("values", FetchMode.JOIN)
      .setFetchMode("values.attachments", FetchMode.JOIN)
      .setFetchMode("values.attachments._originalityReport", FetchMode.JOIN)
      .distinct
      .seq

  def getSubmission(id: String): Option[Submission] = getById[Submission](id)

  def getAllSubmissions(user: User): Seq[Submission] = {
    session.newCriteria[Submission]
      .add(is("usercode", user.getUserId))
      .seq
  }

  def getSubmissionsBetweenDates(usercode: String, startInclusive: DateTime, endInclusive: DateTime): Seq[Submission] =
    session.newCriteria[Submission]
      .add(is("usercode", usercode))
      .add(ge("submittedDate", startInclusive))
      .add(le("submittedDate", endInclusive))
      .seq

  def delete(submission: Submission): Unit = {
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
  var submissionService: SubmissionService = Wire[SubmissionService]
}


@Service(value = "originalityReportService")
class OriginalityReportServiceImpl extends OriginalityReportService with Daoisms with Logging {

  /**
    * Deletes the OriginalityReport attached to this Submission if one
    * exists. It flushes the session straight away because otherwise deletes
    * don't always happen until after some insert operation that assumes
    * we've deleted it.
    */
  def deleteOriginalityReport(attachment: FileAttachment): Unit = {
    if (attachment.originalityReport != null) {
      val report = attachment.originalityReport
      attachment.originalityReport = null
      session.delete(report)
      session.flush()
    }
  }

  def saveOriginalityReport(attachment: FileAttachment): Unit = {
    attachment.originalityReport.attachment = attachment
    session.saveOrUpdate(attachment.originalityReport)
  }

  def getOriginalityReportByFileId(fileId: String): Option[OriginalityReport] = {
    session.newCriteria[OriginalityReport]
      .createAlias("attachment", "attachment")
      .add(is("attachment.id", fileId))
      .seq.headOption
  }

  def getOriginalityReportByTcaSubmissionId(submissionId: String): Option[OriginalityReport] = {
    session.newCriteria[OriginalityReport]
      .add(is("tcaSubmission", submissionId))
      .seq.headOption
  }

  def getIncompleteTcaSubmissions(since: LocalDate): Seq[OriginalityReport] = {
    session.newCriteria[OriginalityReport]
      .add(Restrictions.eq("tcaSubmissionRequested", true))
      .add(Restrictions.eq("reportReceived", false))
      .add(Restrictions.or(
        Restrictions.isNull("lastSubmittedToTurnitin"),
        Restrictions.between("lastSubmittedToTurnitin", since.toDateTimeAtStartOfDay, new DateTime().minusHours(1))
      ))
      .seq
  }

  def saveOrUpdate(report: OriginalityReport): Unit = transactional() { session.saveOrUpdate(report) }

  def refresh(report: OriginalityReport): Unit = session.refresh(report)
}

trait OriginalityReportServiceComponent {
  def originalityReportService: OriginalityReportService
}

trait AutowiringOriginalityReportServiceComponent extends OriginalityReportServiceComponent {
  var originalityReportService: OriginalityReportService = Wire[OriginalityReportService]
}
