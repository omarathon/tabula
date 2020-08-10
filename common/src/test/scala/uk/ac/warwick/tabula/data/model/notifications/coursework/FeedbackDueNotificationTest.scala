package uk.ac.warwick.tabula.data.model.notifications.coursework

import javax.sql.DataSource
import org.hibernate.{Session, SessionFactory}
import org.joda.time.{DateTime, DateTimeConstants}
import org.junit.{After, Before}
import org.springframework.beans.factory.config.{BeanDefinition, ConfigurableListableBeanFactory}
import org.springframework.context.ConfigurableApplicationContext
import uk.ac.warwick.spring.SpringConfigurer
import uk.ac.warwick.tabula.JavaImports._
import uk.ac.warwick.tabula._
import uk.ac.warwick.tabula.data.model.PlagiarismInvestigation.{NotInvestigated, SuspectPlagiarised}
import uk.ac.warwick.tabula.data.model.{Module, Notification, UserGroup}
import uk.ac.warwick.tabula.helpers.Tap._
import uk.ac.warwick.tabula.roles.ModuleManagerRoleDefinition
import uk.ac.warwick.tabula.services._
import uk.ac.warwick.tabula.services.permissions.PermissionsService
import uk.ac.warwick.tabula.web.views.{FreemarkerRendering, ScalaFreemarkerConfiguration}
import uk.ac.warwick.userlookup.AnonymousUser

import scala.jdk.CollectionConverters._

class FeedbackDueNotificationTest extends TestBase with Mockito with FreemarkerRendering {

  val freeMarkerConfig: ScalaFreemarkerConfiguration = newFreemarkerConfiguration()
  val extensionService: ExtensionService = smartMock[ExtensionService]
  val userLookup = new MockUserLookup()
  val moduleAndDepartmentService: ModuleAndDepartmentService = smartMock[ModuleAndDepartmentService]
  val permissionsService: PermissionsService = smartMock[PermissionsService]

  @Before def setupAppContext(): Unit = {
    val applicationContext = smartMock[ConfigurableApplicationContext]
    val beanFactory = smartMock[ConfigurableListableBeanFactory]

    applicationContext.getBeanFactory returns beanFactory

    val beanDefinition = smartMock[BeanDefinition]
    beanFactory.getBeanDefinition(anyString) returns beanDefinition
    beanDefinition.isAutowireCandidate returns true

    applicationContext.getBean("assignmentService") returns smartMock[AssessmentService]
    applicationContext.getBean("assignmentMembershipService") returns smartMock[AssessmentMembershipService]
    applicationContext.getBean("feedbackService") returns smartMock[FeedbackService]
    applicationContext.getBean("extensionService") returns extensionService
    applicationContext.getBean("markingDescriptorService") returns smartMock[MarkingDescriptorService]
    applicationContext.getBean("submissionService") returns smartMock[SubmissionService]
    val sessionFactory = smartMock[SessionFactory]
    val session = smartMock[Session]
    sessionFactory.getCurrentSession returns session
    sessionFactory.openSession() returns session
    applicationContext.getBeansOfType(classOf[SessionFactory]) returns JMap("sessionFactory" -> sessionFactory)
    applicationContext.getBean("dataSource") returns smartMock[DataSource]

    applicationContext.getBean("userLookup") returns userLookup
    applicationContext.getBeansOfType(classOf[UserLookupService]) returns JMap[String, UserLookupService]("userLookup" -> userLookup)

    applicationContext.getBeansOfType(classOf[PermissionsService]) returns JMap("permissionsService" -> permissionsService)
    applicationContext.getBeansOfType(classOf[NotificationService]) returns JMap("notificationService" -> smartMock[NotificationService])
    applicationContext.getBeansOfType(classOf[ModuleAndDepartmentService]) returns JMap("moduleAndDepartmentService" -> moduleAndDepartmentService)
    applicationContext.getBeansOfType(classOf[Features]) returns JMap("features" -> smartMock[Features])
    applicationContext.getBeansOfType(classOf[ProfileService]) returns JMap("profileService" -> smartMock[ProfileService])

    SpringConfigurer.applicationContext = applicationContext
  }

  @After def removeAppContext(): Unit = {
    SpringConfigurer.applicationContext = null
  }

  @Test def titleDueGeneral(): Unit = withFakeTime(new DateTime(2014, DateTimeConstants.SEPTEMBER, 15, 9, 39, 0, 0)) {
    val assignment = Fixtures.assignment("5,000 word essay")
    assignment.module = Fixtures.module("cs118", "Programming for Computer Scientists")
    assignment.closeDate = new DateTime(2014, DateTimeConstants.SEPTEMBER, 16, 9, 0, 0, 0)

    val notification = Notification.init(new FeedbackDueGeneralNotification, new AnonymousUser, assignment)
    notification.title should be("CS118: Feedback for \"5,000 word essay\" is due to be published")
  }

  @Test def outputGeneral(): Unit = withFakeTime(new DateTime(2014, DateTimeConstants.SEPTEMBER, 15, 9, 39, 0, 0)) {
    val assignment = Fixtures.assignment("5,000 word essay")
    assignment.module = Fixtures.module("cs118", "Programming for Computer Scientists")
    assignment.closeDate = new DateTime(2014, DateTimeConstants.SEPTEMBER, 16, 9, 0, 0, 0)

    assignment.extensionService = smartMock[ExtensionService]

    val notification = Notification.init(new FeedbackDueGeneralNotification, new AnonymousUser, assignment)

    val notificationContent = renderToString(freeMarkerConfig.getTemplate(notification.content.template), notification.content.model)
    notificationContent should be(
      """Feedback for CS118 Programming for Computer Scientists 5,000 word essay is due in 21 working days on 14 October 2014."""
    )
  }

  // TAB-3303
  @Test def outputGeneralPlural(): Unit = withFakeTime(new DateTime(2015, DateTimeConstants.FEBRUARY, 16, 17, 0, 0, 0)) {
    val assignment = Fixtures.assignment("Implementation and Report")
    assignment.module = Fixtures.module("cs404", "Agent Based Systems")
    assignment.closeDate = new DateTime(2015, DateTimeConstants.JANUARY, 20, 12, 0, 0, 0)

    assignment.extensionService = smartMock[ExtensionService]

    val notification = Notification.init(new FeedbackDueGeneralNotification, new AnonymousUser, assignment)

    val notificationContent = renderToString(freeMarkerConfig.getTemplate(notification.content.template), notification.content.model)
    notificationContent should be(
      """Feedback for CS404 Agent Based Systems Implementation and Report is due in 1 working day on 17 February 2015."""
    )
  }

  @Test def generalBatch(): Unit = withFakeTime(new DateTime(2015, DateTimeConstants.FEBRUARY, 16, 17, 0, 0, 0)) {
    val assignment1 = Fixtures.assignment("Implementation and Report")
    assignment1.module = Fixtures.module("cs404", "Agent Based Systems")
    assignment1.closeDate = new DateTime(2015, DateTimeConstants.JANUARY, 20, 12, 0, 0, 0)
    assignment1.extensionService = smartMock[ExtensionService]

    val assignment2 = Fixtures.assignment("5,000 word essay")
    assignment2.module = Fixtures.module("cs118", "Programming for Computer Scientists")
    assignment2.closeDate = new DateTime(2014, DateTimeConstants.SEPTEMBER, 16, 9, 0, 0, 0)
    assignment2.extensionService = smartMock[ExtensionService]

    val notification1 = Notification.init(new FeedbackDueGeneralNotification, new AnonymousUser, assignment1)
    val notification2 = Notification.init(new FeedbackDueGeneralNotification, new AnonymousUser, assignment2)

    val batch = Seq(notification1, notification2)

    FeedbackDueGeneralBatchedNotificationHandler.titleForBatch(batch, new AnonymousUser) should be ("Feedback for 2 assignments is due to be published")

    val content = FeedbackDueGeneralBatchedNotificationHandler.contentForBatch(batch)
    renderToString(freeMarkerConfig.getTemplate(content.template), content.model) should be (
      """
        |Feedback for 1 assignment is 125 days late:
        |
        |* Feedback for CS118 Programming for Computer Scientists 5,000 word essay is due in -81 working days on 14 October 2014.
        |
        |Feedback for 1 assignment is due tomorrow:
        |
        |* Feedback for CS404 Agent Based Systems Implementation and Report is due in 1 working day on 17 February 2015.
        |""".stripMargin
    )
  }

  @Test def titleDueExtension(): Unit = withFakeTime(new DateTime(2014, DateTimeConstants.SEPTEMBER, 17, 9, 39, 0, 0)) {
    val assignment = Fixtures.assignment("5,000 word essay")
    assignment.module = Fixtures.module("cs118", "Programming for Computer Scientists")
    assignment.closeDate = new DateTime(2014, DateTimeConstants.SEPTEMBER, 16, 9, 0, 0, 0)

    val extension = Fixtures.extension()
    extension.assignment = assignment
    extension._universityId = "1234567"
    extension.usercode = "1234567"
    extension.expiryDate = new DateTime(2014, DateTimeConstants.SEPTEMBER, 17, 9, 0, 0, 0)

    val notification = Notification.init(new FeedbackDueExtensionNotification, new AnonymousUser, extension)
    notification.title should be("CS118: Feedback for 1234567 for \"5,000 word essay\" is due to be published")
  }

  @Test def outputExtension(): Unit = withFakeTime(new DateTime(2014, DateTimeConstants.SEPTEMBER, 15, 9, 39, 0, 0)) {
    val assignment = Fixtures.assignment("5,000 word essay")
    assignment.module = Fixtures.module("cs118", "Programming for Computer Scientists")
    assignment.closeDate = new DateTime(2014, DateTimeConstants.SEPTEMBER, 15, 9, 0, 0, 0)
    val submission = Fixtures.submission("1234567", "1234567")
    submission.submittedDate = new DateTime(2014, DateTimeConstants.SEPTEMBER, 16, 9, 0, 0, 0)
    assignment.submissions.add(submission)
    val extension = Fixtures.extension()
    extension.assignment = assignment
    extension._universityId = "1234567"
    extension.usercode = "1234567"
    extension.expiryDate = new DateTime(2014, DateTimeConstants.SEPTEMBER, 17, 9, 0, 0, 0)
    extension.approve()

    val notification = Notification.init(new FeedbackDueExtensionNotification, new AnonymousUser, extension)

    val notificationContent = renderToString(freeMarkerConfig.getTemplate(notification.content.template), notification.content.model)
    notificationContent should be(
      """1234567 had an extension until 17 September 2014 for the assignment CS118 Programming for Computer Scientists 5,000 word essay. Their feedback is due in 22 working days on 15 October 2014."""
    )
  }

  // TAB-3303
  @Test def outputExtensionPlural(): Unit = withFakeTime(new DateTime(2015, DateTimeConstants.FEBRUARY, 16, 17, 0, 0, 0)) {
    val assignment = Fixtures.assignment("Implementation and Report")
    assignment.module = Fixtures.module("cs404", "Agent Based Systems")
    assignment.closeDate = new DateTime(2015, DateTimeConstants.JANUARY, 20, 12, 0, 0, 0)

    val submission = Fixtures.submission("1234567", "1234567")
    submission.submittedDate = new DateTime(2014, DateTimeConstants.SEPTEMBER, 20, 13, 0, 0, 0)
    assignment.submissions.add(submission)

    val extension = Fixtures.extension()
    extension.assignment = assignment
    extension._universityId = "1234567"
    extension.usercode = "1234567"
    extension.expiryDate = new DateTime(2015, DateTimeConstants.JANUARY, 20, 17, 0, 0, 0)
    extension.approve()

    val notification = Notification.init(new FeedbackDueExtensionNotification, new AnonymousUser, extension)

    val notificationContent = renderToString(freeMarkerConfig.getTemplate(notification.content.template), notification.content.model)
    notificationContent should be(
      """1234567 had an extension until 20 January 2015 for the assignment CS404 Agent Based Systems Implementation and Report. Their feedback is due in 1 working day on 17 February 2015."""
    )
  }

  @Test def extensionBatch(): Unit = withFakeTime(new DateTime(2015, DateTimeConstants.FEBRUARY, 16, 17, 0, 0, 0)) {
    val assignment = Fixtures.assignment("5,000 word essay")
    assignment.module = Fixtures.module("cs118", "Programming for Computer Scientists")
    assignment.closeDate = new DateTime(2014, DateTimeConstants.SEPTEMBER, 15, 9, 0, 0, 0)

    val submission1 = Fixtures.submission("1234567", "1234567")
    submission1.submittedDate = new DateTime(2014, DateTimeConstants.SEPTEMBER, 16, 9, 0, 0, 0)
    assignment.submissions.add(submission1)

    val extension1 = Fixtures.extension()
    extension1.assignment = assignment
    extension1._universityId = "1234567"
    extension1.usercode = "1234567"
    extension1.expiryDate = new DateTime(2014, DateTimeConstants.SEPTEMBER, 17, 9, 0, 0, 0)
    extension1.approve()

    val submission2 = Fixtures.submission("1234567", "1234567")
    submission2.submittedDate = new DateTime(2014, DateTimeConstants.SEPTEMBER, 20, 13, 0, 0, 0)
    assignment.submissions.add(submission2)

    val extension2 = Fixtures.extension()
    extension2.assignment = assignment
    extension2._universityId = "1234567"
    extension2.usercode = "1234567"
    extension2.expiryDate = new DateTime(2015, DateTimeConstants.JANUARY, 20, 17, 0, 0, 0)
    extension2.approve()

    val submission3 = Fixtures.submission("1234568", "1234568")
    submission3.submittedDate = new DateTime(2014, DateTimeConstants.SEPTEMBER, 20, 13, 0, 0, 0)
    assignment.submissions.add(submission3)

    val extension3 = Fixtures.extension()
    extension3.assignment = assignment
    extension3._universityId = "1234568"
    extension3.usercode = "1234568"
    extension3.expiryDate = new DateTime(2015, DateTimeConstants.JANUARY, 20, 17, 0, 0, 0)
    extension3.approve()

    val notification1 = Notification.init(new FeedbackDueExtensionNotification, new AnonymousUser, extension1)
    val notification2 = Notification.init(new FeedbackDueExtensionNotification, new AnonymousUser, extension2)
    val notification3 = Notification.init(new FeedbackDueExtensionNotification, new AnonymousUser, extension3)

    val batch = Seq(notification1, notification2, notification3)

    FeedbackDueExtensionBatchedNotificationHandler.titleForBatch(batch, new AnonymousUser) should be ("CS118: Feedback for 3 students for \"5,000 word essay\" is due to be published")

    val content = FeedbackDueExtensionBatchedNotificationHandler.contentForBatch(batch)
    renderToString(freeMarkerConfig.getTemplate(content.template), content.model) should be (
      """For the assignment CS118 Programming for Computer Scientists 5,000 word essay:
        |
        |Feedback for 1 student is 124 days late:
        |
        |* 1234567 had an extension until 17 September 2014. Their feedback is due in -80 working days on 15 October 2014.
        |
        |Feedback for 2 students is due tomorrow:
        |
        |* 1234567 had an extension until 20 January 2015. Their feedback is due in 1 working day on 17 February 2015.
        |* 1234568 had an extension until 20 January 2015. Their feedback is due in 1 working day on 17 February 2015.
        |""".stripMargin
    )
  }

  @Test def recipientsExtension(): Unit = withFakeTime(new DateTime(2014, DateTimeConstants.SEPTEMBER, 17, 9, 39, 0, 0)) {
    val assignment = Fixtures.assignment("5,000 word essay")
    assignment.module = Fixtures.module("cs118", "Programming for Computer Scientists")
    assignment.closeDate = new DateTime(2014, DateTimeConstants.SEPTEMBER, 16, 9, 0, 0, 0)

    assignment.feedbackService = smartMock[FeedbackService]
    assignment.feedbackService.loadFeedbackForAssignment(assignment) answers { _: Any => assignment.feedbacks.asScala.toSeq }

    val submission = Fixtures.submission("1234567", "1234567")
    submission.submittedDate = new DateTime(2014, DateTimeConstants.SEPTEMBER, 17, 9, 0, 0, 0)
    assignment.submissions.add(submission)

    val extension = Fixtures.extension()
    extension.assignment = assignment
    extension._universityId = "1234567"
    extension.usercode = "1234567"
    extension.expiryDate = new DateTime(2014, DateTimeConstants.SEPTEMBER, 18, 9, 0, 0, 0)
    extension.approve()
    extension.feedbackDeadline should not be Symbol("empty")

    val managers = UserGroup.ofUsercodes
    userLookup.registerUsers("cuscav", "cusebr")
    managers.addUserId("cuscav")
    managers.addUserId("cusebr")

    moduleAndDepartmentService.getModuleByCode(assignment.module.code) returns Some(assignment.module)
    permissionsService.ensureUserGroupFor[Module](assignment.module, ModuleManagerRoleDefinition) returns managers

    assignment.submissions.add(Fixtures.submission("1234567", "1234567"))

    val notification = Notification.init(new FeedbackDueExtensionNotification, new AnonymousUser, extension)
    notification.deadline should not be Symbol("empty")
    notification.recipients should not be Symbol("empty")
  }

  @Test def recipientsExtensionNoSubmissions(): Unit = withFakeTime(new DateTime(2014, DateTimeConstants.SEPTEMBER, 17, 9, 39, 0, 0)) {
    val assignment = Fixtures.assignment("5,000 word essay")
    assignment.module = Fixtures.module("cs118", "Programming for Computer Scientists")
    assignment.closeDate = new DateTime(2014, DateTimeConstants.SEPTEMBER, 16, 9, 0, 0, 0)

    val extension = Fixtures.extension()
    extension.assignment = assignment
    extension._universityId = "1234567"
    extension.usercode = "1234567"
    extension.expiryDate = new DateTime(2014, DateTimeConstants.SEPTEMBER, 17, 9, 0, 0, 0)

    val notification = Notification.init(new FeedbackDueExtensionNotification, new AnonymousUser, extension)
    notification.recipients should be(Symbol("empty"))
  }

  @Test def recipientsGeneralAlreadyReleased(): Unit = withFakeTime(new DateTime(2014, DateTimeConstants.SEPTEMBER, 17, 9, 39, 0, 0)) {
    val assignment = Fixtures.assignment("5,000 word essay")
    assignment.module = Fixtures.module("cs118", "Programming for Computer Scientists")
    assignment.closeDate = new DateTime(2014, DateTimeConstants.SEPTEMBER, 16, 9, 0, 0, 0)

    assignment.feedbackService = smartMock[FeedbackService]
    assignment.feedbackService.loadFeedbackForAssignment(assignment) answers { _: Any => assignment.feedbacks.asScala.toSeq }

    val managers = UserGroup.ofUsercodes
    userLookup.registerUsers("cuscav", "cusebr")
    managers.addUserId("cuscav")
    managers.addUserId("cusebr")

    moduleAndDepartmentService.getModuleByCode(assignment.module.code) returns Some(assignment.module)
    permissionsService.ensureUserGroupFor[Module](assignment.module, ModuleManagerRoleDefinition) returns managers

    assignment.submissions.add(Fixtures.submission("0000001", "0000001"))
    assignment.submissions.add(Fixtures.submission("0000002", "0000002"))

    assignment.needsFeedbackPublishing should be (true)

    assignment.extensionService.getApprovedExtensionsByUserId(assignment) returns Map.empty

    val notification = Notification.init(new FeedbackDueGeneralNotification, new AnonymousUser, assignment)
    notification.recipients should not be Symbol("empty")

    // Feedback added
    assignment.feedbacks.add(Fixtures.assignmentFeedback("0000001", "0000001").tap { f => f.assignment = assignment; f.actualMark = Some(80) })
    assignment.feedbacks.add(Fixtures.assignmentFeedback("0000002", "0000002").tap { f => f.assignment = assignment; f.actualMark = Some(70) })
    notification.recipients should not be Symbol("empty")

    // Feedback partially released
    assignment.feedbacks.get(0).released = true
    notification.recipients should not be Symbol("empty")

    // Feedback released for all assignments without plagiarism investigation
    assignment.submissions.get(1).plagiarismInvestigation = SuspectPlagiarised
    assignment.needsFeedbackPublishing should be (false)
    notification.recipients should be(Symbol("empty"))
    assignment.submissions.get(1).plagiarismInvestigation = NotInvestigated

    // Feedback fully released
    assignment.feedbacks.get(1).released = true
    assignment.needsFeedbackPublishing should be (false)
    notification.recipients should be(Symbol("empty"))
  }

  @Test def recipientsExtensionAlreadyReleased(): Unit = withFakeTime(new DateTime(2014, DateTimeConstants.SEPTEMBER, 17, 9, 39, 0, 0)) {
    val assignment = Fixtures.assignment("5,000 word essay")
    assignment.module = Fixtures.module("cs118", "Programming for Computer Scientists")
    assignment.closeDate = new DateTime(2014, DateTimeConstants.SEPTEMBER, 14, 9, 0, 0, 0)

    assignment.feedbackService = smartMock[FeedbackService]
    assignment.feedbackService.loadFeedbackForAssignment(assignment) answers { _: Any => assignment.feedbacks.asScala.toSeq }

    val submission1 = Fixtures.submission("0000001", "0000001")
    val submission2 = Fixtures.submission("0000002", "0000002")
    submission1.submittedDate = new DateTime(2014, DateTimeConstants.SEPTEMBER, 15, 9, 0, 0, 0)
    submission2.submittedDate = new DateTime(2014, DateTimeConstants.SEPTEMBER, 15, 9, 0, 0, 0)
    assignment.submissions.add(submission1)
    assignment.submissions.add(submission2)

    val managers = UserGroup.ofUsercodes
    userLookup.registerUsers("cuscav", "cusebr")
    managers.addUserId("cuscav")
    managers.addUserId("cusebr")

    moduleAndDepartmentService.getModuleByCode(assignment.module.code) returns Some(assignment.module)
    permissionsService.ensureUserGroupFor[Module](assignment.module, ModuleManagerRoleDefinition) returns managers

    val extension = Fixtures.extension()
    extension.assignment = assignment
    extension._universityId = "1234567"
    extension.usercode = "1234567"
    extension.expiryDate = new DateTime(2014, DateTimeConstants.SEPTEMBER, 17, 9, 0, 0, 0)
    extension.approve()
    extension.feedbackDeadline should be(Symbol("empty"))

    val submission3 = Fixtures.submission("1234567", "1234567")
    submission3.submittedDate = new DateTime(2014, DateTimeConstants.SEPTEMBER, 15, 9, 0, 0, 0)
    assignment.submissions.add(submission3)

    val notification = Notification.init(new FeedbackDueExtensionNotification, new AnonymousUser, extension)
    notification.deadline should not be Symbol("empty")
    notification.recipients should not be Symbol("empty")

    // Feedback added
    assignment.feedbacks.add(Fixtures.assignmentFeedback("1234567", "1234567").tap { f => f.assignment = assignment; f.actualMark = Some(80) })
    notification.recipients should not be Symbol("empty")

    // Feedback fully released
    assignment.feedbacks.get(0).released = true
    assignment.needsFeedbackPublishing should be (true) // We still haven't released the others
    notification.recipients should be(Symbol("empty"))
  }

}
