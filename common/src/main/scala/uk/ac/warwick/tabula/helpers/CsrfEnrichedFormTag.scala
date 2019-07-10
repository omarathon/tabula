package uk.ac.warwick.tabula.helpers

import javax.servlet.jsp.tagext.Tag
import org.springframework.web.servlet.tags.form.{FormTag, TagWriter}
import uk.ac.warwick.sso.client.CSRFFilter
import uk.ac.warwick.tabula.RequestInfo

class CsrfEnrichedFormTag extends FormTag {
  override def writeTagContent(tagWriter: TagWriter): Int = {
    val retVal = super.writeTagContent(tagWriter)
    if (retVal == Tag.EVAL_BODY_INCLUDE) {
      tagWriter.startTag("input")
      writeOptionalAttribute(tagWriter, "type", "hidden")
      writeOptionalAttribute(tagWriter, "name", CSRFFilter.CSRF_TOKEN_PROPERTY_NAME)
      writeOptionalAttribute(tagWriter, "value", RequestInfo.fromThread.get.csrfToken)
      tagWriter.endTag()
    }
    retVal
  }
}
