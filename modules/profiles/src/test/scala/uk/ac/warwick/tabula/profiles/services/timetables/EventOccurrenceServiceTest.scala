package uk.ac.warwick.tabula.profiles.services.timetables
import uk.ac.warwick.tabula.{AcademicYear, Mockito, TestBase}
import uk.ac.warwick.tabula.data.model.groups.{WeekRange, DayOfWeek}
import org.joda.time._
import uk.ac.warwick.tabula.services.{WeekToDateConverterComponent, WeekToDateConverter, TermAwareWeekToDateConverterComponent}

class EventOccurrenceServiceTest extends TestBase with Mockito {

	val week1:WeekRange.Week = 1
	val week2:WeekRange.Week = 2
	val week1Start = DateTime.now().withDayOfWeek(DateTimeConstants.MONDAY).withTimeAtStartOfDay()
	val week1end = week1Start.plusDays(7)
	val week2Start = week1end
	val week2End = week2Start.plusDays(7)
	val tenAm = new LocalTime(10,0,0)
	val tenThirty = new LocalTime(10,30,0)

	val singleWeek = Seq(WeekRange(week1,week1))
	val twoWeeks = Seq(WeekRange(week1,week2))

	val singleOccurrence = new TimetableEvent("test","test",TimetableEventType.Lecture,singleWeek, DayOfWeek.Monday,tenAm,tenThirty,None,"XX-123",Nil)
	val doubleOccurrenence	= new TimetableEvent("test","test",TimetableEventType.Lecture,twoWeeks, DayOfWeek.Monday,tenAm,tenThirty,None,"XX-123",Nil)

  val intervalIncludingOccurrence = new Interval(week1Start,week1end)
	val intervalIncludingTwoOccurrences = new Interval(week1Start,week2End)

	val intervalOutsideOccurrence = new Interval(1,2)
  val year = AcademicYear.guessByDate(intervalIncludingOccurrence.getStart)
	val mockConverter = mock[WeekToDateConverter]

	val occurrenceService = new EventOccurrenceComponent with WeekToDateConverterComponent{
		val weekToDateConverter = mockConverter
	}.eventOccurenceService

	@Test
	def singleOccurenceDuringInterval(){
		mockConverter.intersectsWeek(intervalIncludingOccurrence,week1,year) returns true
		mockConverter.toLocalDatetime(week1,DayOfWeek.Monday,tenAm,year) returns Some(week1Start.plusHours(10).toLocalDateTime)
		mockConverter.toLocalDatetime(week1,DayOfWeek.Monday,tenThirty,year) returns Some(week1Start.plusHours(10).plusMinutes(30).toLocalDateTime)

		val eo = occurrenceService.fromTimetableEvent(singleOccurrence,intervalIncludingOccurrence)

		eo.size should be (1)
	}

	@Test
	def singleOccurenceOutsideInterval(){
		mockConverter.intersectsWeek(intervalOutsideOccurrence,week1,year) returns false
		val eo = occurrenceService.fromTimetableEvent(singleOccurrence,intervalIncludingOccurrence)
		eo.size should be (0)
	}

	@Test
	def singleOccurenceExcludedByMidweekInterval(){

		// make the event end before the requested interval starts
		val eventEndDate = intervalIncludingOccurrence.getStart.minusDays(1).toLocalDateTime

		// ...but the week that the event is in, still intersects with the interval
		mockConverter.intersectsWeek(intervalIncludingOccurrence,1,year) returns true

		mockConverter.toLocalDatetime(week1,DayOfWeek.Monday,tenAm,year) returns Some(new LocalDateTime())
		mockConverter.toLocalDatetime(week1,DayOfWeek.Monday,tenThirty,year) returns Some(eventEndDate)

		val eo = occurrenceService.fromTimetableEvent(singleOccurrence,intervalIncludingOccurrence)
		eo.size should be (0)
	}

	@Test
  def multipleOccurrencesDuringInterval(){
		mockConverter.intersectsWeek(intervalIncludingTwoOccurrences,week1,year) returns true
		mockConverter.intersectsWeek(intervalIncludingTwoOccurrences,week2,year) returns true
		mockConverter.toLocalDatetime(week1,DayOfWeek.Monday,tenAm,year) returns Some(week1Start.plusHours(10).toLocalDateTime)
		mockConverter.toLocalDatetime(week1,DayOfWeek.Monday,tenThirty,year) returns Some(week1Start.plusHours(10).plusMinutes(30).toLocalDateTime)
		mockConverter.toLocalDatetime(week2,DayOfWeek.Monday,tenAm,year) returns Some(week2Start.plusHours(10).toLocalDateTime)
		mockConverter.toLocalDatetime(week2,DayOfWeek.Monday,tenThirty,year) returns Some(week2Start.plusHours(10).plusMinutes(30).toLocalDateTime)

		val eo = occurrenceService.fromTimetableEvent(doubleOccurrenence,intervalIncludingTwoOccurrences)

		eo.size should be (2)
	}

	@Test
	def multipleOccurrencesNotAllDuringInterval(){
		mockConverter.intersectsWeek(intervalIncludingTwoOccurrences,week1,year) returns true
		mockConverter.intersectsWeek(intervalIncludingTwoOccurrences,week2,year) returns false
		mockConverter.toLocalDatetime(week1,DayOfWeek.Monday,tenAm,year) returns Some(week1Start.plusHours(10).toLocalDateTime)
		mockConverter.toLocalDatetime(week1,DayOfWeek.Monday,tenThirty,year) returns Some(week1Start.plusHours(10).plusMinutes(30).toLocalDateTime)

		val eo = occurrenceService.fromTimetableEvent(doubleOccurrenence,intervalIncludingTwoOccurrences)

		eo.size should be (1)
	}


}
