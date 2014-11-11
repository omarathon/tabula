package uk.ac.warwick.tabula.profiles.web.controllers

import uk.ac.warwick.tabula.AcademicYear
import uk.ac.warwick.tabula.data.model.AbstractMeetingRecord
import uk.ac.warwick.tabula.services.TermServiceComponent
import uk.ac.warwick.util.termdates.{TermNotFoundException, Term}

trait MeetingRecordAcademicYearFiltering {
	self: TermServiceComponent =>

	def meetingNotInAcademicYear(academicYear: AcademicYear)(meeting: AbstractMeetingRecord) =
		try {
			Seq(Term.WEEK_NUMBER_BEFORE_START, Term.WEEK_NUMBER_AFTER_END).contains(
				termService.getAcademicWeekForAcademicYear(meeting.meetingDate, academicYear)
			)
		} catch {
			case e: TermNotFoundException =>
				// TAB-2465 Don't include this meeting - this happens if you are looking at a year before we recorded term dates
				true
		}

}
