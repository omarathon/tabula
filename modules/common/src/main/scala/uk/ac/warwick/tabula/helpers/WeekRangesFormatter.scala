package uk.ac.warwick.tabula.helpers

import scala.collection.JavaConverters._
import scala.collection.mutable
import scala.language.implicitConversions
import org.joda.time.base.BaseDateTime
import org.joda.time.{DateMidnight, DateTimeConstants, DateTime}
import org.springframework.beans.factory.annotation.Autowired
import uk.ac.warwick.tabula.data.model.groups.WeekRange
import uk.ac.warwick.util.termdates.TermFactory
import uk.ac.warwick.spring.Wire
import uk.ac.warwick.tabula.data.model.groups.DayOfWeek
import uk.ac.warwick.tabula.AcademicYear
import uk.ac.warwick.util.termdates.Term
import uk.ac.warwick.tabula.helpers.Promises._
import uk.ac.warwick.tabula.data.model.Department
import uk.ac.warwick.tabula.RequestInfo
import uk.ac.warwick.tabula.data.model.groups.SmallGroupEvent
import uk.ac.warwick.tabula.JavaImports._
import freemarker.template.{ TemplateModel, TemplateMethodModelEx }
import freemarker.template.utility.DeepUnwrap
import uk.ac.warwick.tabula.services.UserSettingsService

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

	var termFactory = Wire[TermFactory]

	// We are confident that November 1st is always in term 1 of the year
	lazy val weeksForYear =
		termFactory.getAcademicWeeksForYear(new DateMidnight(year.startYear, DateTimeConstants.NOVEMBER, 1))
			.asScala.map { pair => (pair.getLeft -> pair.getRight) } // Utils pairs to Scala pairs
			.toMap

	def weekNumberToDate(weekNumber: Int, dayOfWeek: DayOfWeek) =
		weeksForYear(weekNumber).getStart().withDayOfWeek(dayOfWeek.jodaDayOfWeek)

	def groupWeekRangesByTerm(ranges: Seq[WeekRange], dayOfWeek: DayOfWeek) = {
		ranges flatMap { range =>
			if (range.isSingleWeek) Seq((range, termFactory.getTermFromDateIncludingVacations(weekNumberToDate(range.minWeek, dayOfWeek))))
			else {
				val startDate = weekNumberToDate(range.minWeek, dayOfWeek)
				val endDate = weekNumberToDate(range.maxWeek, dayOfWeek)

				termFactory.getTermsBetween(startDate, endDate) map { term =>
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

	val weekRange = WeekRange

	def format(ranges: Seq[WeekRange], dayOfWeek: DayOfWeek, numberingSystem: String) = {
		val allTermWeekRanges = weekRange.termWeekRanges(year)
		val currentTerm = getTermNumber(DateTime.now)
		val eventRanges = ranges flatMap (_.toWeeks)

		val currentTermRanges = allTermWeekRanges.toList(currentTerm).toWeeks
		val weeks = currentTermRanges.intersect(eventRanges)

	  numberingSystem match {
			case "term" => weeks.map( x => EventWeek( x - (currentTermRanges(0) - 1 ), x ) )
			case "cumulative" => weeks.map({x =>
				val date = weekNumberToDate(x, dayOfWeek)
				EventWeek(termFactory.getTermFromDate(date).getCumulativeWeekNumber(date), x)
			})
			case _ => weeks.map( x => EventWeek(x, x) )
		}
	}

	def getTermNumber(now: DateTime): Int = {
		termFactory.getTermFromDate(DateTime.now).getTermType match {
			case Term.TermType.autumn => 0
			case Term.TermType.spring => 1
			case Term.TermType.summer => 2
		}
	}
}

case class EventWeek(weekToDisplay: Int, weekToStore: Int)

class WeekRangeSelectFormatterTag extends TemplateMethodModelEx {
	import WeekRangeSelectFormatter.format

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
				case Seq(event: SmallGroupEvent) =>
					format(event.weekRanges, event.day, event.group.groupSet.academicYear, numberingSystem(event.group.groupSet.module.department))

				case _ => throw new IllegalArgumentException("Bad args: " + args)
			}
		}
}


/** Special implementation of Term to encapsulate the idea of a Vacation.
  * Our default TermFactory doesn't care about Vacations, it returns the
  * next term if you give it a date before a vacation.
  */
case class Vacation(before: Term, after: Term) extends Term {
	// Starts the day after the previous term and ends the day before the new term
	def getStartDate = before.getEndDate().plusDays(1)
	def getEndDate = after.getStartDate().minusDays(1)

	def getTermType = null
	def getTermTypeAsString = before.getTermType match {
		case Term.TermType.autumn => "Christmas vacation"
		case Term.TermType.spring => "Easter vacation"
		case Term.TermType.summer => "Summer vacation"
	}

	def getWeekNumber(date: BaseDateTime) = throw new IllegalStateException("Can't get week numbers from a vacation")
	def getCumulativeWeekNumber(date: BaseDateTime) = throw new IllegalStateException("Can't get week numbers from a vacation")
	def getAcademicWeekNumber(date: BaseDateTime) = after.getAcademicWeekNumber(date)
}
