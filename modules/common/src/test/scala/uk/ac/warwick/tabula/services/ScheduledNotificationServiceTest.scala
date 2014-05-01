package uk.ac.warwick.tabula.services

import uk.ac.warwick.tabula.{Mockito, TestBase}
import uk.ac.warwick.tabula.data.model.{ToEntityReference, Notification, HeronWarningNotification, Heron, ScheduledNotification}
import org.joda.time.DateTime
import uk.ac.warwick.tabula.data.{Scrollable, ScheduledNotificationDao}
import org.mockito.Mockito._
import org.hibernate.{SessionFactory, Session, ScrollableResults}
import java.sql.{Clob, Blob}
import java.util.{Locale, TimeZone, Calendar, Date}
import java.math.{BigDecimal, BigInteger}
import java.lang.{Double, Float, Long, Byte, Short}
import java.lang
import org.hibernate.`type`.Type

class ScheduledNotificationServiceTest extends TestBase with Mockito {

	val service = new ScheduledNotificationServiceImpl
	val dao =  mock[ScheduledNotificationDao]
	val notificationService = mock[NotificationService]
	service.dao = dao
	service.notificationService = notificationService

	val sessionFactory = mock[SessionFactory]
	val session = mock[Session]

	sessionFactory.getCurrentSession() returns (session)

	service.sessionFactory = sessionFactory

	val heron = new Heron()
	val sn1 = new ScheduledNotification("HeronWarning", heron, DateTime.now.minusDays(1))
	val sn2 = new ScheduledNotification("HeronDefeat", heron, DateTime.now.minusDays(2))
	val sn3 = new ScheduledNotification("HeronWarning", heron, DateTime.now.minusDays(3))

	val scheduledNotifications = Seq(sn1, sn2, sn3)
	val itr = scheduledNotifications.iterator

	val scrollingScheduledNotifications = new ScrollableResults {
		override def next(): Boolean = itr.hasNext
		override def get(i: Int): AnyRef = if (i == 0) itr.next() else ???
		override def close(): Unit = {}

		override def getType(i: Int): Type = ???
		override def getCharacter(col: Int): Character = ???
		override def scroll(i: Int): Boolean = ???
		override def getRowNumber: Int = ???
		override def getLocale(col: Int): Locale = ???
		override def beforeFirst(): Unit = ???
		override def getTimeZone(col: Int): TimeZone = ???
		override def get(): Array[AnyRef] = ???
		override def last(): Boolean = ???
		override def isLast: Boolean = ???
		override def getBinary(col: Int) = Array()
		override def getDouble(col: Int): Double = ???
		override def isFirst: Boolean = ???
		override def setRowNumber(rowNumber: Int): Boolean = ???
		override def getClob(col: Int): Clob = ???
		override def getFloat(col: Int): Float = ???
		override def getBigDecimal(col: Int): BigDecimal = ???
		override def getLong(col: Int): Long = ???
		override def getCalendar(col: Int): Calendar = ???
		override def afterLast(): Unit = ???
		override def getByte(col: Int): Byte = ???
		override def getBoolean(col: Int): lang.Boolean = ???
		override def getShort(col: Int): Short = ???
		override def getBigInteger(col: Int): BigInteger = ???
		override def getInteger(col: Int): Integer = ???
		override def getDate(col: Int): Date = ???
		override def getText(col: Int): String = ???
		override def previous(): Boolean = ???
		override def getBlob(col: Int): Blob = ???
		override def first(): Boolean = ???
		override def getString(col: Int): String = ???
	}

	when (dao.notificationsToComplete) thenReturn (new Scrollable[ScheduledNotification[_  >: Null <: ToEntityReference]](scrollingScheduledNotifications, session))

	@Test
	def generateNotifications() {
		val notification = service.generateNotification(sn1).get

		notification.isInstanceOf[HeronWarningNotification] should be (true)
		notification.title should be("You all need to know. Herons would love to kill you in your sleep")
		notification.url should be ("/beware/herons")
		notification.urlTitle should be ("see how evil herons really are")
	}

	@Test
	def processNotifications() {
		service.processNotifications()

		verify(notificationService, times(3)).push(isA[Notification[_,_]])

		for(sn <- scheduledNotifications) {
			sn.completed should be (true)
		}
	}

}
