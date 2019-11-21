package uk.ac.warwick.tabula.data.model.notifications

import org.joda.time.DateTime
import org.quartz.Scheduler
import uk.ac.warwick.tabula._
import uk.ac.warwick.tabula.commands.CurrentAcademicYear
import uk.ac.warwick.tabula.data.model._
import uk.ac.warwick.tabula.data.model.notifications.cm2.Cm2StudentFeedbackAdjustmentNotification
import uk.ac.warwick.tabula.data.model.notifications.coursework.{FeedbackPublishedNotification, SubmissionDueGeneralNotification}
import uk.ac.warwick.tabula.notifications.{MyWarwickNotificationListener, MyWarwickServiceComponent}
import uk.ac.warwick.tabula.services.scheduling.SchedulerComponent
import uk.ac.warwick.tabula.web.Routes
import uk.ac.warwick.tabula.web.views.{TextRenderer, TextRendererComponent}
import uk.ac.warwick.userlookup.{AnonymousUser, User}
import uk.ac.warwick.util.mywarwick.MyWarwickService
import uk.ac.warwick.util.mywarwick.model.request.Activity

import scala.jdk.CollectionConverters._


class MyWarwickNotificationListenerTest extends TestBase with Mockito {

  var listener: MyWarwickNotificationListener with TextRendererComponent with FeaturesComponent with MyWarwickServiceComponent with TopLevelUrlComponent with SchedulerComponent =
    new MyWarwickNotificationListener with TextRendererComponent with FeaturesComponent with MyWarwickServiceComponent with TopLevelUrlComponent with SchedulerComponent {

    def textRenderer: TextRenderer = smartMock[TextRenderer]

    def features: Features = emptyFeatures

    var myWarwickService: MyWarwickService = smartMock[MyWarwickService]

    var scheduler: Scheduler = smartMock[Scheduler]

    def toplevelUrl: String = ""
  }

  trait Fixture extends CurrentAcademicYear {
    val cm2Prefix = "cm2"
    Routes.cm2._cm2Prefix = Some(cm2Prefix)

    val user = new User("cusxad")
    user.setFoundUser(true)

    val currentUser = new CurrentUser(user, user)
    val feedback = new AssignmentFeedback

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

  @Test def alertNotifcation() {
    new Fixture {
      val fpn = new FeedbackPublishedNotification
      val rn = new RecipientNotificationInfo(fpn, user)
      fpn.recipientNotificationInfos.add(rn)
      val n: FeedbackPublishedNotification = Notification.init(fpn, currentUser.apparentUser, Seq(feedback), feedback.assignment)
      listener.listen(n)
      verify(listener.myWarwickService, times(1)).sendAsNotification(any[Activity])
    }
  }


  @Test def activitiyNotifcation() {
    new Fixture {
      val sfan = new Cm2StudentFeedbackAdjustmentNotification
      val rn = new RecipientNotificationInfo(sfan, user)
      sfan.recipientNotificationInfos.add(rn)
      val n: Cm2StudentFeedbackAdjustmentNotification = Notification.init(sfan, currentUser.apparentUser, Seq(feedback), feedback.assignment)
      listener.listen(n)
      verify(listener.myWarwickService, times(1)).sendAsActivity(any[Activity])
    }
  }

  @Test def noDupes() {
    new Fixture {
      val notification = new SubmissionDueGeneralNotification

      val user2 = new User("cuslaj")
      user2.setFoundUser(true)
      val user3 = new User("cuscav")
      user3.setFoundUser(true)

      val rn = new RecipientNotificationInfo(notification, user)
      val rn2 = new RecipientNotificationInfo(notification, user2)
      val rn3 = new RecipientNotificationInfo(notification, user3)

      notification.recipientNotificationInfos.addAll(Seq(rn, rn2, rn3).asJava)
      val n: SubmissionDueGeneralNotification = Notification.init(notification, new AnonymousUser, assignment)

      val activities: Seq[Activity] = listener.toMyWarwickActivities(n)
      val recipients: Seq[String] = activities.flatMap(_.getRecipients.getUsers.asScala.toSeq)
      recipients.size should be (recipients.toSet.size)

      listener.listen(n)
      verify(listener.myWarwickService, times(3)).sendAsNotification(any[Activity])
    }
  }

}
