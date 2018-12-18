package uk.ac.warwick.tabula.web.views

import freemarker.template.TemplateMethodModelEx
import uk.ac.warwick.util.web.UriBuilder

import scala.util.Try

class UriParser extends TemplateMethodModelEx {
	override def exec(args: java.util.List[_]): Object = Try {
		args.get(0).toString
	}.toOption.map(UriBuilder.parse).getOrElse("")
}
