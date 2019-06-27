package uk.ac.warwick.tabula.data.model.notifications.mitcircs

import org.joda.time.{DateTime, DateTimeConstants, LocalDate}
import org.mockito.Mockito._
import uk.ac.warwick.tabula.helpers.Tap._
import uk.ac.warwick.tabula.commands.mitcircs.submission.AffectedAssessmentItem
import uk.ac.warwick.tabula.data.model.mitcircs.{MitigatingCircumstancesAcuteOutcome, MitigatingCircumstancesAffectedAssessment, MitigatingCircumstancesGrading, MitigatingCircumstancesRejectionReason, MitigatingCircumstancesSubmission}
import uk.ac.warwick.tabula.data.model.permissions.GrantedRole
import uk.ac.warwick.tabula.data.model.{AssessmentType, Department, Notification, UserGroup}
import uk.ac.warwick.tabula.roles.MitigatingCircumstancesOfficerRoleDefinition
import uk.ac.warwick.tabula.services.UserLookupService
import uk.ac.warwick.tabula.services.permissions.PermissionsService
import uk.ac.warwick.tabula.web.views.{FreemarkerRendering, MarkdownRendererImpl, ScalaFreemarkerConfiguration}
import uk.ac.warwick.tabula.{AcademicYear, Fixtures, Mockito, TestBase}
import uk.ac.warwick.userlookup.User

import scala.collection.JavaConverters._

class MitCircsRecordAcuteOutcomesNotificationTest extends TestBase with Mockito with FreemarkerRendering with MarkdownRendererImpl {

  val freeMarkerConfig: ScalaFreemarkerConfiguration = newFreemarkerConfiguration()

  val now: DateTime = new DateTime(2019, DateTimeConstants.JUNE, 13, 13, 15, 0, 0)

  private trait MitCircsRecordAcuteOutcomesNotificationFixture {
    val submission: MitigatingCircumstancesSubmission = Fixtures.mitigatingCircumstancesSubmission("student", "student")
    submission.key = 1000L
    val student: User = submission.student.asSsoUser
    val admin: User = Fixtures.user("admin", "admin").tap(_.setFullName("John Admin"))
    val mcoRole: GrantedRole[Department] = new GrantedRole[Department]
    mcoRole.users.add(admin)
    mcoRole.users.asInstanceOf[UserGroup].userLookup = Fixtures.userLookupService(admin)
    lazy val mockPermissionsService: PermissionsService = smartMock[PermissionsService]
    when(mockPermissionsService.getGrantedRole(submission.department, MitigatingCircumstancesOfficerRoleDefinition)).thenReturn(Some(mcoRole))
    val userLookup: UserLookupService = Fixtures.userLookupService(student, admin)

    submission.affectedAssessments.add(
      new MitigatingCircumstancesAffectedAssessment(submission, new AffectedAssessmentItem {{
        moduleCode = "IN304-15"
        module = Fixtures.module("in304")
        sequence = "A01"
        academicYear = AcademicYear.starting(2018)
        name = "Essay (2000 words)"
        assessmentType = AssessmentType.Assignment
        deadline = new LocalDate(2019, DateTimeConstants.JUNE, 20)
      }})
    )
    submission.affectedAssessments.add(
      new MitigatingCircumstancesAffectedAssessment(submission, new AffectedAssessmentItem {{
        moduleCode = "IN305-30"
        module = Fixtures.module("in305")
        sequence = "A01"
        academicYear = AcademicYear.starting(2018)
        name = "Dissertation"
        assessmentType = AssessmentType.Assignment
        deadline = new LocalDate(2019, DateTimeConstants.JUNE, 30)
      }})
    )

    submission.approveAndSubmit()

    submission.outcomeGrading = MitigatingCircumstancesGrading.Moderate
    submission.acuteOutcome = MitigatingCircumstancesAcuteOutcome.Extension
    submission.affectedAssessments.asScala.foreach(_.acuteOutcome = MitigatingCircumstancesAcuteOutcome.Extension)

    submission.outcomesLastRecordedOn = now.plusHours(2).plusMinutes(5)
    submission.outcomesLastRecordedBy = admin
    submission.outcomesRecorded()

    val n: MitCircsRecordAcuteOutcomesNotification =
      Notification.init(new MitCircsRecordAcuteOutcomesNotification, admin, submission)
  }

  @Test
  def textIsFormattedCorrectly(): Unit = withFakeTime(now) { new MitCircsRecordAcuteOutcomesNotificationFixture {
    val notificationContent: String = renderToString(freeMarkerConfig.getTemplate(n.content.template), n.content.model)
    notificationContent should be(
      """Outcomes last recorded by John Admin at 13 June 2019 at 15:20:00:
        |
        |MIT-1,000: 13 June 2019 - 27 June 2019
        |
        |**Grant extensions**
        |
        |Affected assessments:
        |- IN304 Module in304 (18/19) - Essay (2000 words)
        |- IN305 Module in305 (18/19) - Dissertation
        |""".stripMargin
    )

    // Make sure the Markdown -> HTML is sane
    renderMarkdown(notificationContent) should be (
      """<p>Outcomes last recorded by John Admin at 13 June 2019 at 15:20:00:</p>
        |<p>MIT-1,000: 13 June 2019 - 27 June 2019</p>
        |<p><strong>Grant extensions</strong></p>
        |<p>Affected assessments:</p>
        |<ul><li>IN304 Module in304 (18/19) - Essay (2000 words)</li><li>IN305 Module in305 (18/19) - Dissertation</li></ul>
        |""".stripMargin
    )
  }}

  @Test
  def textIsFormattedCorrectlyWithPartialData(): Unit = withFakeTime(now) { new MitCircsRecordAcuteOutcomesNotificationFixture {
    submission.acuteOutcome = null
    submission.affectedAssessments.asScala.foreach(_.acuteOutcome = null)
    submission.endDate = null

    val notificationContent: String = renderToString(freeMarkerConfig.getTemplate(n.content.template), n.content.model)
    notificationContent should be(
      """Outcomes last recorded by John Admin at 13 June 2019 at 15:20:00:
        |
        |MIT-1,000: 13 June 2019 - (ongoing)
        |
        |Affected assessments:
        |- IN304 Module in304 (18/19) - Essay (2000 words)
        |- IN305 Module in305 (18/19) - Dissertation
        |""".stripMargin
    )

    // Make sure the Markdown -> HTML is sane
    renderMarkdown(notificationContent) should be (
      """<p>Outcomes last recorded by John Admin at 13 June 2019 at 15:20:00:</p>
        |<p>MIT-1,000: 13 June 2019 - (ongoing)</p>
        |<p>Affected assessments:</p>
        |<ul><li>IN304 Module in304 (18/19) - Essay (2000 words)</li><li>IN305 Module in305 (18/19) - Dissertation</li></ul>
        |""".stripMargin
    )
  }}

  @Test
  def textIsFormattedCorrectlyWithNoAffectedAssessments(): Unit = withFakeTime(now) { new MitCircsRecordAcuteOutcomesNotificationFixture {
    submission.acuteOutcome = MitigatingCircumstancesAcuteOutcome.HandledElsewhere
    submission.affectedAssessments.clear()

    val notificationContent: String = renderToString(freeMarkerConfig.getTemplate(n.content.template), n.content.model)
    notificationContent should be(
      """Outcomes last recorded by John Admin at 13 June 2019 at 15:20:00:
        |
        |MIT-1,000: 13 June 2019 - 27 June 2019
        |
        |**Handled elsewhere**""".stripMargin
    )

    // Make sure the Markdown -> HTML is sane
    renderMarkdown(notificationContent) should be (
      """<p>Outcomes last recorded by John Admin at 13 June 2019 at 15:20:00:</p>
        |<p>MIT-1,000: 13 June 2019 - 27 June 2019</p>
        |<p><strong>Handled elsewhere</strong></p>
        |""".stripMargin
    )
  }}

  @Test
  def textIsFormattedCorrectlyWhenRejected(): Unit = withFakeTime(now) { new MitCircsRecordAcuteOutcomesNotificationFixture {
    submission.outcomeGrading = MitigatingCircumstancesGrading.Rejected
    submission.rejectionReasons = Seq(MitigatingCircumstancesRejectionReason.ReasonableAdjustmentExists, MitigatingCircumstancesRejectionReason.TimingNotAdverse)

    val notificationContent: String = renderToString(freeMarkerConfig.getTemplate(n.content.template), n.content.model)
    notificationContent should be(
      """Outcomes last recorded by John Admin at 13 June 2019 at 15:20:00:
        |
        |MIT-1,000: 13 June 2019 - 27 June 2019
        |
        |The submission was rejected - Already mitigated via a reasonable adjustment, There was insufficient evidence to show that the timing of the circumstances adversely affected the assessment.
        |""".stripMargin
    )

    // Make sure the Markdown -> HTML is sane
    renderMarkdown(notificationContent) should be (
      """<p>Outcomes last recorded by John Admin at 13 June 2019 at 15:20:00:</p>
        |<p>MIT-1,000: 13 June 2019 - 27 June 2019</p>
        |<p>The submission was rejected - Already mitigated via a reasonable adjustment, There was insufficient evidence to show that the timing of the circumstances adversely affected the assessment.</p>
        |""".stripMargin
    )
  }}

}
