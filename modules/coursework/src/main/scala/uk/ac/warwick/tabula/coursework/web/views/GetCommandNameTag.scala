package uk.ac.warwick.tabula.coursework.web.views

import freemarker.template.TemplateMethodModel
import org.springframework.web.servlet.tags.form.FormTag
import uk.ac.warwick.tabula.JavaImports._
import freemarker.template.TemplateDirectiveModel
import freemarker.template.TemplateDirectiveBody
import freemarker.template.TemplateModel
import freemarker.core.Environment

class GetCommandNameTag extends TemplateDirectiveModel {
	val modelAttributeVariableName = FormTag.MODEL_ATTRIBUTE_VARIABLE_NAME

	override def execute(env: Environment,
		params: java.util.Map[_, _],
		loopVars: Array[TemplateModel],
		body: TemplateDirectiveBody) = {

		val varName = Option(params.get("var")).getOrElse("commandName")

		//			env.get
		//			
		//			env.setLocalVariable(varName, new StringModel())
		//			body.render(env.getOut)

	}

}