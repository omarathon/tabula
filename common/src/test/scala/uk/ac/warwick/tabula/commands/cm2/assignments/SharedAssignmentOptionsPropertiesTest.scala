package uk.ac.warwick.tabula.commands.cm2.assignments

import org.springframework.validation.BindException
import uk.ac.warwick.tabula.TestBase
import scala.jdk.CollectionConverters._

class SharedAssignmentOptionsPropertiesTest extends TestBase {

  val properties: SharedAssignmentOptionsProperties = new SharedAssignmentOptionsProperties {}

  @Test def validateEmpty(): Unit = {
    val errors = new BindException(properties, "properties")
    properties.validateSharedOptions(errors)
    errors.hasErrors should be (false)
  }

  @Test def validateValidFileTypes(): Unit = {
    properties.fileAttachmentTypes.addAll(Seq(
      "txt", "docx", "c", "x_t", "m4a", "", "mp4", "tar.gz"
    ).asJava)

    val errors = new BindException(properties, "properties")
    properties.validateSharedOptions(errors)
    errors.hasErrors should be (false)
  }

  @Test def validateInvalidFileTypes(): Unit = {
    properties.fileAttachmentTypes.addAll(Seq(
      "txt;", "DOCX;", "c", ""
    ).asJava)

    val errors = new BindException(properties, "properties")
    properties.validateSharedOptions(errors)
    errors.hasErrors should be (true)
    errors.getErrorCount should be (1)
    errors.getFieldError("fileAttachmentTypes").getCode should be ("attachment.invalidChars")
  }

}
