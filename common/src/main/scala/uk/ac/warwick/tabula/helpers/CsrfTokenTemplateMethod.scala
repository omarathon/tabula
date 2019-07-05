import freemarker.template.TemplateMethodModelEx

class CsrfTokenTemplateMethod extends TemplateMethodModelEx {
  override def exec(args: java.util.List[_]): Object = {

    val currentUser = RequestInfo.fromThread.get.user

    userSettings.getByUserId(currentUser.apparentId) match {
      case Some(settings) => settings.string(setting)
      case _ => null
    }
  }
}
