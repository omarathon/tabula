package uk.ac.warwick.courses.web.controllers.sysadmin

import uk.ac.warwick.courses.web.controllers.BaseController
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.stereotype.Controller
import tools.nsc.interpreter.IMain
import tools.nsc.Settings
import tools.nsc.interpreter.IR.Result
import org.springframework.web.bind.annotation.RequestParam
import java.io.PrintWriter
import java.io.StringWriter
import org.springframework.beans.factory.BeanFactoryAware
import org.springframework.beans.factory.BeanFactory
import scala.reflect.BeanProperty
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.expression.spel.standard.SpelExpressionParser
import org.springframework.expression.Expression
import org.springframework.expression.ParserContext
import org.springframework.expression.spel.support.StandardEvaluationContext
import uk.ac.warwick.courses.JavaImports._
import collection.JavaConversions._
import java.util.HashMap
import org.springframework.expression.spel.SpelEvaluationException
import org.hibernate.Session

@Controller
@RequestMapping(value=Array("/sysadmin/repl"))
class SysadminREPL extends BaseController with BeanFactoryAware {
	
	@BeanProperty var beanFactory:BeanFactory = _
	
	val spel:SpelExpressionParser = new SpelExpressionParser
	
	@RequestMapping
	def evaluate(@RequestParam(value="query", defaultValue="") query:String) = {
		val response = if (query.hasText) {
			val context = new StandardEvaluationContext(ContextVariables(
					session,
					beanFactory		
			))
			val expression = spel.parseExpression(query)
			try Return(expression.getValue(context))
			catch { case e:Exception => Return(null, e) }
		} else {
			Return(null)
		}
		Mav("sysadmin/repl", "query" -> query, "response" -> response)
	}

}

case class ContextVariables(
		@BeanProperty val session:Session,
		@BeanProperty val beanFactory:BeanFactory
)

case class Return(val value:Any, val exception:Exception = null) {
	lazy val stringValue = value.toString
	lazy val isNone = value match {
		case None => true
		case _ => false
	}
}