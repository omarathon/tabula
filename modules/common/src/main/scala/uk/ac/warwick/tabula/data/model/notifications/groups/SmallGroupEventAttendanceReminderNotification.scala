package uk.ac.warwick.tabula.data.model.notifications.groups

import javax.persistence.{DiscriminatorValue, Entity}

import org.joda.time.Days
import uk.ac.warwick.spring.Wire
import uk.ac.warwick.tabula.data.model.NotificationPriority.{Critical, Warning}
import uk.ac.warwick.tabula.data.model.groups.SmallGroupEventOccurrence
import uk.ac.warwick.tabula.data.model._
import uk.ac.warwick.tabula.groups.web.Routes
import uk.ac.warwick.tabula.services.ModuleAndDepartmentService
import uk.ac.warwick.userlookup.User

import scala.collection.JavaConverters._

@Entity
@DiscriminatorValue(value="SmallGroupEventAttendanceReminder")
class SmallGroupEventAttendanceReminderNotification
	extends Notification[SmallGroupEventOccurrence, SmallGroupEventOccurrence]
		with SingleItemNotification[SmallGroupEventOccurrence]
		with ConfigurableNotification {

	override final def verb = "record"

	override final def actionRequired = true

	override def urlTitle = "record attendance for these seminars"

	override def url = Routes.tutor.registerForWeek(event, item.entity.week)
		
	@transient
	final lazy val event = item.entity.event

	@transient
	final lazy val configuringDepartment = event.group.groupSet.module.adminDepartment

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

	override def allRecipients: Seq[User] = {
		val attendanceIds = item.entity.attendance.asScala.map(_.universityId)
		if (!event.group.groupSet.collectAttendance || event.group.students.isEmpty || event.group.students.users.map(_.getWarwickId).forall(attendanceIds.contains)) {
			Seq()
		} else {
			var users: Seq[User] = Seq()

			val settings = new SmallGroupEventAttendanceReminderNotificationSettings(departmentSettings)
			val notifyAllGroups = !settings.notifyFirstNonEmptyGroupOnly.value

			val moduleAndDepartmentService = Wire[ModuleAndDepartmentService]
			val module =
				moduleAndDepartmentService.getModuleByCode(event.group.groupSet.module.code)
					.getOrElse(throw new IllegalStateException("No such module"))

			if (settings.notifyNamedUsers.value && settings.notifyNamedUsersFirst.value) {
				users ++= settings.namedUsers.value
			}

			if (settings.notifyTutors.value && (users.isEmpty || notifyAllGroups)) {
				users ++= event.tutors.users
			}

			if (settings.notifyModuleAssistants.value && (users.isEmpty || notifyAllGroups)) {
				users ++= module.assistants.users
			}

			if (settings.notifyModuleManagers.value && (users.isEmpty || notifyAllGroups)) {
				users ++= module.managers.users
			}

			if (settings.notifyDepartmentAdministrators.value && (users.isEmpty || notifyAllGroups)) {
				users ++= module.adminDepartment.owners.users
			}

			if (settings.notifyNamedUsers.value && !settings.notifyNamedUsersFirst.value && (users.isEmpty || notifyAllGroups)) {
				users ++= settings.namedUsers.value
			}

			users.distinct
		}
	}
}

class SmallGroupEventAttendanceReminderNotificationSettings(departmentSettings: NotificationSettings) {
	// Configuration settings specific to this type of notification
	def enabled = departmentSettings.enabled
	def notifyTutors = departmentSettings.BooleanSetting("notifyTutors", default = true)
	def notifyModuleAssistants = departmentSettings.BooleanSetting("notifyModuleAssistants", default = false)
	def notifyModuleManagers = departmentSettings.BooleanSetting("notifyModuleManagers", default = true)
	def notifyDepartmentAdministrators = departmentSettings.BooleanSetting("notifyDepartmentAdministrators", default = false)
	def notifyNamedUsers = departmentSettings.BooleanSetting("notifyNamedUsers", default = false)
	def notifyNamedUsersFirst = departmentSettings.BooleanSetting("notifyNamedUsersFirst", default = false)
	def namedUsers = departmentSettings.UserSeqSetting("namedUsers", default = Seq())
	def notifyFirstNonEmptyGroupOnly = departmentSettings.BooleanSetting("notifyFirstNonEmptyGroupOnly", default = true)
}