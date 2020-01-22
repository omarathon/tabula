package uk.ac.warwick.tabula.data.convert

import uk.ac.warwick.tabula.TestBase
import scala.concurrent.duration._

class FiniteDurationConverterTest extends TestBase {

  val converter: FiniteDurationConverter = new FiniteDurationConverter

  val tests: Seq[(String, FiniteDuration)] = Seq(
    "0" -> Duration.Zero,
    "300" -> 300.millis,
    "1ms" -> 1.millisecond,
    "1 milli" -> 1.millisecond,
    "1 millisecond" -> 1.millisecond,
    "30us" -> 30.microseconds,
    "30 nanos" -> 30.nanoseconds,
    "1d" -> 1.day,
    "1 day" -> 1.day,
    "30d" -> 30.days,
    "30 days" -> 30.days,
    "1h" -> 1.hour,
    "1 hour" -> 1.hour,
    "30h" -> 30.hours,
    "30 hours" -> 30.hours,
    "1h" -> 1.hour,
    "1 hour" -> 1.hour,
    "30h" -> 30.hours,
    "30 hours" -> 30.hours,
    "1s" -> 1.second,
    "1 second" -> 1.second,
    "30s" -> 30.seconds,
    "30 seconds" -> 30.seconds,
    "1m" -> 1.minute,
    "1.minute" -> 1.minute,
    "30m" -> 30.minutes,
    "30 minutes" -> 30.minutes,
  )

  @Test def convertRight(): Unit = {
    tests.foreach { case (str, dur) =>
      converter.convertRight(str) should be (dur)
    }

    tests.map(_._2).distinct.foreach { dur =>
      converter.convertRight(dur.toString()) should be (dur)
      converter.convertLeft(dur) should be (dur.toString())
    }
  }

}
