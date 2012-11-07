package uk.ac.warwick.courses.web.views

import java.util.List
import freemarker.template.TemplateMethodModel
import freemarker.template.TemplateDirectiveModel
import freemarker.template.TemplateDirectiveBody
import freemarker.template.TemplateModel
import freemarker.core.Environment
import org.springframework.beans.factory.annotation.Value
import java.util.Properties
import javax.annotation.Resource

/**
 *
 */
class UrlMethodModel extends TemplateDirectiveModel {

	@Value("${toplevel.url}") var toplevelUrl: String = _

	@Resource(name = "staticHashes") var staticHashes: Properties = _

	override def execute(env: Environment,
		params: java.util.Map[_, _],
		loopVars: Array[TemplateModel],
		body: TemplateDirectiveBody) {

		val path: String = if (params.containsKey("page")) {
			params.get("page").toString()
		} else if (params.containsKey("resource")) {
			addSuffix(params.get("resource").toString())
		} else {
			throw new IllegalArgumentException("")
		}

		val writer = env.getOut()
		writer.write(toplevelUrl)
		writer.write(path)

	}

	def addSuffix(path: String) = {
		staticHashes.getProperty(path.substring(1)) match {
			case hash: String => path + "." + hash
			case _ => path
		}
	}

}