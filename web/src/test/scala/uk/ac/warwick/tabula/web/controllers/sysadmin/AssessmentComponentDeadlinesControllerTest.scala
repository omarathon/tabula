package uk.ac.warwick.tabula.web.controllers.sysadmin

import org.joda.time.{DateTimeConstants, LocalDate}
import uk.ac.warwick.tabula.TestBase
import uk.ac.warwick.tabula.web.controllers.sysadmin.AssessmentComponentDeadlinesController.DeadlineDateFormats

import scala.util.Try

class AssessmentComponentDeadlinesControllerTest extends TestBase {

  @Test def dateFormatting(): Unit = {
    def parse(in: String): LocalDate =
      DeadlineDateFormats.map(df => Try(df.parseLocalDate(in)))
        .find(_.isSuccess)
        .map(_.get)
        .getOrElse(throw new IllegalArgumentException(s"Couldn't parse date $in"))

    parse("02-06-2020") should be (new LocalDate(2020, DateTimeConstants.JUNE, 2))
    parse("11-Feb-20") should be (new LocalDate(2020, DateTimeConstants.FEBRUARY, 11))
    parse("21-05-2020") should be (new LocalDate(2020, DateTimeConstants.MAY, 21))
    parse("10-Mar-20") should be (new LocalDate(2020, DateTimeConstants.MARCH, 10))
    parse("12/4/19") should be (new LocalDate(2019, DateTimeConstants.DECEMBER, 4))
    parse("1/9/20") should be (new LocalDate(2020, DateTimeConstants.JANUARY, 9))
  }

}
