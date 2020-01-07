package uk.ac.warwick.tabula.helpers

import org.springframework.core.io.ClassPathResource
import uk.ac.warwick.tabula.TestBase

import scala.util.Using

class DetectMimeTypeTest extends TestBase {

  @Test
  def detect(): Unit = {
    Using.resource(new ClassPathResource("/feedback1.zip").getInputStream) { is =>
      val mimeType = DetectMimeType.detectMimeType(is).toString
      mimeType should be("application/zip")
    }
  }

}
