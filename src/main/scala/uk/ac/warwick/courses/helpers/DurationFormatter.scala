package uk.ac.warwick.courses.helpers
import org.joda.time.Duration

class SplitDuration(val days:Long, val hours:Long, val minutes:Long, val seconds:Long) {
	
}

object SplitDuration {
	def apply() = {
		
	}
}


object DurationFormatter {
	def format(duration:Duration):String = {
		val days = duration.getStandardDays()
		(plural(days,"day")) + hours(duration.minus(Duration.standardDays(days)))
	}
	
	def hours(duration:Duration) = {
		val hours = duration.getStandardHours
		" " + plural(hours,"hour") + minutes(duration.minus(Duration.standardHours(hours)))
	}
	
	def minutes(duration:Duration) = {
		val minutes = duration.getStandardMinutes
		" " + plural(minutes,"minute")
	}
	
	def plural(num:Long, unit:String) = num + " " + (num match {
		case 1 => unit
		case _ => unit + "s"
	})
}