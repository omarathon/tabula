package uk.ac.warwick.courses.web.views

import java.util.List
import freemarker.template.TemplateMethodModel
import freemarker.template.TemplateDirectiveModel
import freemarker.template.TemplateDirectiveBody
import freemarker.template.TemplateModel
import freemarker.core.Environment
import org.springframework.beans.factory.annotation.Value

/**
 * 
 */
class UrlMethodModel extends TemplateDirectiveModel {
  
	@Value("${toplevel.url}") var toplevelUrl:String = _
  
	override def execute(env: Environment, 
		    params: java.util.Map[_,_], 
		    loopVars: Array[TemplateModel], 
		    body: TemplateDirectiveBody ) {
	
	  val path:String = if (params.containsKey("page")) {
	    params.get("page").toString()
	  } else if (params.containsKey("resource")) {
	    params.get("resource").toString()
	  } else {
	    throw new IllegalArgumentException("")
	  }
	  
	  val writer = env.getOut()
	  writer.write(toplevelUrl)
	  writer.write(path)
	  
	}
	

}