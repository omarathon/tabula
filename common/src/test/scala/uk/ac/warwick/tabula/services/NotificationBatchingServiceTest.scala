package uk.ac.warwick.tabula.services

import java.util.UUID

import org.hibernate.Session
import org.joda.time.{DateTime, DateTimeConstants}
import uk.ac.warwick.tabula._
import uk.ac.warwick.tabula.data.model.notifications.RecipientNotificationInfo
import uk.ac.warwick.tabula.data.model._
import uk.ac.warwick.tabula.data.{NotificationDao, NotificationDaoComponent, SessionComponent}
import uk.ac.warwick.tabula.notifications.{EmailNotificationListenerComponent, MyWarwickNotificationListenerComponent}
import uk.ac.warwick.userlookup.User

import scala.concurrent.duration._

class NotificationBatchingServiceTest extends TestBase with Mockito {

  private[this] val referenceDate = new DateTime(2020, DateTimeConstants.JANUARY, 15, 12, 54, 54, 0)

  private[this] trait TestSupport
    extends NotificationDaoComponent
      with EmailNotificationListenerComponent
      with MyWarwickNotificationListenerComponent
      with UserSettingsServiceComponent
      with FeaturesComponent
      with SessionComponent {
    override val notificationDao: NotificationDao = smartMock[NotificationDao]
    override val emailNotificationListener: BatchingRecipientNotificationListener = smartMock[BatchingRecipientNotificationListener]
    override val myWarwickNotificationListener: BatchingRecipientNotificationListener = smartMock[BatchingRecipientNotificationListener]
    override val userSettingsService: UserSettingsService = smartMock[UserSettingsService]
    override val features: Features = emptyFeatures
    override val session: Session = smartMock[Session]
  }

  private[this] trait Fixture {
    val notificationBatchingService = new AbstractNotificationBatchingService with TestSupport
    notificationBatchingService.features.notificationBatching = true

    val recipient: User = Fixtures.user()
    val userSettings: UserSettings = new UserSettings
    userSettings.batchedNotifications = 30.minutes

    notificationBatchingService.userSettingsService.getByUserId(recipient.getUserId) returns Some(userSettings)

    val agent: User = Fixtures.user("1234567", "u1234567")
    val meeting: MeetingRecord = new MeetingRecord
  }

  private[this] trait BatchingFixture extends Fixture {
    val notification1: HeronWarningNotification = Notification.init(new HeronWarningNotification, agent, meeting)
    notification1.created = referenceDate.minusMinutes(35)

    val notification2: HeronWarningNotification = Notification.init(new HeronWarningNotification, agent, meeting)
    notification2.created = referenceDate.minusMinutes(25)

    val notification3: HeronWarningNotification = Notification.init(new HeronWarningNotification, agent, meeting)
    notification3.created = referenceDate.minusMinutes(15)

    val rni1 = new RecipientNotificationInfo(notification1, recipient)
    rni1.id = UUID.randomUUID().toString

    val rni2 = new RecipientNotificationInfo(notification2, recipient)
    rni2.id = UUID.randomUUID().toString

    val rni3 = new RecipientNotificationInfo(notification3, recipient)
    rni3.id = UUID.randomUUID().toString

    notificationBatchingService.notificationDao.unemailedRecipientsByNotificationType(NotificationBatchingService.RunBatchSize) returns Seq(
      (recipient, "HeronWarning", notification1.created)
    )
    notificationBatchingService.notificationDao.unemailedNotificationsFor(isEq(recipient), isEq("HeronWarning"), anyBoolean) returns Seq(
      rni1, rni2, rni3
    )

    notificationBatchingService.session.load(classOf[RecipientNotificationInfo], rni1.id) returns rni1
    notificationBatchingService.session.load(classOf[RecipientNotificationInfo], rni2.id) returns rni2
    notificationBatchingService.session.load(classOf[RecipientNotificationInfo], rni3.id) returns rni3
  }

  private[this] trait NonBatchingFixture extends Fixture {
    val notification1: HeronDefeatedNotification = Notification.init(new HeronDefeatedNotification, agent, meeting)
    notification1.created = referenceDate.minusMinutes(35)

    val notification2: HeronDefeatedNotification = Notification.init(new HeronDefeatedNotification, agent, meeting)
    notification2.created = referenceDate.minusMinutes(25)

    val notification3: HeronDefeatedNotification = Notification.init(new HeronDefeatedNotification, agent, meeting)
    notification3.created = referenceDate.minusMinutes(15)

    val rni1 = new RecipientNotificationInfo(notification1, recipient)
    rni1.id = UUID.randomUUID().toString

    val rni2 = new RecipientNotificationInfo(notification2, recipient)
    rni2.id = UUID.randomUUID().toString

    val rni3 = new RecipientNotificationInfo(notification3, recipient)
    rni3.id = UUID.randomUUID().toString

    notificationBatchingService.notificationDao.unemailedRecipientsByNotificationType(NotificationBatchingService.RunBatchSize) returns Seq(
      (recipient, "HeronDefeat", notification1.created)
    )
    notificationBatchingService.notificationDao.unemailedNotificationsFor(isEq(recipient), isEq("HeronDefeat"), anyBoolean) returns Seq(
      rni1, rni2, rni3
    )

    notificationBatchingService.session.load(classOf[RecipientNotificationInfo], rni1.id) returns rni1
    notificationBatchingService.session.load(classOf[RecipientNotificationInfo], rni2.id) returns rni2
    notificationBatchingService.session.load(classOf[RecipientNotificationInfo], rni3.id) returns rni3
  }

  @Test def batching(): Unit = withFakeTime(referenceDate) { new BatchingFixture {
    notificationBatchingService.processNotifications()

    verify(notificationBatchingService.emailNotificationListener, times(1)).listenBatch(Seq(rni1, rni2, rni3))
    verifyNoMoreInteractions(notificationBatchingService.emailNotificationListener)
  }}

  @Test def batchingSendImmediately(): Unit = withFakeTime(referenceDate) { new BatchingFixture {
    userSettings.batchedNotifications = Duration.Zero

    notificationBatchingService.processNotifications()

    verify(notificationBatchingService.emailNotificationListener, times(1)).listen(rni1)
    verify(notificationBatchingService.emailNotificationListener, times(1)).listen(rni2)
    verify(notificationBatchingService.emailNotificationListener, times(1)).listen(rni3)
    verifyNoMoreInteractions(notificationBatchingService.emailNotificationListener)
  }}

  @Test def batchingFeatureDisabled(): Unit = withFakeTime(referenceDate) { new BatchingFixture {
    notificationBatchingService.features.notificationBatching = false

    notificationBatchingService.processNotifications()

    verify(notificationBatchingService.emailNotificationListener, times(1)).listen(rni1)
    verify(notificationBatchingService.emailNotificationListener, times(1)).listen(rni2)
    verify(notificationBatchingService.emailNotificationListener, times(1)).listen(rni3)
    verifyNoMoreInteractions(notificationBatchingService.emailNotificationListener)
  }}

  @Test def batchingNotificationNotBatchable(): Unit = withFakeTime(referenceDate) { new NonBatchingFixture {
    notificationBatchingService.processNotifications()

    verify(notificationBatchingService.emailNotificationListener, times(1)).listen(rni1)
    verify(notificationBatchingService.emailNotificationListener, times(1)).listen(rni2)
    verify(notificationBatchingService.emailNotificationListener, times(1)).listen(rni3)
    verifyNoMoreInteractions(notificationBatchingService.emailNotificationListener)
  }}

  @Test def batchingWithinDelay(): Unit = withFakeTime(referenceDate) { new BatchingFixture {
    userSettings.batchedNotifications = 1.hour

    notificationBatchingService.processNotifications()

    verifyNoMoreInteractions(notificationBatchingService.emailNotificationListener)
  }}

}
