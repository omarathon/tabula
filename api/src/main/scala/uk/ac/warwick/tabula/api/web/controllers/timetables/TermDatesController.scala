package uk.ac.warwick.tabula.api.web.controllers.timetables

import net.fortuna.ical4j.model.Calendar
import net.fortuna.ical4j.model.component.VEvent
import net.fortuna.ical4j.model.property._
import org.joda.time.LocalDate
import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation._
import uk.ac.warwick.tabula.api.web.controllers.ApiController
import uk.ac.warwick.tabula.api.web.controllers.timetables.TermDatesController._
import uk.ac.warwick.tabula.data.model.groups.WeekRange
import uk.ac.warwick.tabula.web.Mav
import uk.ac.warwick.tabula.web.views.{IcalView, JSONView}
import uk.ac.warwick.tabula.{AcademicPeriod, AcademicYear, DateFormats}

object TermDatesController {
	case class NamedTerm(academicYear: AcademicYear, name: String, term: AcademicPeriod, weekRange: WeekRange)
}

@Controller
@RequestMapping(Array("/v1/termdates", "/v1/termdates.*"))
class TermDatesController extends ApiController
	with GetTermDatesApi
	with DefaultAcademicYearApi

@Controller
@RequestMapping(Array("/v1/termdates/{year}", "/v1/termdates/{year}.*"))
class TermDatesForYearController extends ApiController
	with GetTermDatesApi
	with PathVariableAcademicYearScopedApi

trait GetTermDatesApi {
	self: ApiController
		with AcademicYearScopedApi =>

	@ModelAttribute("terms")
	def terms(@ModelAttribute("academicYear") academicYears: Seq[AcademicYear]): Seq[NamedTerm] = academicYears.flatMap { academicYear =>
		val weeks = academicYear.weeks

		val terms =
			weeks
				.map { case (weekNumber, week) =>
					(weekNumber, week.period)
				}
				.groupBy { _._2 }
				.map { case (term, weekNumbersAndTerms) =>
					(term, WeekRange(weekNumbersAndTerms.keys.min, weekNumbersAndTerms.keys.max))
				}
				.toSeq
				.sortBy { case (_, weekRange) => weekRange.minWeek }

		AcademicPeriod.allPeriodTypes.map(_.toString).zip(terms).map { case (name, (term, weekRange)) => NamedTerm(academicYear, name, term, weekRange) }
	}

	@RequestMapping(method = Array(GET), produces = Array("application/json"))
	def jsonTermDates(@ModelAttribute("terms") terms: Seq[NamedTerm]): Mav = {
		Mav(new JSONView(Map(
			"success" -> true,
			"status" -> "ok",
			"terms" -> terms.map { t => Map(
				"academicYear" -> t.academicYear.toString,
				"name" -> t.name,
				"start" -> DateFormats.IsoDate.print(t.term.firstDay),
				"end" -> DateFormats.IsoDate.print(t.term.lastDay),
				"weekRange" -> Map(
					"minWeek" -> t.weekRange.minWeek,
					"maxWeek" -> t.weekRange.maxWeek
				)
			)}
		)))
	}

	@RequestMapping(method = Array(GET), produces = Array("text/calendar"))
	def icalTermDates(@ModelAttribute("terms") terms: Seq[NamedTerm], @ModelAttribute("academicYear") academicYears: Seq[AcademicYear]): Mav = {
		val cal: Calendar = new Calendar
		cal.getProperties.add(Version.VERSION_2_0)
		cal.getProperties.add(new ProdId("-//Tabula//University of Warwick IT Services//EN"))
		cal.getProperties.add(CalScale.GREGORIAN)
		cal.getProperties.add(Method.PUBLISH)
		cal.getProperties.add(new XProperty("X-PUBLISHED-TTL", "PT1W")) // 1 week
		cal.getProperties.add(new XProperty("X-WR-CALNAME", s"Term dates - ${academicYears.head.startYear}-${academicYears.last.endYear}"))

		def toIcalDate(date: LocalDate): net.fortuna.ical4j.model.Date =
			new net.fortuna.ical4j.model.Date(date.toString("yyyyMMdd"))

		terms.zipWithIndex.foreach { case (t, termNumber) =>
			val event: VEvent = new VEvent(toIcalDate(t.term.firstDay), toIcalDate(t.term.lastDay.plusDays(1)), t.name.safeSubstring(0, 255))

			event.getProperties.add(new Uid(s"${t.academicYear.startYear}-term-$termNumber"))
			event.getProperties.add(Method.PUBLISH)
			event.getProperties.add(Transp.OPAQUE)

			val organiser: Organizer = new Organizer(s"MAILTO:no-reply@tabula.warwick.ac.uk")
			event.getProperties.add(organiser)

			event.getProperties.add(new XProperty("X-MICROSOFT-CDO-BUSYSTATUS", "FREE"))

			cal.getComponents.add(event)
		}

		Mav(new IcalView(cal), "filename" -> s"termdates-${academicYears.head.startYear}-${academicYears.last.endYear}.ics")
	}

}
