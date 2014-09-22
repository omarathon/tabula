package uk.ac.warwick.tabula.attendance.commands.manage

import uk.ac.warwick.spring.Wire
import uk.ac.warwick.tabula.data.model.attendance.AttendanceMonitoringScheme
import uk.ac.warwick.tabula.data.model.{Department, ScheduledNotification, StudentMember}
import uk.ac.warwick.tabula.helpers.Logging
import uk.ac.warwick.tabula.services._
import uk.ac.warwick.tabula.services.attendancemonitoring.AttendanceMonitoringServiceComponent

import scala.collection.JavaConverters._

trait UpdatesAttendanceMonitoringScheme extends Logging {

	self: AttendanceMonitoringServiceComponent with ProfileServiceComponent =>

	var thisScheduledNotificationService = Wire.auto[ScheduledNotificationService]

	def afterUpdate(schemes: Seq[AttendanceMonitoringScheme]) = {
		val allStudents = profileService.getAllMembersWithUniversityIds(schemes.flatMap(_.members.members).distinct).flatMap {
			case student: StudentMember => Option(student)
			case _ => None
		}.map(s => s.universityId -> s).toMap

		schemes.groupBy(s => (s.department, s.academicYear)).map{case((department, academicYear), groupedSchemes) =>
			attendanceMonitoringService.updateCheckpointTotalsAsync(
				groupedSchemes.flatMap(_.members.members).distinct.map(allStudents),
				department,
				academicYear
			)
		}

		// Custom scheduled notifications
		schemes.groupBy(_.department).map{case(department, _) =>

			thisScheduledNotificationService.removeInvalidNotifications(department)

			val schemes = attendanceMonitoringService.listAllSchemes(department)

			val notifications = schemes.flatMap(_.points.asScala.map(_.endDate.plusDays(7).toDateTimeAtStartOfDay)).flatMap(notificationDate => {
				Seq(
					new ScheduledNotification[Department]("AttendanceMonitoringUnrecordedPoints", department, notificationDate),
					new ScheduledNotification[Department]("AttendanceMonitoringUnrecordedStudents", department, notificationDate)
				)
			})

			for (scheduledNotification <- notifications) {
				if (scheduledNotification.scheduledDate.isBeforeNow) {
					logger.warn("ScheduledNotification generated in the past, ignoring: " + scheduledNotification)
				} else {
					thisScheduledNotificationService.push(scheduledNotification)
				}
			}
		}
	}

}
