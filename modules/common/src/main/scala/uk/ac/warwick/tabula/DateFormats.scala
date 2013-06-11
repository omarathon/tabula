package uk.ac.warwick.tabula

import org.joda.time.format.ISODateTimeFormat
import org.joda.time.format.DateTimeFormat

/**
 * Date format strings and formatter objects that can be used
 * throughout the application. Typically we'll only have a few
 * different date formats and use them consistently, for consistency.
 */
object DateFormats {
	/** String format as expected by Javascript date time picker */
	final val DateTimePicker = "dd-MMM-yyyy HH:mm:ss"
	final val DatePicker = "dd-MMM-yyyy"
	final val TimePicker = "HH:mm:ss"
	final val NotificationDatePattern = "d MMMM yyyy 'at' HH:mm:ss"
	final val CSVDatePattern = "dd/MM/yyyy"       // we need the pattern for SpreadsheetHelpers
	final val CSVDateTimePattern = "dd/MM/yyyy HH:mm"

	/**
	 * ISO 8601 date format. Has date, hours, minutes, timezone, no millis.
	 * We don't really need seconds either but there's no simple method to
	 * make a formatter with no seconds but still having a timezone.
	 */
	final val IsoDateTime = ISODateTimeFormat.dateTimeNoMillis
	
	/** Date-time format used in Formsbuilder-style CSV export */
	final val CSVDateTime = DateTimeFormat.forPattern(CSVDateTimePattern)

	/** Date format used in spreadsheets */
	final val CSVDate = DateTimeFormat.forPattern(CSVDatePattern)

	/** Date format used in emails */
	final val NotificationDate = DateTimeFormat.forPattern(NotificationDatePattern)

}

