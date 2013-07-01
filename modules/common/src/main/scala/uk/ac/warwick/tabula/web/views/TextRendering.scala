package uk.ac.warwick.tabula.web.views

import freemarker.template.Configuration
import uk.ac.warwick.spring.Wire


trait TextRenderer {
  def renderTemplate(templateId:String, model:Any):String
}

trait FreemarkerTextRenderer extends TextRenderer with FreemarkerRendering{
  implicit var freemarker = Wire[Configuration]

  def renderTemplate(templateId:String, model:Any):String = renderToString(templateId, model)

}
