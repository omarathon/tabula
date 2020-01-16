package uk.ac.warwick.tabula.data.model.notifications.coursework

import org.joda.time.{DateTime, DateTimeConstants}
import uk.ac.warwick.tabula.data.model.forms.Extension
import uk.ac.warwick.tabula.data.model.{Assignment, Notification, Submission}
import uk.ac.warwick.tabula.services.{AssessmentMembershipService, ExtensionService, IncludeType, MembershipItem, UserLookupService}
import uk.ac.warwick.tabula.web.views.{FreemarkerRendering, ScalaFreemarkerConfiguration}
import uk.ac.warwick.tabula.{Fixtures, Mockito, TestBase}
import uk.ac.warwick.userlookup.{AnonymousUser, User}

class SubmissionDueNotificationTest extends TestBase with Mockito with FreemarkerRendering {

  val freeMarkerConfig: ScalaFreemarkerConfiguration = newFreemarkerConfiguration()

  val users = Seq(
    Fixtures.user(universityId = "0123456", userId = "0123456"),
    Fixtures.user(universityId = "0133454", userId = "0133454")
  )

  val assignment = new Assignment
  assignment.extensionService = smartMock[ExtensionService]
  assignment.extensionService.getApprovedExtensionsByUserId(assignment) returns Map.empty

  assignment.collectSubmissions = true
  assignment.openEnded = false
  assignment.closeDate = DateTime.now.plusDays(1)

  @Test
  def generalRecipients(): Unit = {
    val notification = new SubmissionDueGeneralNotification {
      override def assignment: Assignment = SubmissionDueNotificationTest.this.assignment
    }

    val membershipService = smartMock[AssessmentMembershipService]
    membershipService.determineMembershipUsers(assignment) returns users
    notification.membershipService = membershipService

    notification.recipients should be(users)

    // Don't notify them if they've submitted
    withClue("Shouldn't notify user who has submitted") {
      val submission = new Submission
      submission._universityId = "0133454"
      submission.usercode = "0133454"
      assignment.addSubmission(submission)
      notification.recipients should be(Seq(users.head))
    }

    withClue("Shouldn't notify user with extension") { // A different class handles individual extensions
      val extension = new Extension
      extension._universityId = "0123456"
      extension.usercode = "0123456"
      extension.approve()
      assignment.addExtension(extension)

      assignment.extensionService = smartMock[ExtensionService]
      assignment.extensionService.getApprovedExtensionsByUserId(assignment) returns Map("0123456" -> extension)

      notification.recipients should be(Seq())
    }
  }

  @Test
  def extensionRecipients(): Unit = {
    val anExtension = new Extension
    anExtension._universityId = "0133454"
    anExtension.usercode = "u0133454"

    val notification = new SubmissionDueWithExtensionNotification {
      override def extension: Extension = anExtension
    }
    notification.userLookup = mock[UserLookupService]
    notification.userLookup.getUserByUserId("u0133454") returns users(1)
    assignment.addExtension(anExtension)

    assignment.extensionService = smartMock[ExtensionService]
    assignment.extensionService.getAllExtensionsByUserId(assignment) returns Map(anExtension.usercode -> Seq(anExtension))
    assignment.extensionService.getApprovedExtensionsByUserId(assignment) returns Map(anExtension.usercode -> anExtension)

    withClue("Shouldn't send if the extension hasn't been approved") {
      notification.recipients should be(Symbol("empty"))
    }

    anExtension.approve()
    anExtension.expiryDate = DateTime.now.plusWeeks(1)

    withClue("Should only be sent to the one user who has an extension") {
      notification.recipients should be(Seq(users(1)))
    }

    withClue("Shouldn't be sent if the extension's expiry date is before the assignment close date") {
      assignment.extensionService.getAllExtensionsByUserId(assignment) returns Map(anExtension.usercode -> Seq(anExtension))
      assignment.extensionService.getApprovedExtensionsByUserId(assignment) returns Map(anExtension.usercode -> anExtension)
      assignment.closeDate = DateTime.now.plusWeeks(3)
      notification.recipients should be(Seq())
    }

    assignment.closeDate = DateTime.now.plusDays(1)

    withClue("Shouldn't be sent if there is a more recent, approved extension") {
      val laterExtension = new Extension
      laterExtension._universityId = "0133454"
      laterExtension.usercode = "u0133454"
      assignment.addExtension(laterExtension)
      laterExtension.approve()
      assignment.extensionService.getApprovedExtensionsByUserId(assignment) returns Map(laterExtension.usercode -> laterExtension)
      notification.recipients should be(Seq())
    }

    withClue("Should be sent if there is another approved extension, but that one is less recent") {
      val earlierExtension = new Extension
      earlierExtension._universityId = "0133454"
      earlierExtension.usercode = "u0133454"
      assignment.addExtension(earlierExtension)
      earlierExtension.approve()
      assignment.extensionService.getAllExtensionsByUserId(assignment) returns Map(anExtension.usercode -> Seq(anExtension, earlierExtension))
      assignment.extensionService.getApprovedExtensionsByUserId(assignment) returns Map(earlierExtension.usercode -> earlierExtension, anExtension.usercode -> anExtension)
      notification.recipients should be(Seq(users(1)))
    }

    withClue("Shouldn't be sent if extension user has submitted") {
      val submission = new Submission
      submission._universityId = "0133454"
      submission.usercode = "u0133454"
      assignment.addSubmission(submission)
      notification.recipients should be(Seq())
    }
  }

  private def membershipItem(uniId: String, usercode: String) = {
    val user = new User(usercode)
    user.setWarwickId(uniId)
    MembershipItem(user, Some(uniId), Some(usercode), IncludeType, extraneous = false)
  }

  @Test def titleDueGeneral(): Unit = withFakeTime(new DateTime(2014, DateTimeConstants.SEPTEMBER, 15, 9, 39, 0, 0)) {
    val assignment = Fixtures.assignment("5,000 word essay")
    assignment.module = Fixtures.module("cs118", "Programming for Computer Scientists")
    assignment.closeDate = new DateTime(2014, DateTimeConstants.SEPTEMBER, 16, 9, 0, 0, 0)

    val notification = Notification.init(new SubmissionDueGeneralNotification, new AnonymousUser, assignment)
    notification.title should be("CS118: Your submission for '5,000 word essay' is due tomorrow")
  }

  @Test def titleLateGeneral(): Unit = withFakeTime(new DateTime(2014, DateTimeConstants.SEPTEMBER, 18, 9, 39, 0, 0)) {
    val assignment = Fixtures.assignment("5,000 word essay")
    assignment.module = Fixtures.module("cs118", "Programming for Computer Scientists")
    assignment.closeDate = new DateTime(2014, DateTimeConstants.SEPTEMBER, 16, 9, 0, 0, 0)

    val notification = Notification.init(new SubmissionDueGeneralNotification, new AnonymousUser, assignment)
    notification.title should be("CS118: Your submission for '5,000 word essay' is 2 days late")
  }

  @Test def titleDueExtension(): Unit = withFakeTime(new DateTime(2014, DateTimeConstants.SEPTEMBER, 17, 9, 39, 0, 0)) {
    val assignment = Fixtures.assignment("5,000 word essay")
    assignment.module = Fixtures.module("cs118", "Programming for Computer Scientists")
    assignment.closeDate = new DateTime(2014, DateTimeConstants.SEPTEMBER, 16, 9, 0, 0, 0)

    val extension = Fixtures.extension()
    extension.assignment = assignment
    extension.expiryDate = new DateTime(2014, DateTimeConstants.SEPTEMBER, 17, 9, 0, 0, 0)
    extension.approve()

    val notification = Notification.init(new SubmissionDueWithExtensionNotification, new AnonymousUser, extension)
    notification.title should be("CS118: Your submission for '5,000 word essay' is due today")
  }

  @Test def titleLateExtension(): Unit = withFakeTime(new DateTime(2014, DateTimeConstants.SEPTEMBER, 18, 9, 39, 0, 0)) {
    val assignment = Fixtures.assignment("5,000 word essay")
    assignment.module = Fixtures.module("cs118", "Programming for Computer Scientists")
    assignment.closeDate = new DateTime(2014, DateTimeConstants.SEPTEMBER, 16, 9, 0, 0, 0)

    val extension = Fixtures.extension()
    extension.assignment = assignment
    extension.expiryDate = new DateTime(2014, DateTimeConstants.SEPTEMBER, 17, 9, 0, 0, 0)
    extension.approve()

    val notification = Notification.init(new SubmissionDueWithExtensionNotification, new AnonymousUser, extension)
    notification.title should be("CS118: Your submission for '5,000 word essay' is 1 day late")
  }

  private[this] trait BatchedNotificationsDueFixture {
    val assignment1 = Fixtures.assignment("5,000 word essay")
    assignment1.module = Fixtures.module("cs118", "Programming for Computer Scientists")
    assignment1.closeDate = DateTime.now.plusDays(1).withTime(Assignment.closeTime)

    val assignment2 = Fixtures.assignment("10,000 word essay")
    assignment2.module = Fixtures.module("cs118", "Programming for Computer Scientists")
    assignment2.closeDate = DateTime.now.plusDays(1).withTime(Assignment.closeTime)

    val assignment3 = Fixtures.assignment("5,000 word essay")
    assignment3.module = Fixtures.module("cs123", "Talking to Computer Scientists")
    assignment3.closeDate = DateTime.now.plusDays(3).withTime(Assignment.closeTime)

    val notification1 = Notification.init(new SubmissionDueGeneralNotification, new AnonymousUser, assignment1)
    val notification2 = Notification.init(new SubmissionDueGeneralNotification, new AnonymousUser, assignment2)
    val notification3 = Notification.init(new SubmissionDueGeneralNotification, new AnonymousUser, assignment3)

    val batch = Seq(notification1, notification2, notification3)
  }

  private[this] trait BatchedNotificationsLateFixture {
    val assignment1 = Fixtures.assignment("5,000 word essay")
    assignment1.module = Fixtures.module("cs118", "Programming for Computer Scientists")
    assignment1.closeDate = DateTime.now.minusDays(1).withTime(Assignment.closeTime)

    val assignment2 = Fixtures.assignment("10,000 word essay")
    assignment2.module = Fixtures.module("cs118", "Programming for Computer Scientists")
    assignment2.allowLateSubmissions = false
    assignment2.closeDate = DateTime.now.minusDays(1).withTime(Assignment.closeTime)

    val assignment3 = Fixtures.assignment("5,000 word essay")
    assignment3.module = Fixtures.module("cs123", "Talking to Computer Scientists")
    assignment3.closeDate = DateTime.now.minusDays(3).withTime(Assignment.closeTime)

    val notification1 = Notification.init(new SubmissionDueGeneralNotification, new AnonymousUser, assignment1)
    val notification2 = Notification.init(new SubmissionDueGeneralNotification, new AnonymousUser, assignment2)
    val notification3 = Notification.init(new SubmissionDueGeneralNotification, new AnonymousUser, assignment3)

    val batch = Seq(notification1, notification2, notification3)
  }

  private[this] trait BatchedNotificationsMixedFixture {
    val assignment1 = Fixtures.assignment("5,000 word essay")
    assignment1.module = Fixtures.module("cs118", "Programming for Computer Scientists")
    assignment1.closeDate = DateTime.now.minusDays(1).withTime(Assignment.closeTime)

    val assignment2 = Fixtures.assignment("10,000 word essay")
    assignment2.module = Fixtures.module("cs118", "Programming for Computer Scientists")
    assignment2.closeDate = DateTime.now.minusDays(1).withTime(Assignment.closeTime)
    assignment2.allowLateSubmissions = false

    val assignment3 = Fixtures.assignment("5,000 word essay")
    assignment3.module = Fixtures.module("cs123", "Talking to Computer Scientists")
    assignment3.closeDate = DateTime.now.plusDays(3).withTime(Assignment.closeTime)

    val notification1 = Notification.init(new SubmissionDueGeneralNotification, new AnonymousUser, assignment1)
    val notification2 = Notification.init(new SubmissionDueGeneralNotification, new AnonymousUser, assignment2)
    val notification3 = Notification.init(new SubmissionDueGeneralNotification, new AnonymousUser, assignment3)

    val batch = Seq(notification1, notification2, notification3)
  }

  @Test def batchedTitleDue(): Unit = withFakeTime(new DateTime(2014, DateTimeConstants.SEPTEMBER, 18, 9, 39, 0, 0)) {
    new BatchedNotificationsDueFixture {
      notification1.titleForBatch(batch, new AnonymousUser) should be("Your submissions for 2 assignments are due tomorrow (+ 1 other)")
    }
  }

  @Test def batchedTitleLate(): Unit = withFakeTime(new DateTime(2014, DateTimeConstants.SEPTEMBER, 18, 9, 39, 0, 0)) {
    new BatchedNotificationsLateFixture {
      notification1.titleForBatch(batch, new AnonymousUser) should be("CS123: Your submission for '5,000 word essay' is 3 days late (+ 2 others)")
    }
  }

  @Test def batchedTitleMixed(): Unit = withFakeTime(new DateTime(2014, DateTimeConstants.SEPTEMBER, 18, 9, 39, 0, 0)) {
    new BatchedNotificationsMixedFixture {
      notification1.titleForBatch(batch, new AnonymousUser) should be("Your submissions for 2 assignments are 1 day late (+ 1 other)")
    }
  }

  @Test def batchedContentDue(): Unit = withFakeTime(new DateTime(2014, DateTimeConstants.SEPTEMBER, 18, 9, 39, 0, 0)) {
    new BatchedNotificationsDueFixture {
      val content = notification1.contentForBatch(batch)

      renderToString(freeMarkerConfig.getTemplate(content.template), content.model) should be(
        """
          |Your submissions for 2 assignments are due tomorrow:
          |
          |* '10,000 word essay' for CS118 Programming for Computer Scientists. Your deadline for this assignment is 19 September 2014 at 12:00:00.
          |* '5,000 word essay' for CS118 Programming for Computer Scientists. Your deadline for this assignment is 19 September 2014 at 12:00:00.
          |
          |Your submission for 1 assignment is due in 3 days:
          |
          |* '5,000 word essay' for CS123 Talking to Computer Scientists. Your deadline for this assignment is 21 September 2014 at 12:00:00.
          |""".stripMargin
      )
    }
  }

  @Test def batchedContentLate(): Unit = withFakeTime(new DateTime(2014, DateTimeConstants.SEPTEMBER, 18, 9, 39, 0, 0)) {
    new BatchedNotificationsLateFixture {
      val content = notification1.contentForBatch(batch)

      renderToString(freeMarkerConfig.getTemplate(content.template), content.model) should be(
        """
          |Your submission for 1 assignment is 3 days late:
          |
          |* '5,000 word essay' for CS123 Talking to Computer Scientists. Your deadline for this assignment was 15 September 2014 at 12:00:00.
          |
          |Your submissions for 2 assignments are 1 day late:
          |
          |* '10,000 word essay' for CS118 Programming for Computer Scientists. Your deadline for this assignment was 17 September 2014 at 12:00:00. You can no longer submit this assignment via Tabula.
          |* '5,000 word essay' for CS118 Programming for Computer Scientists. Your deadline for this assignment was 17 September 2014 at 12:00:00.
          |""".stripMargin
      )
    }
  }

  @Test def batchedContentMixed(): Unit = withFakeTime(new DateTime(2014, DateTimeConstants.SEPTEMBER, 18, 9, 39, 0, 0)) {
    new BatchedNotificationsMixedFixture {
      val content = notification1.contentForBatch(batch)

      renderToString(freeMarkerConfig.getTemplate(content.template), content.model) should be(
        """
          |Your submissions for 2 assignments are 1 day late:
          |
          |* '10,000 word essay' for CS118 Programming for Computer Scientists. Your deadline for this assignment was 17 September 2014 at 12:00:00. You can no longer submit this assignment via Tabula.
          |* '5,000 word essay' for CS118 Programming for Computer Scientists. Your deadline for this assignment was 17 September 2014 at 12:00:00.
          |
          |Your submission for 1 assignment is due in 3 days:
          |
          |* '5,000 word essay' for CS123 Talking to Computer Scientists. Your deadline for this assignment is 21 September 2014 at 12:00:00.
          |""".stripMargin
      )
    }
  }

}
