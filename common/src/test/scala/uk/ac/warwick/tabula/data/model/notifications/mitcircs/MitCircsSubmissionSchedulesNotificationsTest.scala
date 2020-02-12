package uk.ac.warwick.tabula.data.model.notifications.mitcircs

import org.joda.time.LocalDate
import uk.ac.warwick.tabula.commands.mitcircs.submission.{MitCircsSubmissionSchedulesNotifications, MitCircsSubmissionState}
import uk.ac.warwick.tabula.data.model.StudentMember
import uk.ac.warwick.tabula.data.model.mitcircs.MitigatingCircumstancesSubmission
import uk.ac.warwick.tabula.{Fixtures, Mockito, TestBase}
import uk.ac.warwick.userlookup.User

class MitCircsSubmissionSchedulesNotificationsTest extends TestBase with Mockito with MitCircsNotificationFixture {

  @Test
  def scheduledNotifications(): Unit = {
    val submission: MitigatingCircumstancesSubmission = Fixtures.mitigatingCircumstancesSubmission("student", "student")
    submission.pendingEvidenceDue = LocalDate.now.plusDays(1)
    submission.pendingEvidence = "Doctors note"

    val scheduler = new MitCircsSubmissionSchedulesNotifications with MitCircsSubmissionState {
      override val student: StudentMember = submission.student
      override val currentUser: User = student.asSsoUser
    }

    val notifications  = scheduler.scheduledNotifications(submission).groupBy(_.notificationType)
    notifications("PendingEvidenceReminder").size should be (7)
    notifications("PendingEvidenceReminder").head.scheduledDate should be (LocalDate.now.plusDays(1).toDateTimeAtStartOfDay)
    notifications("PendingEvidenceReminder").tail.head.scheduledDate should be (LocalDate.now.plusDays(2).toDateTimeAtStartOfDay)
    notifications("DraftSubmissionReminder").size should be (12)
  }

}
