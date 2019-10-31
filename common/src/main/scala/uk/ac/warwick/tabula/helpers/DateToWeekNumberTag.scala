package uk.ac.warwick.tabula.helpers

import org.joda.time.{DateTime, LocalDate}
import uk.ac.warwick.tabula.data.model.groups.{DayOfWeek, WeekRange}
import uk.ac.warwick.tabula.services.AutowiringUserSettingsServiceComponent
import uk.ac.warwick.tabula.web.views.BaseTemplateMethodModelEx
import uk.ac.warwick.tabula.{AcademicYear, RequestInfo}

class DateToWeekNumberTag extends BaseTemplateMethodModelEx with KnowsUserNumberingSystem with AutowiringUserSettingsServiceComponent {

  import WeekRangesFormatter.format

  override def execMethod(args: Seq[_]): String = {
    val user = RequestInfo.fromThread.get.user

    def formatDate(date: LocalDate): String = {
      val year = AcademicYear.forDate(date)
      if (!year.placeholder) {
        val weekNumber = year.weekForDate(date).weekNumber
        val day = DayOfWeek(date.getDayOfWeek)

        format(Seq(WeekRange(weekNumber)), day, year, numberingSystem(user, None))
      } else {
        DateBuilder.format(date.toDateTimeAtStartOfDay, includeTime = false)
      }
    }

    args match {
      case Seq(dt: DateTime) => formatDate(dt.toLocalDate)
      case Seq(date: LocalDate) => formatDate(date)
      case _ => throw new IllegalArgumentException("Bad args: " + args)
    }
  }
}
