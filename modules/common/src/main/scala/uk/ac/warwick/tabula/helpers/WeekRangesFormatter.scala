package uk.ac.warwick.tabula.helpers

import scala.collection.JavaConverters._
import scala.collection.mutable
import scala.language.implicitConversions
import uk.ac.warwick.tabula.data.model.groups.WeekRange
import uk.ac.warwick.util.termdates.TermFactory
import uk.ac.warwick.spring.Wire
import uk.ac.warwick.tabula.data.model.groups.DayOfWeek
import uk.ac.warwick.tabula.{CurrentUser, AcademicYear, RequestInfo}
import org.joda.time.DateMidnight
import org.joda.time.DateTimeConstants
import uk.ac.warwick.util.termdates.Term
import org.joda.time.DateTime
import org.joda.time.base.BaseDateTime
import uk.ac.warwick.tabula.helpers.Promises._
import uk.ac.warwick.tabula.data.model.Department
import uk.ac.warwick.tabula.data.model.groups.SmallGroupEvent
import uk.ac.warwick.tabula.JavaImports._
import freemarker.template.{ TemplateModel, TemplateMethodModelEx }
import freemarker.template.utility.DeepUnwrap
import org.springframework.beans.factory.annotation.Autowired
import uk.ac.warwick.tabula.services.{ModuleAndDepartmentService, Vacation, TermService, UserSettingsService}
import uk.ac.warwick.tabula.data.model.attendance.{MonitoringPointSet, MonitoringPoint}
import java.util
import uk.ac.warwick.util.termdates.Term.TermType


/** Format week ranges, using a formatting preference for term week numbers, cumulative week numbers or academic week numbers.
  *
  * WeekRange objects are always _stored_ with academic week numbers. These may span multiple terms, holidays etc.
  */
object WeekRangesFormatter extends VacationAware {

	val separator = "; "

	private val formatterMap = new WeekRangesFormatterCache

	/** The reason we need the academic year and day of the week here is that it might affect
	  * which term a date falls under. Often, the Spring term starts on a Wednesday after New
	  * Year's Day, so Monday of that week is in the vacation, but Thursday is week 1 of term 2.
	  */
	def format(ranges: Seq[WeekRange], dayOfWeek: DayOfWeek, year: AcademicYear, numberingSystem: String) =
		formatterMap.retrieve(year) format (ranges, dayOfWeek, numberingSystem)

	class WeekRangesFormatterCache {
		private val map = mutable.HashMap[AcademicYear, WeekRangesFormatter]()
		def retrieve(year: AcademicYear) = map.getOrElseUpdate(year, new WeekRangesFormatter(year))
	}
}




/* Pimp the TermFactory to include Vacation "Terms"
 * We extend AnyVal here to make this a Value class (c.f. http://docs.scala-lang.org/overviews/core/value-classes.html)
 * This means we never instantiate the wrapper, the compiler just performs voodoo to call the methods.
 */

trait VacationAware {
	implicit def vacationAwareTermFactory(delegate: TermFactory) = new VacationAwareTermFactory(delegate)
}

class VacationAwareTermFactory(val delegate: TermFactory) extends AnyVal  {
	def getTermFromDateIncludingVacations(date: BaseDateTime) = {
		val term = delegate.getTermFromDate(date)
		if (date.isBefore(term.getStartDate())) Vacation(delegate.getPreviousTerm(term), term)
		else term
	}

	def getTermsBetween(start: BaseDateTime, end: BaseDateTime): Seq[Term] = {
		val startTerm = getTermFromDateIncludingVacations(start)
		val endTerm = getTermFromDateIncludingVacations(end)

		if (startTerm == endTerm) Seq(startTerm)
		else startTerm +: getTermsBetween(startTerm.getEndDate().plusDays(1), end)
	}
}






/** Companion class for Freemarker.
  */
class WeekRangesFormatterTag extends TemplateMethodModelEx {

	import WeekRangesFormatter.format

	@Autowired var userSettings: UserSettingsService = _

	/** Pass through all the arguments, or just a SmallGroupEvent if you're lazy */
	override def exec(list: JList[_]) = {
		val user = RequestInfo.fromThread.get.user

		def numberingSystem(department: Department) = {
			userSettings.getByUserId(user.apparentId)
				.flatMap { settings => Option(settings.weekNumberingSystem) }
				.getOrElse(department.weekNumberingSystem)
		}
		
		val args = list.asScala.toSeq.map { model => DeepUnwrap.unwrap(model.asInstanceOf[TemplateModel]) }
		args match {
			case Seq(ranges: Seq[_], dayOfWeek: DayOfWeek, year: AcademicYear, dept: Department) => 
				format(ranges.asInstanceOf[Seq[WeekRange]], dayOfWeek, year, numberingSystem(dept))
			
			case Seq(ranges: JList[_], dayOfWeek: DayOfWeek, year: AcademicYear, dept: Department) => 
				format(ranges.asScala.toSeq.asInstanceOf[Seq[WeekRange]], dayOfWeek, year, numberingSystem(dept))
				
			case Seq(event: SmallGroupEvent) => 
				format(event.weekRanges, event.day, event.group.groupSet.academicYear, numberingSystem(event.group.groupSet.module.department))

			case Seq(monitoringPoint: MonitoringPoint) => {
				format(
					Seq(WeekRange(monitoringPoint.week,monitoringPoint.week)),
					DayOfWeek(1),
					monitoringPoint.pointSet.academicYear,
					numberingSystem(monitoringPoint.pointSet.route.department)
				)
			}

			case Seq(week: Integer, academicYear: AcademicYear, dept: Department) => {
				format(Seq(WeekRange(week)), DayOfWeek(1), academicYear, numberingSystem(dept))
			}

			case _ => throw new IllegalArgumentException("Bad args: " + args)
		}
	}
}

class WeekRangesFormatter(year: AcademicYear) extends WeekRanges(year: AcademicYear) {
	import WeekRangesFormatter._

	// Pimp Term to have a clever toString output
	implicit class PimpedTerm(term: Term) {
		def print(weekRange: WeekRange, dayOfWeek: DayOfWeek, numberingSystem: String) = {
			// TODO we've already done this calculation once in groupWeekRangesByTerm, do we really need to do it again?
			val startDate = weekNumberToDate(weekRange.minWeek, dayOfWeek)
			val endDate = weekNumberToDate(weekRange.maxWeek, dayOfWeek)

			term match {
				case vac: Vacation => {
					// Date range
					if (startDate.equals(endDate))
						"%s, %s" format (vac.getTermTypeAsString, IntervalFormatter.format(startDate, false))
					else
						"%s, %s" format (vac.getTermTypeAsString, IntervalFormatter.format(startDate, endDate, false))
				}
				case term => {
					// Convert week numbers to the correct style
					val termNumber = term.getTermType match {
						case Term.TermType.autumn => 1
						case Term.TermType.spring => 2
						case Term.TermType.summer => 3
					}

					def weekNumber(date: DateTime) =
						numberingSystem match {
							case WeekRange.NumberingSystem.Term => term.getWeekNumber(date)
							case WeekRange.NumberingSystem.Cumulative => term.getCumulativeWeekNumber(date)
						}

					if (weekRange.isSingleWeek) "Term %d, week %d" format (termNumber, weekNumber(startDate))
					else "Term %d, weeks %d-%d" format (termNumber, weekNumber(startDate), weekNumber(endDate))
				}
			}
		}
	}

	def format(ranges: Seq[WeekRange], dayOfWeek: DayOfWeek, numberingSystem: String) = numberingSystem match {
		case WeekRange.NumberingSystem.Academic => {
			// Special early exit for the "academic" week numbering system - because that's what we
			// store, we can simply return that.
			val prefix =
				if (ranges.size == 1 && ranges.head.isSingleWeek) "Week"
				else "Weeks"

			prefix + " " + ranges.mkString(separator)
		}
		case WeekRange.NumberingSystem.None => {
			ranges.map { weekRange =>
				val startDate = weekNumberToDate(weekRange.minWeek, dayOfWeek)
				val endDate = weekNumberToDate(weekRange.maxWeek, dayOfWeek)

				IntervalFormatter.format(startDate, endDate, false)
			}.mkString(separator)
		}
		case _ =>
			/*
			 * The first thing we need to do is split the WeekRanges by term.
			 * 
			 * If we have a weekRange that is 1-24, we might split that into three week ranges:
			 *  1-10, 11-15, 16-24
			 *  
			 * This is because we display it as three separate ranges.
			 *
			 * Then we use our PimpedTerm to print the week numbers based on the numbering system.
			 */
			groupWeekRangesByTerm(ranges, dayOfWeek).map {
				case (weekRange, term) =>
					term.print(weekRange, dayOfWeek, numberingSystem)
			}.mkString(separator)
	}

}


object WeekRangeSelectFormatter extends VacationAware {

	val separator = "; "

	private val formatterMap = new WeekRangeSelectFormatterCache

	/** The reason we need the academic year and day of the week here is that it might affect
		* which term a date falls under. Often, the Spring term starts on a Wednesday after New
		* Year's Day, so Monday of that week is in the vacation, but Thursday is week 1 of term 2.
		*/
	def format(ranges: Seq[WeekRange], dayOfWeek: DayOfWeek, year: AcademicYear, numberingSystem: String) =
		formatterMap.retrieve(year) format (ranges, dayOfWeek, numberingSystem)

	class WeekRangeSelectFormatterCache {
		private val map = mutable.HashMap[AcademicYear, WeekRangeSelectFormatter]()
		def retrieve(year: AcademicYear) = map.getOrElseUpdate(year, new WeekRangeSelectFormatter(year))
	}
}


class WeekRanges(year:AcademicYear) extends VacationAware {

	var termService = Wire[TermService]

	// We are confident that November 1st is always in term 1 of the year
	lazy val weeksForYear =
		termService.getAcademicWeeksForYear(year.dateInTermOne)
			.asScala.map { pair => (pair.getLeft -> pair.getRight) } // Utils pairs to Scala pairs
			.toMap

	def weekNumberToDate(weekNumber: Int, dayOfWeek: DayOfWeek) =
		weeksForYear(weekNumber).getStart().withDayOfWeek(dayOfWeek.jodaDayOfWeek)

	def groupWeekRangesByTerm(ranges: Seq[WeekRange], dayOfWeek: DayOfWeek) = {
		ranges flatMap { range =>
			if (range.isSingleWeek) Seq((range, termService.getTermFromDateIncludingVacations(weekNumberToDate(range.minWeek, dayOfWeek))))
			else {
				val startDate = weekNumberToDate(range.minWeek, dayOfWeek)
				val endDate = weekNumberToDate(range.maxWeek, dayOfWeek)

				termService.getTermsBetween(startDate, endDate) map { term =>
					val minWeek =
						if (startDate.isBefore(term.getStartDate())) term.getAcademicWeekNumber(term.getStartDate)
						else term.getAcademicWeekNumber(startDate)

					val maxWeek =
						if (endDate.isAfter(term.getEndDate())) term.getAcademicWeekNumber(term.getEndDate)
						else term.getAcademicWeekNumber(endDate)

					(WeekRange(minWeek, maxWeek), term)
				}
			}
		}
	}
}



class WeekRangeSelectFormatter(year: AcademicYear) extends WeekRanges(year: AcademicYear) {

	def format(ranges: Seq[WeekRange], dayOfWeek: DayOfWeek, numberingSystem: String) = {
		val allTermWeekRanges = WeekRange.termWeekRanges(year)
		val currentTerm = getTermNumber(DateTime.now)
		val eventRanges = ranges flatMap (_.toWeeks)

		val currentTermRanges = allTermWeekRanges.toList(currentTerm).toWeeks
		val weeks = currentTermRanges.intersect(eventRanges)

	  numberingSystem match {
			case WeekRange.NumberingSystem.Term => weeks.map( x => EventWeek( x - (currentTermRanges(0) - 1 ), x ) )
			case WeekRange.NumberingSystem.Cumulative => weeks.map({x =>
				val date = weekNumberToDate(x, dayOfWeek)
				EventWeek(termService.getTermFromDate(date).getCumulativeWeekNumber(date), x)
			})
			case WeekRange.NumberingSystem.Academic => eventRanges.map(x => EventWeek(x, x))
			case _ => weeks.map( x => EventWeek(x, x) )
		}
	}

	def getTermNumber(now: DateTime): Int = {
		termService.getTermFromDate(DateTime.now).getTermType match {
			case Term.TermType.autumn => 0
			case Term.TermType.spring => 1
			case Term.TermType.summer => 2
		}
	}
}

case class EventWeek(weekToDisplay: Int, weekToStore: Int)

class WeekRangeSelectFormatterTag extends TemplateMethodModelEx with KnowsUserNumberingSystem{
	import WeekRangeSelectFormatter.format

	@Autowired var userSettings: UserSettingsService = _

		/** Pass through all the arguments, or just a SmallGroupEvent if you're lazy */
		override def exec(list: JList[_]) = {
			val user = RequestInfo.fromThread.get.user

			val args = list.asScala.toSeq.map { model => DeepUnwrap.unwrap(model.asInstanceOf[TemplateModel]) }
			args match {
				case Seq(event: SmallGroupEvent) =>
					format(event.weekRanges, event.day, event.group.groupSet.academicYear, numberingSystem(user,()=>Option(event.group.groupSet.module.department)))

				case _ => throw new IllegalArgumentException("Bad args: " + args)
			}
		}
}
trait KnowsUserNumberingSystem{
	var userSettings: UserSettingsService

	def numberingSystem(user:CurrentUser, department: ()=>Option[Department]) = {
		userSettings.getByUserId(user.apparentId)
			.flatMap { settings => Option(settings.weekNumberingSystem) }
			.getOrElse(department().map(_.weekNumberingSystem).getOrElse(WeekRange.NumberingSystem.Default))
	}
}
/**
 * Outputs a list of weeks, with the start and end times for each week, and a description which is formatted using
 * the user's preferred numbering system (if set) or the department's.
 *
 * The list is output as JSON, to allow client-side code to access it for formatting weeks in javascript
 */
trait WeekRangesDumper extends KnowsUserNumberingSystem {
	this: ClockComponent=>
	var userSettings: UserSettingsService = Wire[UserSettingsService]
	var departmentService: ModuleAndDepartmentService = Wire[ModuleAndDepartmentService]
	var termService = Wire[TermService]
	lazy val tf = termService.termFactory

	def getWeekRangesAsJSON(formatWeekName: (AcademicYear, Int, String) => String) = {

		// set a fairly large window over which to get week dates since we don't know exactly
		// how they might be used
		val startDate = clock.now.minusYears(2)
		val endDate = clock.now.plusYears(2)
		val user = RequestInfo.fromThread.get.user
		// don't fetch the department if we don't have to, and if we _do_ have to, don't fetch it more than once.
		lazy val department = departmentService.getDepartmentByCode(user.departmentCode)

		val termsAndWeeks = termService.getAcademicWeeksBetween(startDate, endDate)
		val weekDescriptions = termsAndWeeks map {
			case (year, weekNumber, weekInterval) => {
				val defaultDescription = formatWeekName(year, weekNumber, numberingSystem(user, () => department))
				// weekRangeFormatter always includes vacations, but we don't want them here, so if the
				// descripton doesn't look like "Term X Week Y", throw it away and use a standard IntervalFormat.
				val description = if (defaultDescription.startsWith("Term")) {
					defaultDescription
				} else {
					IntervalFormatter.format(weekInterval.getStart, weekInterval.getEnd, includeTime = false, includeDays = false)
				}

			(weekInterval.getStart.getMillis, weekInterval.getEnd.getMillis, description)
			}
		}

		// could use Jackson to map these objects but it doesn't seem worth it
		"[" + weekDescriptions.map {
			case (start, end, desc) => s"""{"start":$start,"end":$end,"desc":"$desc"}"""
		}.mkString(",") + "]"

	}
}

/**
 * Freemarker companion for the WeekRangesDumper
 */
class WeekRangesDumperTag extends TemplateMethodModelEx with WeekRangesDumper with SystemClockComponent{

	def formatWeekName(year:AcademicYear, weekNumber:Int, numberingSystem:String) = {
		// use a WeekRangesFormatter to format the week name, obv.
		val formatter = new WeekRangesFormatter(year)
		formatter.termService = termService
		formatter.format(Seq(WeekRange(weekNumber)),DayOfWeek.Monday,numberingSystem)
	}

	def exec(unused: util.List[_]): AnyRef = {
		  getWeekRangesAsJSON(formatWeekName)
	}
}
