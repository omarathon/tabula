package uk.ac.warwick.courses.services.turnitin

import org.joda.time.format.DateTimeFormat
import org.joda.time.DateTime

/**
 * Object handling dates and date formats with the Turnitin API.
 */
object TurnitinDates {
	val timestampFormat = DateTimeFormat.forPattern("YYYYMMddHHmm").withZoneUTC
	val dateFormat = DateTimeFormat.forPattern("YYYYMMdd")

	def gmtTimestamp = {
		val s = timestampFormat print DateTime.now
		s.substring(0, s.length - 1) // Must have only the first digit of the minutes! 
	}

	def yearsFromNow(years: Int) = dateFormat print DateTime.now.plusYears(years)
	def monthsFromNow(months: Int) = dateFormat print DateTime.now.plusMonths(months)
}