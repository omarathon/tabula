package uk.ac.warwick.tabula.web.views

import freemarker.ext.beans.BeansWrapper
import freemarker.template.TemplateDateModel
import freemarker.template.Configuration
import freemarker.template.TemplateExceptionHandler
import org.springframework.web.context.ServletContextAware
import collection.JavaConversions._
import collection.mutable
import org.springframework.web.servlet.tags.form.FormTag
import uk.ac.warwick.tabula.JavaImports._
import org.springframework.beans.factory.annotation.Required

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
	this.setAutoIncludes(List("/WEB-INF/freemarker/prelude.ftl"))

	val wrapper = new ScalaBeansWrapper
	wrapper.setMethodsShadowItems(false) // do not lookup method first.
	wrapper.setDefaultDateType(TemplateDateModel.DATETIME) //this allow java.util.Date to work from model.
	
	// TAB-351 TAB-469 Don't enable caching	
	wrapper.setUseCache(false)
	wrapper.setExposureLevel(BeansWrapper.EXPOSE_SAFE);

	this.setObjectWrapper(wrapper)

	// Mainly for tests to run - if servlet context is never set, it will
	// use the classloader to find templates.
	setClassForTemplateLoading(getClass(), "/")

	@Required	def setSharedVariables(vars: JMap[String, Any]) {
		this.setSharedVariable("commandVarName", FormTag.MODEL_ATTRIBUTE_VARIABLE_NAME)
		for ((key, value) <- vars) this.setSharedVariable(key, value)
	}

	override def setServletContext(ctx: javax.servlet.ServletContext) = {
		setServletContextForTemplateLoading(ctx, "/")
	}
}