package uk.ac.warwick.courses.web.views

import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpServletResponse
import freemarker.template.TemplateModel
import freemarker.template.Template
import freemarker.ext.servlet.FreemarkerServlet
import uk.ac.warwick.sso.client.SSOClientFilter
import freemarker.template.SimpleHash
import freemarker.template.Configuration
import javax.servlet.ServletConfig
import uk.ac.warwick.courses.helpers.Logging

/**
 * FreemarkerServlet which adds some useful model stuff to every request.
 */
class CustomFreemarkerServlet extends FreemarkerServlet() with Logging {
  
  var config:Configuration =_
  
  logger.info("Creating custom freemarker servlet")
  
  /**
   * Add items to the model that should be available to every Freemarker view
   */
  override def preTemplateProcess(req:HttpServletRequest,res:HttpServletResponse,template:Template,data:TemplateModel) = {
	val model = data.asInstanceOf[SimpleHash]
	model.put("user", SSOClientFilter.getUserFromRequest(req))
	true
  }
  
  /* FreemarkerServlet tries to create an object wrapper and set it on the config,
   * but we like the custom one that's already set so return that.
   */
  override def createObjectWrapper = config.getObjectWrapper
  
  /**
   * Spring config has a Configuration bean which we then export to the
   * servlet context. This then grabs that, so that we're using one shared
   * config (important because we set up a bunch of Scala conversions)
   */
  override def init {
    config = getServletConfig().getServletContext().getAttribute("freemarkerConfiguration") match {
      case c:Configuration => c
      case _ => throw new IllegalStateException("Couldn't find config in servlet attribute 'freemarkerConfiguration' - should have been exported by ServletContextAttributeExporter")
    }
	super.init
  }
  
  // Use the configuration that was wired into the constructor
  override def createConfiguration = config

}