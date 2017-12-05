package uk.ac.warwick.tabula.helpers

import java.time.temporal.{ChronoField => JChronoField}
import java.{time => javatime}

import org.joda.{time => jodatime}
import uk.ac.warwick.tabula.helpers.Decorators._

import scala.language.implicitConversions

object JodaConverters extends DecorateAsJavaTime with DecorateAsJodaTime

trait DecorateAsJavaTime {
	implicit def asJava(i: jodatime.LocalDate): AsJava[javatime.LocalDate] =
		new AsJava(javatime.LocalDate.of(i.getYear, i.getMonthOfYear, i.getDayOfMonth))

	implicit def asJava(i: jodatime.LocalDateTime): AsJava[javatime.LocalDateTime] =
		new AsJava(javatime.LocalDateTime.of(i.getYear, i.getMonthOfYear, i.getDayOfMonth, i.getHourOfDay, i.getMinuteOfHour, i.getSecondOfMinute, i.getMillisOfSecond))
}

trait DecorateAsJodaTime {
	implicit def asJoda(i: javatime.LocalDate): AsJoda[jodatime.LocalDate] =
		new AsJoda(new jodatime.LocalDate(i.getYear, i.getMonthValue, i.getDayOfMonth))

	implicit def asJoda(i: javatime.LocalDateTime): AsJoda[jodatime.LocalDateTime] =
		new AsJoda(new jodatime.LocalDateTime(i.getYear, i.getMonthValue, i.getDayOfMonth, i.getHour, i.getMinute, i.getSecond, i.get(JChronoField.MILLI_OF_SECOND)))
}

private[helpers] object Decorators extends Decorators

private[helpers] trait Decorators {
  class AsJava[A](op: => A) {
    def asJava: A = op
  }

  class AsJoda[A](op: => A) {
    def asJoda: A = op
  }
}