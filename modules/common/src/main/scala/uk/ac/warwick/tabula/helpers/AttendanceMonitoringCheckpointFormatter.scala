package uk.ac.warwick.tabula.helpers

import org.springframework.beans.factory.annotation.Autowired

import freemarker.template._
import collection.JavaConverters._
import uk.ac.warwick.tabula.JavaImports._
import freemarker.template.utility.DeepUnwrap
import uk.ac.warwick.tabula.data.model.attendance.{AttendanceMonitoringPointStyle, AttendanceState, AttendanceMonitoringPoint, AttendanceMonitoringCheckpoint}
import freemarker.core.Environment
import uk.ac.warwick.tabula.data.model.{StudentMember, Department}
import org.joda.time.DateTime
import uk.ac.warwick.tabula.services.{AttendanceMonitoringService, UserLookupService}
import uk.ac.warwick.tabula.attendance.web.Routes

case class AttendanceMonitoringCheckpointFormatterResult(
	labelText: String,
	labelClass: String,
	iconClass: String,
	status: String,
	metadata: String,
	noteText: String,
	noteUrl: String
)

/**
 * Freemarker helper to build the necessary fields to display a checkpoint
 */
class AttendanceMonitoringCheckpointFormatter extends TemplateMethodModelEx {

	@Autowired var userLookup: UserLookupService = _
	@Autowired var attendanceMonitoringService: AttendanceMonitoringService = _

	override def exec(list: JList[_]): AttendanceMonitoringCheckpointFormatterResult = {
		val args = list.asScala.toSeq.map {
			model => DeepUnwrap.unwrap(model.asInstanceOf[TemplateModel])
		}
		args match {
			case Seq(department: Department, checkpoint: AttendanceMonitoringCheckpoint, _*) => result(department, checkpoint)
			case Seq(department: Department, point: AttendanceMonitoringPoint, student: StudentMember, _*) => result(department, point, student)
			case _ => throw new IllegalArgumentException("Bad args")
		}
	}

	private def describeCheckpoint(checkpoint: AttendanceMonitoringCheckpoint) = {
		val userString = userLookup.getUserByUserId(checkpoint.updatedBy) match {
			case FoundUser(user) => s"by ${user.getFullName}, "
			case _ => ""
		}

		s"Recorded ${userString}${DateBuilder.format(checkpoint.updatedDate)}"
	}

	private def pointDuration(point: AttendanceMonitoringPoint, department: Department) = {
		val wrapper = new DefaultObjectWrapper()
		point.scheme.pointStyle match {
			case AttendanceMonitoringPointStyle.Date =>
				val intervalFormatter = Environment.getCurrentEnvironment.getGlobalVariable("intervalFormatter").asInstanceOf[TemplateMethodModel]
				intervalFormatter.exec(JList(
					wrapper.wrap(point.startDate.toDate),
					wrapper.wrap(point.endDate.toDate)
				))
			case AttendanceMonitoringPointStyle.Week =>
				val wholeWeekFormatter = Environment.getCurrentEnvironment.getGlobalVariable("wholeWeekFormatter").asInstanceOf[TemplateMethodModel]
				wholeWeekFormatter.exec(JList(
					wrapper.wrap(point.startWeek),
					wrapper.wrap(point.endWeek),
					wrapper.wrap(point.scheme.academicYear),
					wrapper.wrap(department),
					wrapper.wrap(false)
				))
		}
	}

	private def result(department: Department, checkpoint: AttendanceMonitoringCheckpoint): AttendanceMonitoringCheckpointFormatterResult = {
		val point = checkpoint.point
		val (noteText, noteUrl) = attendanceMonitoringService.getAttendanceNote(checkpoint.student, point).fold(("", ""))(note =>
			(note.truncatedNote, Routes.Note.view(point.scheme.academicYear, checkpoint.student, point))
		)

		checkpoint.state match {
			case AttendanceState.Attended =>
				AttendanceMonitoringCheckpointFormatterResult(
					"Attended",
					"label-success",
					"icon-ok attended",
					s"Attended: ${point.name} (${pointDuration(point, department)})",
					describeCheckpoint(checkpoint),
					s"$noteText",
					s"$noteUrl"
				)
			case AttendanceState.MissedAuthorised =>
				AttendanceMonitoringCheckpointFormatterResult(
					"Missed (authorised)",
					"label-info",
					"icon-remove-circle authorised",
					s"Missed (authorised): ${point.name} (${pointDuration(point, department)})",
					describeCheckpoint(checkpoint),
					s"$noteText",
					s"$noteUrl"
				)
			case AttendanceState.MissedUnauthorised =>
				AttendanceMonitoringCheckpointFormatterResult(
					"Missed (unauthorised)",
					"label-important",
					"icon-remove unauthorised",
					s"Missed (unauthorised): ${point.name} (${pointDuration(point, department)})",
					describeCheckpoint(checkpoint),
					s"$noteText",
					s"$noteUrl"
				)
		}
	}

	private def result(department: Department, point: AttendanceMonitoringPoint, student: StudentMember): AttendanceMonitoringCheckpointFormatterResult = {
		val (noteText, noteUrl) = attendanceMonitoringService.getAttendanceNote(student, point).fold(("", ""))(note =>
			(note.truncatedNote, Routes.Note.view(point.scheme.academicYear, student, point))
		)

		if (point.endDate.isBefore(DateTime.now.toLocalDate)) {
			// is late
			AttendanceMonitoringCheckpointFormatterResult(
				"Unrecorded",
				"label-warning",
				"icon-warning-sign late",
				s"${point.name} (${pointDuration(point, department)})",
				"",
				s"$noteText",
				s"$noteUrl"
			)
		} else {
			AttendanceMonitoringCheckpointFormatterResult(
				"",
				"",
				"icon-minus",
				s"${point.name} (${pointDuration(point, department)})",
				"",
				s"$noteText",
				s"$noteUrl"
			)
		}
	}
}