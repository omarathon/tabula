import freemarker.template.TemplateMethodModelEx

class CsrfTokenTemplateMethod extends TemplateMethodModelEx {
  override def exec(args: java.util.List[_]): Object = {
    RequestInfo.fromThread.get.csrfToken
  }
}
