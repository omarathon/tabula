package uk.ac.warwick.tabula.helpers

import org.joda.time.{DateTime, Duration}
import uk.ac.warwick.tabula.JavaImports._
import uk.ac.warwick.tabula.web.views.BaseTemplateMethodModelEx

class DurationPercentageTag extends BaseTemplateMethodModelEx {
	override def execMethod(args: Seq[_]): JInteger = args match {
		case Seq(null, null) | Seq(null, null, null) => 0
		case Seq(start: DateTime, end: DateTime) => DurationPercentage.format(start, DateTime.now, end)
		case Seq(start: DateTime, now: DateTime, end: DateTime) => DurationPercentage.format(start, now, end)
		case _ => throw new IllegalArgumentException("Bad args")
	}
}

/**
	* Actually formats Intervals, not Durations.
	*/
object DurationPercentage {

	/**
		* Prints the progress between a duration, as a whole integer between 0 and 100
		*/
	def format(start: DateTime, now: DateTime, end: DateTime): Int =
		if ((now isAfter end) || (now isEqual end))
			100
		else if ((now isBefore start) || (now isEqual start))
			0
		else if ((start isAfter end) || (start isEqual end))
			100
		else {
			val wholeDuration = new Duration(start, end).getMillis
			val currentDuration = new Duration(start, now).getMillis

			((currentDuration.toDouble * 100.0) / wholeDuration.toFloat).toInt
		}

}