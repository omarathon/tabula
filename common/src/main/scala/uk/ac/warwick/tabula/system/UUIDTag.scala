package uk.ac.warwick.tabula.system

import java.util
import java.util.UUID

import freemarker.template.TemplateMethodModelEx

class UUIDTag extends TemplateMethodModelEx {
  override def exec(unused: util.List[_]): String =
    UUID.randomUUID().toString
}
