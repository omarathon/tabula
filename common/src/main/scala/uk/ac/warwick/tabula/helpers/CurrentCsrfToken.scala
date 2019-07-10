package uk.ac.warwick.tabula.helpers

import freemarker.template.TemplateMethodModelEx
import uk.ac.warwick.tabula.RequestInfo

class CurrentCsrfToken extends TemplateMethodModelEx {
  override def exec(args: java.util.List[_]): Object = {
    RequestInfo.fromThread.get.csrfToken
  }
}
