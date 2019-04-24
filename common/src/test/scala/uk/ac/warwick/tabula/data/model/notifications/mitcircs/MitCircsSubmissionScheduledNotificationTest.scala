package uk.ac.warwick.tabula.data.model.notifications.mitcircs

import org.joda.time.LocalDate
import uk.ac.warwick.tabula.commands.mitcircs.submission.{CreateMitCircsSubmissionState, MitCircsSubmissionScheduledNotification}
import uk.ac.warwick.tabula.data.model.mitcircs.MitigatingCircumstancesSubmission
import uk.ac.warwick.tabula.data.model.{Department, StudentMember}
import uk.ac.warwick.tabula.{Fixtures, Mockito, TestBase}
import uk.ac.warwick.userlookup.User

class MitCircsSubmissionScheduledNotificationTest extends TestBase with Mockito with MitCircsNotificationFixture {

  @Test
  def scheduledNotifications(): Unit = {
    val submission: MitigatingCircumstancesSubmission = Fixtures.mitigatingCircumstancesSubmission("student", "student")
    submission.pendingEvidenceDue = LocalDate.now.plusDays(1)
    submission.pendingEvidence = "Doctors note"

    val scheduler = new MitCircsSubmissionScheduledNotification with CreateMitCircsSubmissionState {
      override lazy val department: Department = submission.department
      override val student: StudentMember = submission.student
      override val currentUser: User = student.asSsoUser
    }

    val notifications  = scheduler.scheduledNotifications(submission)
    notifications.size should be (2)
    notifications.head.notificationType should be ("PendingEvidenceReminder")
    notifications.head.scheduledDate should be (LocalDate.now.plusDays(1).toDateTimeAtStartOfDay)
    notifications.tail.head.scheduledDate should be (LocalDate.now.plusDays(2).toDateTimeAtStartOfDay)
  }

}
