package uk.ac.warwick.tabula.data.model

import enumeratum.{Enum, EnumEntry}
import org.joda.time.DateTime

/**
 * As of TAB-8520 this is no longer an entity in its own right, but instead is a thin wrapper on
 * RecordedAssessmentComponentStudent
 */
case class FeedbackForSits(
  feedback: Feedback,
  status: FeedbackForSitsStatus,
  dateOfUpload: Option[DateTime],
  actualMarkLastUploaded: Option[Int],
  actualGradeLastUploaded: Option[String],
)

object FeedbackForSits {
  def apply(feedback: Feedback, recordedAssessmentComponentStudent: RecordedAssessmentComponentStudent): FeedbackForSits =
    FeedbackForSits(
      feedback = feedback,
      status =
        if (recordedAssessmentComponentStudent.needsWritingToSits) FeedbackForSitsStatus.UploadNotAttempted
        else FeedbackForSitsStatus.Successful,
      dateOfUpload = recordedAssessmentComponentStudent.lastWrittenToSits,
      actualMarkLastUploaded = recordedAssessmentComponentStudent.latestMark,
      actualGradeLastUploaded = recordedAssessmentComponentStudent.latestGrade,
    )

  def apply(feedback: Feedback, recordedAssessmentComponentStudents: Seq[RecordedAssessmentComponentStudent]): FeedbackForSits =
    FeedbackForSits(
      feedback = feedback,
      status =
        if (recordedAssessmentComponentStudents.exists(_.needsWritingToSits)) FeedbackForSitsStatus.UploadNotAttempted
        else FeedbackForSitsStatus.Successful,
      dateOfUpload = recordedAssessmentComponentStudents.map(_.lastWrittenToSits).max,
      actualMarkLastUploaded = recordedAssessmentComponentStudents.maxBy(_.lastWrittenToSits).latestMark,
      actualGradeLastUploaded = recordedAssessmentComponentStudents.maxBy(_.lastWrittenToSits).latestGrade,
    )
}

sealed abstract class FeedbackForSitsStatus(val code: String, val description: String) extends EnumEntry {
  override val entryName: String = code
  override def toString: String = description
}

object FeedbackForSitsStatus extends Enum[FeedbackForSitsStatus] {
  case object UploadNotAttempted extends FeedbackForSitsStatus("uploadNotAttempted", "Queued for SITS upload")
  case object Failed extends FeedbackForSitsStatus("failed", "SITS upload failed")
  case object Successful extends FeedbackForSitsStatus("successful", "Uploaded to SITS")

  override def values: IndexedSeq[FeedbackForSitsStatus] = findValues
}
