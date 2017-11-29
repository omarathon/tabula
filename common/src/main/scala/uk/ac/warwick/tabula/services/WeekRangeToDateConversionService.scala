package uk.ac.warwick.tabula.services

import org.joda.time._
import uk.ac.warwick.tabula.data.model.groups.{DayOfWeek, WeekRange}
import uk.ac.warwick.tabula.AcademicYear

trait WeekToDateConverterComponent{
	val weekToDateConverter: WeekToDateConverter
}
trait WeekToDateConverter {
	def intersectsWeek(interval: Interval, week: WeekRange.Week, year: AcademicYear): Boolean
	def toLocalDatetime(week: WeekRange.Week, day: DayOfWeek, time: LocalTime, year: AcademicYear): Option[LocalDateTime]
}

trait TermAwareWeekToDateConverterComponent extends WeekToDateConverterComponent {
	val weekToDateConverter: WeekToDateConverter = new TermAwareWeekRangeToDateConversionService

	class TermAwareWeekRangeToDateConversionService extends WeekToDateConverter {
		def intersectsWeek(interval: Interval, week: WeekRange.Week, year: AcademicYear): Boolean = {
			year.weeks.get(week).exists { week =>
				interval.overlap(week.interval) != null
			}
		}

		def toLocalDatetime(week: WeekRange.Week, day: DayOfWeek, time: LocalTime, year: AcademicYear): Option[LocalDateTime] =
			year.weeks.get(week).map { week =>
				week.firstDay
					.withDayOfWeek(day.jodaDayOfWeek)
					.toLocalDateTime(time)
			}
	}

}
