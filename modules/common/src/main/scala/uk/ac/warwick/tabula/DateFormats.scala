package uk.ac.warwick.tabula

import org.joda.time.format.DateTimeFormatterBuilder
import org.joda.time.format.ISODateTimeFormat
import org.joda.time.DateTimeFieldType._
import collection.JavaConversions._
import org.joda.time.format.DateTimeFormat

/**
 * Date format strings and formatter objects that can be used
 * throughout the application. Typically we'll only have a few
 * different date formats and use them consistently, for consistency.
 */
object DateFormats {
	/** String format as expected by Javascript date time picker */
	final val DateTimePicker = "dd-MMM-yyyy HH:mm:ss"

	/**
	 * ISO 8601 date format. Has date, hours, minutes, timezone, no millis.
	 * We don't really need seconds either but there's no simple method to
	 * make a formatter with no seconds but still having a timezone.
	 */
	final val IsoDateTime = ISODateTimeFormat.dateTimeNoMillis
	
	/** Date-time format used in Formsbuilder-style CSV export */
	final val CSVDateTime = DateTimeFormat.forPattern("dd/MM/yyyy HH:mm")
	
	/** Date format used in feedback report spreadsheet */
	final val CSVDate = DateTimeFormat.forPattern("dd/MM/yyyy")
	
}

