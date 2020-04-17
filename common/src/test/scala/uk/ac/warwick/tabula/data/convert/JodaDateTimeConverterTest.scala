package uk.ac.warwick.tabula.data.convert

import uk.ac.warwick.tabula.TestBase
import uk.ac.warwick.tabula.AcademicYear
import org.joda.time.DateTime

// scalastyle:off magic.number
class JodaDateTimeConverterTest extends TestBase {

  val converter = new JodaDateTimeConverter

  @Test def validInput: Unit = {
    converter.convertRight("10-Mar-2012 12:13:14") should be(new DateTime(2012, 3, 10, 12, 13, 14))
    converter.convertRight("2020-02-15T15:04:11.00+00:00") should be(new DateTime(2020, 2, 15, 15, 4, 11))
  }

  @Test def invalidInput: Unit = {
    converter.convertRight("5th April 1996") should be(null)
    converter.convertRight("") should be(null)
    converter.convertRight(null) should be(null)
  }

  @Test def formatting: Unit = {
    converter.convertLeft(new DateTime(2013, 12, 31, 12, 30, 0)) should be("31-Dec-2013 12:30:00")
    converter.convertLeft(null) should be(null)
  }

}
