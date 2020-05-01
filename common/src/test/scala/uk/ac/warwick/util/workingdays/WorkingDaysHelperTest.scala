package uk.ac.warwick.util.workingdays

import org.joda.time.LocalDate
import org.junit.Ignore
import uk.ac.warwick.tabula.TestBase
import uk.ac.warwick.tabula.helpers.DateTimeOrdering._
import uk.ac.warwick.tabula.helpers.JodaConverters._

import scala.jdk.CollectionConverters._

class WorkingDaysHelperTest extends TestBase {

  //https://repo.elab.warwick.ac.uk/projects/SHARED/repos/warwick-utils/browse/modules/warwickutils-core/src/main/java/uk/ac/warwick/util/workingdays/workingdays.txt
  /**
    * This is a copy of the WorkingDaysHelperTest in Warwick Utils. If it starts failing, but the latest
    * Warwick Utils tests pass, you probably just need to update to the latest Warwick Utils.
    */
  @Test
  @Ignore("Time being ignore as new dates for 2020/21 have not been released.Once released remove this ignore")
  def enoughDates(): Unit = {
    val helper = new WorkingDaysHelperImpl()
    val dates = helper.getHolidayDates.asScala.toSeq.map(_.asJoda).sorted

    dates.last should be >= LocalDate.now.plusMonths(4)
  }

}
