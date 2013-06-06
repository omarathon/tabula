package uk.ac.warwick.tabula.helpers

import scala.collection.mutable
import org.joda.time.format.{DateTimeFormat, DateTimeFormatter}

private[helpers] class DateTimeFormatterCache {
	private val map = mutable.HashMap[String, DateTimeFormatter]()
	def retrieve(pattern: String) = map.getOrElseUpdate(pattern, DateTimeFormat.forPattern(pattern))
}