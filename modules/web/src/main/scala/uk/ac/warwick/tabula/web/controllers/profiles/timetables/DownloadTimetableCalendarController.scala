package uk.ac.warwick.tabula.web.controllers.profiles.timetables

import org.joda.time.{DateTime, Days, LocalDate}
import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.{ModelAttribute, PathVariable, RequestMapping, RequestParam}
import uk.ac.warwick.tabula.commands.profiles.PhotosWarwickMemberPhotoUrlGeneratorComponent
import uk.ac.warwick.tabula.commands.timetables.ViewMemberEventsCommand
import uk.ac.warwick.tabula.commands.timetables.ViewMemberEventsCommand.TimetableCommand
import uk.ac.warwick.tabula.data.model.Member
import uk.ac.warwick.tabula.data.model.groups.{DayOfWeek, WeekRange}
import uk.ac.warwick.tabula.helpers.{IntervalFormatter, KnowsUserNumberingSystem, WeekRangesFormatter}
import uk.ac.warwick.tabula.pdf.FreemarkerXHTMLPDFGeneratorComponent
import uk.ac.warwick.tabula.services.{AutowiringModuleAndDepartmentServiceComponent, AutowiringTermServiceComponent, AutowiringUserLookupComponent, AutowiringUserSettingsServiceComponent}
import uk.ac.warwick.tabula.web.controllers.profiles.ProfilesController
import uk.ac.warwick.tabula.web.views.{AutowiredTextRendererComponent, FullCalendarEvent, PDFView}
import uk.ac.warwick.tabula.{AcademicYear, CurrentUser, RequestFailedException}

import scala.util.{Failure, Success}

@Controller
@RequestMapping(Array("/profiles/view/{member}/timetable/download-calendar"))
class DownloadTimetableCalendarController extends ProfilesController
	with AutowiringUserLookupComponent with AutowiringTermServiceComponent
	with KnowsUserNumberingSystem with AutowiringUserSettingsServiceComponent
	with AutowiringModuleAndDepartmentServiceComponent {

	@ModelAttribute("timetableCommand")
	def timetableCommand(@PathVariable member: Member, currentUser: CurrentUser) =
		ViewMemberEventsCommand(mandatory(member), currentUser)

	@RequestMapping
	def render(
		@ModelAttribute("timetableCommand") cmd: TimetableCommand,
		@PathVariable member: Member,
		@RequestParam(value = "calendarView", required = false) calendarView: String,
		@RequestParam(value = "renderDate", required = false) renderDate: LocalDate
	) = {
		val thisRenderDate = Option(renderDate).getOrElse(DateTime.now.toLocalDate)
		val thisCalendarView = Option(calendarView).getOrElse("month")
		val (startDate, endDate) = Option(calendarView).getOrElse("month") match {
			case "agendaDay" =>
				(
					thisRenderDate,
					thisRenderDate.plusDays(1)
				)
			case "agendaWeek" =>
				(
					thisRenderDate.minusDays(thisRenderDate.getDayOfWeek - 1), // The Monday of that week
					thisRenderDate.plusDays(7).minusDays(thisRenderDate.getDayOfWeek - 1) // The Moday of the following week
				)
			case _ =>  // month
				val firstDayOfMonth = thisRenderDate.minusDays(thisRenderDate.getDayOfMonth - 1)
				val lastDayOfMonth = firstDayOfMonth.plusDays(thisRenderDate.dayOfMonth.getMaximumValue - 1)
				(
					firstDayOfMonth.minusDays(firstDayOfMonth.getDayOfWeek - 1), // The Monday of that week
					lastDayOfMonth.plusDays(7).minusDays(lastDayOfMonth.getDayOfWeek - 1) // The Moday of the following week
				)
		}

		cmd.from = startDate
		cmd.to = endDate

		cmd.apply() match {
			case Success(result) =>

				val fullCalendarEvents = FullCalendarEvent.colourEvents(result.events.map(FullCalendarEvent(_, userLookup)))
				val allDays: Seq[LocalDate] = (0 until Days.daysBetween(startDate, endDate).getDays).map(startDate.plusDays)

				thisCalendarView match {
					case "agendaDay" | "agendaWeek" =>
						agendaView(fullCalendarEvents, thisCalendarView, thisRenderDate, startDate, endDate, member, allDays)
					case _ =>
						monthView(fullCalendarEvents, thisCalendarView, thisRenderDate, startDate, endDate, member, allDays)
				}
			case Failure(t) => throw new RequestFailedException("The timetabling service could not be reached", t)
		}
	}

	private def agendaView(
		fullCalendarEvents: Seq[FullCalendarEvent],
		thisCalendarView: String,
		thisRenderDate: LocalDate,
		startDate: LocalDate,
		endDate: LocalDate,
		member: Member,
		allDays: Seq[LocalDate]
	): PDFView = {
		val groupedEvents: Map[LocalDate, Map[Int, Seq[FullCalendarEvent]]] = fullCalendarEvents
			.groupBy(e => new DateTime(e.start*1000).toLocalDate)
			.mapValues(_.groupBy(e => new DateTime(e.start*1000).getHourOfDay))

		val title = {
			if (thisCalendarView == "agendaWeek") {
				val thisWeek = termService.getTermFromDateIncludingVacations(thisRenderDate.toDateTimeAtStartOfDay)
					.getAcademicWeekNumber(thisRenderDate.toDateTimeAtStartOfDay)
				val system = numberingSystem(user, moduleAndDepartmentService.getDepartmentByCode(user.departmentCode))
				val description = WeekRangesFormatter.format(
					Seq(WeekRange(thisWeek)),
					DayOfWeek(thisRenderDate.dayOfWeek.get),
					AcademicYear.findAcademicYearContainingDate(thisRenderDate.toDateTimeAtStartOfDay),
					system
				)
				if (system == WeekRange.NumberingSystem.Academic || description.startsWith("Term")) {
					description
				} else {
					IntervalFormatter.format(startDate.toDateTimeAtStartOfDay, endDate.toDateTimeAtStartOfDay, includeTime = false, includeDays = false)
				}
			} else {
				""
			}
		}

		createPdfView(member, Map(
			"calendarView" -> thisCalendarView,
			"allDays" -> allDays,
			"groupedEvents" -> groupedEvents,
			"renderDate" -> thisRenderDate,
			"title" -> title
		))
	}

	private def monthView(
		fullCalendarEvents: Seq[FullCalendarEvent],
		thisCalendarView: String,
		thisRenderDate: LocalDate,
		startDate: LocalDate,
		endDate: LocalDate,
		member: Member,
		allDays: Seq[LocalDate]
	): PDFView = {
		val groupedEvents: Map[Int, Map[LocalDate, Seq[FullCalendarEvent]]] = fullCalendarEvents
			.groupBy(e => new DateTime(e.start*1000).weekOfWeekyear.get)
			.mapValues(_.groupBy(e => new DateTime(e.start*1000).toLocalDate))
		val firstDayOfMonth = thisRenderDate.minusDays(thisRenderDate.getDayOfMonth - 1)
		val lastDayOfMonth = firstDayOfMonth.plusDays(thisRenderDate.dayOfMonth.getMaximumValue - 1)
		createPdfView(member, Map(
			"calendarView" -> thisCalendarView,
			"groupedAllDays" -> allDays.groupBy(_.weekOfWeekyear.get),
			"groupedEvents" -> groupedEvents,
			"renderDate" -> thisRenderDate,
			"firstDayOfMonth" -> firstDayOfMonth,
			"lastDayOfMonth" -> lastDayOfMonth
		))
	}

	private def createPdfView(member: Member, modelMap: Map[String, Any]): PDFView =
		new PDFView(
			s"timetable-calendar-${member.universityId}.pdf",
			"/WEB-INF/freemarker/profiles/timetables/export-calendar.ftl",
			modelMap
		) with FreemarkerXHTMLPDFGeneratorComponent
			with AutowiredTextRendererComponent
			with PhotosWarwickMemberPhotoUrlGeneratorComponent

}
