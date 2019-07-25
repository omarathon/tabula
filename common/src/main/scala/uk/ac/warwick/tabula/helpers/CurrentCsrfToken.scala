package uk.ac.warwick.tabula.helpers

import freemarker.template.TemplateMethodModelEx
import uk.ac.warwick.sso.client.CSRFFilter
import uk.ac.warwick.tabula.RequestInfo

class CurrentCsrfToken extends TemplateMethodModelEx {

  case class CsrfTokenNameValuePair(
    tokenName: String,
    tokenValue: String
  )

  override def exec(args: java.util.List[_]): Object = {
    CsrfTokenNameValuePair(CSRFFilter.CSRF_TOKEN_PROPERTY_NAME, RequestInfo.fromThread.get.csrfToken)
  }
}
