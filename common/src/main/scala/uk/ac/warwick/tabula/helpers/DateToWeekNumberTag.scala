package uk.ac.warwick.tabula.helpers

import freemarker.template.utility.DeepUnwrap
import freemarker.template.{TemplateMethodModelEx, TemplateModel}
import org.joda.time.{DateTime, LocalDate}
import uk.ac.warwick.tabula.JavaImports.JList
import uk.ac.warwick.tabula.data.model.groups.{DayOfWeek, WeekRange}
import uk.ac.warwick.tabula.services.AutowiringUserSettingsServiceComponent
import uk.ac.warwick.tabula.{AcademicYear, RequestInfo}

import scala.collection.JavaConverters._

class DateToWeekNumberTag extends TemplateMethodModelEx with KnowsUserNumberingSystem with AutowiringUserSettingsServiceComponent {
	import WeekRangesFormatter.format

	override def exec(list: JList[_]): String = {
		val user = RequestInfo.fromThread.get.user

		def formatDate(date: LocalDate): String = {
			val year = AcademicYear.forDate(date)
			val weekNumber = year.weekForDate(date).weekNumber
			val day = DayOfWeek(date.getDayOfWeek)

			format(Seq(WeekRange(weekNumber)), day, year, numberingSystem(user, None))
		}

		val args = list.asScala.map { model => DeepUnwrap.unwrap(model.asInstanceOf[TemplateModel]) }
		args match {
			case Seq(dt: DateTime) => formatDate(dt.toLocalDate)
			case Seq(date: LocalDate) => formatDate(date)
			case _ => throw new IllegalArgumentException("Bad args: " + args)
		}
	}
}
