package uk.ac.warwick.courses.helpers

import org.joda.time.DateTime
import org.joda.time.format.DateTimeFormat
import org.joda.time.format.DateTimeFormatter
import collection.mutable
import collection.JavaConversions._
import freemarker.template.TemplateMethodModelEx
import freemarker.template.utility.DeepUnwrap
import freemarker.template.TemplateModel
import uk.ac.warwick.courses.JavaImports._

class DateBuilder extends TemplateMethodModelEx {
	
	private val dayAndDateFormat = DateTimeFormat.forPattern("EEE d")
	private val monthAndYearFormat = DateTimeFormat.forPattern(" MMMM yyyy")
	private val formatterMap = new DateTimeFormatterCache
	
	private val _relativeWords = Map(
		'yesterday -> "Yesterday",
		'today -> "Today",
		'tomorrow -> "Tomorrow"
	) 
	private val relativeWords = Map(
		true -> _relativeWords,
		false -> _relativeWords.mapValues(_.toLowerCase)
	)
	
	def format(date:DateTime, includeSeconds:Boolean, includeAt:Boolean, includeTimezone:Boolean, capitalise:Boolean) = {
		val pattern = new StringBuilder
		if (includeAt) pattern.append(" 'at'")
		pattern.append(" HH:mm")
		if (includeSeconds) pattern.append(":ss")
		if (includeTimezone) pattern.append(" (z)")
		
		datePart(date, capitalise) + (formatterMap(pattern.toString) print date)
	}
	
	def ordinal(day:Int) = day%10 match {
		case _ if (day >= 10 && day <= 20) => "th"
		case 1 => "st"
		case 2 => "nd"
		case 3 => "rd"
		case _ => "th"
	}
	
	private def datePart(date:DateTime, capitalise:Boolean) = {
		val today = new DateTime().toDateMidnight
		val thatDay = date.toDateMidnight
		
		if (today isEqual thatDay) relativeWords(capitalise)('today)
		else if (today.minusDays(1) isEqual thatDay) relativeWords(capitalise)('yesterday)
		else if (today.plusDays(1) isEqual thatDay) relativeWords(capitalise)('tomorrow)
		else (dayAndDateFormat print date) + 
			 ordinal(date.getDayOfMonth) +
			 (monthAndYearFormat print date)
	}
	
		
	/** For Freemarker */
	override def exec(list:java.util.List[_]) = {
		val args = list.toSeq.map{model => DeepUnwrap.unwrap(model.asInstanceOf[TemplateModel])}
		args match {
			case Seq(date:DateTime, secs:JBoolean, at:JBoolean, tz:JBoolean, caps:JBoolean) => 
				format(date, secs, at, tz, caps)
			case _ => throw new IllegalArgumentException("Bad args")
		}
	}
	
	class DateTimeFormatterCache extends mutable.HashMap[String, DateTimeFormatter] {
		override def default(pattern:String) = DateTimeFormat.forPattern(pattern)
	}
}