package uk.ac.warwick.tabula.web.views

import freemarker.template.TemplateMethodModel
import freemarker.template.TemplateDirectiveModel
import freemarker.template.TemplateDirectiveBody
import freemarker.template.TemplateModel
import freemarker.core.Environment
import uk.ac.warwick.spring.Wire
import java.util.Properties
import freemarker.template.SimpleScalar
import java.net.URLEncoder
import uk.ac.warwick.util.web.EscapingUriParser
import uk.ac.warwick.tabula.JavaImports._

/**
 *
 */
class UrlMethodModel extends TemplateDirectiveModel with TemplateMethodModel {
  
	var context = Wire[String]("${module.context}")

	var toplevelUrl = Wire[String]("${toplevel.url}")

	var staticHashes = Wire[Properties]("staticHashes")
	
	val parser = new EscapingUriParser
	
	private def rewrite(path: String, contextOverridden: Option[String]) = {
		val contextNoRoot = contextOverridden.getOrElse(context) match {
			case "/" => ""
		  case context => context
		}
		
		contextNoRoot + path
	}
	
	override def exec(args: JList[_]): TemplateModel = {
		if (args.size >= 1) {
			val contextOverridden = 
				if (args.size > 1) Option(args.get(1).toString())
				else None
			
			val prependTopLevelUrl = 
				if (args.size > 2) args.get(2) match {
					case b: Boolean => b
					case "true" => true
					case _ => false
				} else false
							
			val prefix = 
				if (prependTopLevelUrl) toplevelUrl
				else ""
				
			new SimpleScalar(prefix + encode(rewrite(args.get(0).toString(), contextOverridden)))
	  	} else {
	  		throw new IllegalArgumentException("")
	  	}
	}

	override def execute(env: Environment,
		params: JMap[_, _],
		loopVars: Array[TemplateModel],
		body: TemplateDirectiveBody) {

		val path: String = if (params.containsKey("page")) {
			val contextOverridden = 
			  if (params.containsKey("context")) Option(params.get("context").toString())
			  else None
			  
			rewrite(params.get("page").toString(), contextOverridden)
		} else if (params.containsKey("resource")) {
			addSuffix(params.get("resource").toString())
		} else {
			throw new IllegalArgumentException("")
		}

		val writer = env.getOut()
		writer.write(toplevelUrl)
		writer.write(encode(path))

	}
	
	def encode(url: String) = parser.parse(url).toString

	def addSuffix(path: String) = {
		staticHashes.getProperty(path.substring(1)) match {
			case hash: String => path + "." + hash
			case _ => path
		}
	}

}