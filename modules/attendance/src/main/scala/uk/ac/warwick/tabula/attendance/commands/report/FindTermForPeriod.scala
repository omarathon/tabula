package uk.ac.warwick.tabula.attendance.commands.report

import org.joda.time.DateTime
import uk.ac.warwick.util.termdates.Term
import uk.ac.warwick.tabula.AcademicYear
import uk.ac.warwick.tabula.services.TermServiceComponent

trait FindTermForPeriod extends TermServiceComponent {

	def findTermAndWeeksForPeriod(period: String, academicYear: AcademicYear) = {

		def findTermForPeriod(dateToCheck: DateTime): Term = {
			val term = termService.getTermFromDateIncludingVacations(dateToCheck)
			if (term.getTermTypeAsString == period)
				term
			else
				findTermForPeriod(dateToCheck.plusWeeks(1))
		}

		val termForPeriod = findTermForPeriod(academicYear.dateInTermOne.toDateTime)
		val periodStartWeek = termForPeriod.getAcademicWeekNumber(termForPeriod.getStartDate)
		val periodEndWeek = termForPeriod.getAcademicWeekNumber(termForPeriod.getEndDate)
		(termForPeriod, periodStartWeek, periodEndWeek)
	}


}
