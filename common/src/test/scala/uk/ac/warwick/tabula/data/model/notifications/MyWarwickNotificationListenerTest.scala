package uk.ac.warwick.tabula.data.model.notifications

import org.joda.time.DateTime
import org.quartz.Scheduler
import uk.ac.warwick.tabula._
import uk.ac.warwick.tabula.commands.CurrentAcademicYear
import uk.ac.warwick.tabula.data.model._
import uk.ac.warwick.tabula.data.model.notifications.cm2.Cm2StudentFeedbackAdjustmentNotification
import uk.ac.warwick.tabula.data.model.notifications.coursework.FeedbackPublishedNotification
import uk.ac.warwick.tabula.notifications.{MyWarwickNotificationListener, MyWarwickServiceComponent}
import uk.ac.warwick.tabula.services.scheduling.SchedulerComponent
import uk.ac.warwick.tabula.services.{NotificationService, NotificationServiceComponent}
import uk.ac.warwick.tabula.web.views.{TextRenderer, TextRendererComponent}
import uk.ac.warwick.userlookup.User
import uk.ac.warwick.util.mywarwick.MyWarwickService
import uk.ac.warwick.util.mywarwick.model.request.Activity

class MyWarwickNotificationListenerTest extends TestBase with Mockito {

  trait TestSupport
    extends TextRendererComponent
      with FeaturesComponent
      with MyWarwickServiceComponent
      with TopLevelUrlComponent
      with SchedulerComponent
      with NotificationServiceComponent {
    val textRenderer: TextRenderer = smartMock[TextRenderer]
    val features: Features = emptyFeatures
    val myWarwickService: MyWarwickService = smartMock[MyWarwickService]
    val scheduler: Scheduler = smartMock[Scheduler]
    val toplevelUrl: String = ""
    val notificationService: NotificationService = smartMock[NotificationService]
  }

  val listener = new MyWarwickNotificationListener with TestSupport

  trait Fixture extends CurrentAcademicYear {
    val user = new User("cusxad")
    user.setFoundUser(true)

    val currentUser = new CurrentUser(user, user)
    val feedback = new Feedback

    val module: Module = Fixtures.module(code = "ls101")
    val assignment: Assignment = Fixtures.assignment("test")
    assignment.id = "12345"
    assignment.academicYear = academicYear
    assignment.closeDate = DateTime.now.plusDays(30)
    assignment.members = UserGroup.ofUsercodes
    assignment.module = module
    module.assignments.add(assignment)
    feedback.assignment = assignment
    assignment.feedbacks.add(feedback)
  }

  @Test def alertNotifcation(): Unit = {
    new Fixture {
      val fpn = new FeedbackPublishedNotification
      val rn = new RecipientNotificationInfo(fpn, user)
      fpn.recipientNotificationInfos.add(rn)
      val n: FeedbackPublishedNotification = Notification.init(fpn, currentUser.apparentUser, Seq(feedback), feedback.assignment)
      listener.listen(rn)
      verify(listener.myWarwickService, times(1)).queueNotification(any[Activity], isEq(listener.scheduler))
    }
  }


  @Test def activitiyNotifcation(): Unit = {
    new Fixture {
      val sfan = new Cm2StudentFeedbackAdjustmentNotification
      val rn = new RecipientNotificationInfo(sfan, user)
      sfan.recipientNotificationInfos.add(rn)
      val n: Cm2StudentFeedbackAdjustmentNotification = Notification.init(sfan, currentUser.apparentUser, Seq(feedback), feedback.assignment)
      listener.listen(rn)
      verify(listener.myWarwickService, times(1)).queueActivity(any[Activity], isEq(listener.scheduler))
    }
  }

}
