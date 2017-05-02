package uk.ac.warwick.tabula.helpers

import freemarker.template.{TemplateMethodModelEx, TemplateModel}
import freemarker.template.utility.DeepUnwrap
import org.joda.time.base.BaseDateTime
import org.joda.time.{DateTime, LocalDate}
import uk.ac.warwick.tabula.JavaImports.JList
import uk.ac.warwick.tabula.{AcademicYear, RequestInfo}
import uk.ac.warwick.tabula.data.model.groups.{DayOfWeek, WeekRange}
import uk.ac.warwick.tabula.services.{AutowiringTermServiceComponent, AutowiringUserSettingsServiceComponent, TermService}

import scala.collection.JavaConverters._

class DateToWeekNumberTag extends TemplateMethodModelEx with KnowsUserNumberingSystem with AutowiringUserSettingsServiceComponent with AutowiringTermServiceComponent {
	import WeekRangesFormatter.format

	override def exec(list: JList[_]): String = {
		val user = RequestInfo.fromThread.get.user

		def formatDate(date: BaseDateTime)(implicit termService: TermService): String = {
			val year = AcademicYear.findAcademicYearContainingDate(date)
			val weekNumber = termService.getAcademicWeekForAcademicYear(date, year)
			val day = DayOfWeek(date.getDayOfWeek)

			format(Seq(WeekRange(weekNumber)), day, year, numberingSystem(user, None))
		}

		val args = list.asScala.map { model => DeepUnwrap.unwrap(model.asInstanceOf[TemplateModel]) }
		args match {
			case Seq(dt: DateTime) => formatDate(dt)
			case Seq(date: LocalDate) => formatDate(date.toDateTimeAtStartOfDay)
			case _ => throw new IllegalArgumentException("Bad args: " + args)
		}
	}
}
