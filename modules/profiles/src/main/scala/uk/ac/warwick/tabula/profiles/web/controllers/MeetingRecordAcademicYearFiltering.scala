package uk.ac.warwick.tabula.profiles.web.controllers

import uk.ac.warwick.tabula.AcademicYear
import uk.ac.warwick.tabula.data.model.AbstractMeetingRecord
import uk.ac.warwick.tabula.services.TermServiceComponent
import uk.ac.warwick.util.termdates.{TermNotFoundException, Term}

trait MeetingRecordAcademicYearFiltering {
	self: TermServiceComponent =>

	def meetingNotInAcademicYear(academicYear: AcademicYear)(meeting: AbstractMeetingRecord) =
		try {
			termService.getAcademicWeekForAcademicYear(meeting.meetingDate, academicYear) match {
				case Term.WEEK_NUMBER_AFTER_END =>
					true
				case Term.WEEK_NUMBER_BEFORE_START if meeting.relationship.studentCourseDetails.freshStudentCourseYearDetails.nonEmpty =>
					meeting.relationship.studentCourseDetails.freshStudentCourseYearDetails.min.academicYear != academicYear
				case _ =>
					false
			}
		} catch {
			case e: TermNotFoundException =>
				// TAB-2465 Don't include this meeting - this happens if you are looking at a year before we recorded term dates
				true
		}

}
