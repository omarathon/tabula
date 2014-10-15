package uk.ac.warwick.tabula.data.model.notifications.groups

import javax.persistence.{DiscriminatorValue, Entity}

import org.joda.time.Days
import uk.ac.warwick.spring.Wire
import uk.ac.warwick.tabula.data.model.NotificationPriority.{Critical, Warning}
import uk.ac.warwick.tabula.data.model.groups.SmallGroupEventOccurrence
import uk.ac.warwick.tabula.data.model.{FreemarkerModel, Notification, SingleItemNotification}
import uk.ac.warwick.tabula.groups.web.Routes
import uk.ac.warwick.tabula.services.ModuleAndDepartmentService
import uk.ac.warwick.userlookup.User

import scala.collection.JavaConverters._

@Entity
@DiscriminatorValue(value="SmallGroupEventAttendanceReminder")
class SmallGroupEventAttendanceReminderNotification
	extends Notification[SmallGroupEventOccurrence, SmallGroupEventOccurrence]
	with SingleItemNotification[SmallGroupEventOccurrence] {

	override final def verb = "record"

	override final def actionRequired = true

	override def urlTitle = "record attendance for these seminars"

	override def url = Routes.tutor.registerForWeek(event, item.entity.week)
		
	@transient
	final lazy val event = item.entity.event

	@transient
	final lazy val referenceDate = item.entity.dateTime.getOrElse(throw new IllegalArgumentException("Tried to create notification for occurrence with no date time"))

	override final def onPreSave(newRecord: Boolean) {
		priority = if (Days.daysBetween(created, referenceDate).getDays >= 5) {
			Critical
		} else {
			Warning
		}
	}

	override def title = s"${event.group.groupSet.format.description} attendance needs recording"

	@transient
	final val FreemarkerTemplate = "/WEB-INF/freemarker/notifications/groups/small_group_event_attendance_reminder_notification.ftl"

	override def content: FreemarkerModel = FreemarkerModel(FreemarkerTemplate, Map(
		"occurrence" -> item.entity,
		"dateTimeFormatter" -> dateTimeFormatter
	))

	override def recipients: Seq[User] = {
		val attendanceIds = item.entity.attendance.asScala.map(_.universityId)
		if (!event.group.groupSet.collectAttendance || event.group.students.isEmpty || event.group.students.users.map(_.getWarwickId).forall(attendanceIds.contains)) {
			Seq()
		} else {
			event.tutors.users // TAB-2830 If there are no tutors, just don't send the email
		}
	}
}
