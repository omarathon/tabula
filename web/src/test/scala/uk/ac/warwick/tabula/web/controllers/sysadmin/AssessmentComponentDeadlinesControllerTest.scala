package uk.ac.warwick.tabula.web.controllers.sysadmin

import org.joda.time.{DateTimeConstants, LocalDate}
import uk.ac.warwick.tabula.TestBase
import uk.ac.warwick.tabula.web.controllers.sysadmin.AssessmentComponentDeadlinesController._

class AssessmentComponentDeadlinesControllerTest extends TestBase {

  @Test def deadlineParsing(): Unit = {
    parseDeadlineDate("02-06-2020") should be (new LocalDate(2020, DateTimeConstants.JUNE, 2))
    parseDeadlineDate("11-Feb-20") should be (new LocalDate(2020, DateTimeConstants.FEBRUARY, 11))
    parseDeadlineDate("21-05-2020") should be (new LocalDate(2020, DateTimeConstants.MAY, 21))
    parseDeadlineDate("10-Mar-20") should be (new LocalDate(2020, DateTimeConstants.MARCH, 10))
    parseDeadlineDate("12/4/19") should be (new LocalDate(2019, DateTimeConstants.DECEMBER, 4))
    parseDeadlineDate("1/9/20") should be (new LocalDate(2020, DateTimeConstants.JANUARY, 9))
    parseDeadlineDate("Sep-20") should be (new LocalDate(2020, DateTimeConstants.SEPTEMBER, 1))
    parseDeadlineDate("WEEK 40") should be (new LocalDate(2020, DateTimeConstants.JUNE, 29))
    parseDeadlineDate("week 40") should be (new LocalDate(2020, DateTimeConstants.JUNE, 29))
    parseDeadlineDate("17.1.2020") should be (new LocalDate(2020, DateTimeConstants.JANUARY, 17))
  }

}
