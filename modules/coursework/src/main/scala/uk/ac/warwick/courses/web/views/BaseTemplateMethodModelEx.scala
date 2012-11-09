package uk.ac.warwick.courses.web.views

import java.util.List
import freemarker.template.TemplateMethodModelEx
import uk.ac.warwick.courses.JavaImports._
import collection.JavaConverters._
import freemarker.template.utility.DeepUnwrap
import freemarker.template.TemplateModel

/**
 * Handles some of the boilerplate of unwrapping model objects into regular objects.
 */
abstract class BaseTemplateMethodModelEx extends TemplateMethodModelEx {

	def execMethod(args: Seq[_]): Object

	override def exec(args: JList[_]): Object = execMethod(unwrapArgs(args))

	protected def unwrapArgs(list: JList[_]) =
		list.asScala.toSeq.map { model =>
			DeepUnwrap.unwrap(model.asInstanceOf[TemplateModel])
		}
}