package uk.ac.warwick.tabula.data.convert

import java.util.concurrent.TimeUnit

import uk.ac.warwick.tabula.system.TwoWayConverter

import scala.concurrent.duration.{Duration, FiniteDuration}

object FiniteDurationConverter {
  def asString(duration: FiniteDuration): String = duration.toString()

  /**
   * Parses a duration string. If no units are specified in the string, it is
   * assumed to be in milliseconds. The returned duration is in nanoseconds.
   * The purpose of this function is to implement the duration-related methods
   * in the ConfigObject interface.
   *
   * From Typesafe SimpleConfig
   *
   * @param input
   *              the string to parse
   * @return duration as a Scala FiniteDuration
   */
  def asDuration(input: String): FiniteDuration = {
    val unitStringRaw: String = input.reverse.takeWhile(Character.isLetter).reverse
    val numberStringRaw: String = input.substring(0, input.length - unitStringRaw.length).trim
    val numberString =
      if (numberStringRaw.endsWith(".")) numberStringRaw.substring(0, numberStringRaw.length - 1)
      else numberStringRaw

    require(numberString.length > 0, s"No number in period value '$input'")

    val unitString =
      if (unitStringRaw.length > 2 && !unitStringRaw.endsWith("s")) s"${unitStringRaw}s"
      else unitStringRaw

    // Note that this is deliberately case-sensitive
    val unit: TimeUnit = unitString match {
      case "" | "ms" | "millis" | "milliseconds" => TimeUnit.MILLISECONDS
      case "us" | "micros" | "microseconds" => TimeUnit.MICROSECONDS
      case "ns" | "nanos" | "nanoseconds" => TimeUnit.NANOSECONDS
      case "d" | "days" => TimeUnit.DAYS
      case "h" | "hours" => TimeUnit.HOURS
      case "s" | "seconds" => TimeUnit.SECONDS
      case "m" | "minutes" => TimeUnit.MINUTES
      case _ => throw new IllegalArgumentException(s"Could not parse time unit '$unitStringRaw' (try ns, us, ms, s, m, h, d)")
    }

    Duration(numberString.toLong, unit)
  }
}

class FiniteDurationConverter extends TwoWayConverter[String, FiniteDuration] {
  override def convertRight(source: String): FiniteDuration = FiniteDurationConverter.asDuration(source)
  override def convertLeft(source: FiniteDuration): String = FiniteDurationConverter.asString(source)
}
