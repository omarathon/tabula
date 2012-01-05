package uk.ac.warwick.courses.web.views
import freemarker.ext.beans.BeansWrapper
import freemarker.template.TemplateDateModel
import freemarker.template.Configuration
import freemarker.template.TemplateExceptionHandler
import org.springframework.web.context.ServletContextAware
import collection.JavaConversions._
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Required
import collection.mutable
import org.springframework.web.servlet.tags.form.FormTag

/**
 * Adapted from http://code.google.com/p/sweetscala
 * 
 * Sets Freemarker to use ScalaBeansWrapper will tells it how to
 * deal with the various Scala collection classes, so we don't
 * have to convert them to Java collections manually every time.
 */
class ScalaFreemarkerConfiguration extends Configuration with ServletContextAware {
  //Default constructor initialzation
  this.setTagSyntax(Configuration.AUTO_DETECT_TAG_SYNTAX)
  this.setTemplateExceptionHandler(TemplateExceptionHandler.RETHROW_HANDLER)
  this.setAutoIncludes(List( "/WEB-INF/freemarker/prelude.ftl" ))
  
  val wrapper = new ScalaBeansWrapper
  wrapper.setMethodsShadowItems(false) // do not lookup method first.
  wrapper.setDefaultDateType(TemplateDateModel.DATETIME) //this allow java.util.Date to work from model.
  wrapper.setUseCache(true) //do caching by default.
  wrapper.setExposureLevel(BeansWrapper.EXPOSE_PROPERTIES_ONLY); //don't expose method, but properties only
  this.setObjectWrapper(wrapper)  
  
  @Required 
  def setSharedVariables(vars:java.util.Map[String,Any]) {
	this.setSharedVariable("commandVarName", FormTag.MODEL_ATTRIBUTE_VARIABLE_NAME)
    for ((key,value) <- vars) this.setSharedVariable(key,value)
  }
  
  override def setServletContext(ctx:javax.servlet.ServletContext) =
    	setServletContextForTemplateLoading(ctx, "/")
}