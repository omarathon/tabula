package uk.ac.warwick.tabula.web.controllers.sysadmin

import org.joda.time.LocalDate
import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.{GetMapping, PostMapping, RequestMapping}
import uk.ac.warwick.tabula.data.model.MeetingRecord
import uk.ac.warwick.tabula.data.model.attendance.AttendanceMonitoringCheckpoint
import uk.ac.warwick.tabula.services.AutowiringMeetingRecordServiceComponent
import uk.ac.warwick.tabula.services.attendancemonitoring.AutowiringAttendanceMonitoringMeetingRecordServiceComponent
import uk.ac.warwick.tabula.web.Mav

@Controller
@RequestMapping(Array("/sysadmin/fix-monitoring-checkpoints"))
class FixMonitoringCheckpointsForAttendedMeetingsController extends BaseSysadminController
	with AutowiringAttendanceMonitoringMeetingRecordServiceComponent
	with AutowiringMeetingRecordServiceComponent {

	def meetingRecords: Seq[MeetingRecord] = meetingRecordService.listAllOnOrAfter(LocalDate.parse("2017-08-01"))

	@GetMapping
	def form: Mav = {
		val checkpoints: Seq[AttendanceMonitoringCheckpoint] = meetingRecords.flatMap(attendanceMonitoringMeetingRecordService.getCheckpoints)

		Mav("sysadmin/fix-monitoring-checkpoints/form", Map(
			"checkpoints" -> checkpoints
		))
	}

	@PostMapping
	def process: Mav = {
		meetingRecords.foreach(attendanceMonitoringMeetingRecordService.updateCheckpoints)

		Mav("sysadmin/fix-monitoring-checkpoints/done")
	}

}
