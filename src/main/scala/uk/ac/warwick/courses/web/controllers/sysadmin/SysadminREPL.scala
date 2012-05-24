package uk.ac.warwick.courses.web.controllers.sysadmin

import uk.ac.warwick.courses.web.controllers.BaseController
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.stereotype.Controller
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
import uk.ac.warwick.courses.data.model._
import uk.ac.warwick.courses.services._
import collection.JavaConversions._
import java.util.HashMap
import org.springframework.expression.spel.SpelEvaluationException
import org.hibernate.Session

@Controller
@RequestMapping(value=Array("/sysadmin/repl"))
class SysadminREPL extends BaseController with BeanFactoryAware {
	
	@BeanProperty var beanFactory:BeanFactory =_
	@Autowired var assignmentService:AssignmentService =_
	@Autowired var moduleAndDepartmentService:ModuleAndDepartmentService =_
	
	val spel:SpelExpressionParser = new SpelExpressionParser
	
	@RequestMapping
	def evaluate(@RequestParam(value="query", defaultValue="") query:String) = {
		val response = if (query.hasText) {
			val context = new StandardEvaluationContext(RootObject(session))
			val expression = spel.parseExpression(query)
			try Return(expression.getValue(context))
			catch { case e:Exception => Return(null, e) }
		} else {
			Return(null)
		}
		Mav("sysadmin/repl", "query" -> query, "response" -> response)
	}
	
	/**
	 * Root object for evaluator. Its properties and methods will be available
	 * as top-level items in your SpEL query.
	 */
	case class RootObject(
		@BeanProperty val session:Session,
		@BeanProperty val beanFactory:BeanFactory = beanFactory,
		// expose Assignments as a map of ids
		@BeanProperty val assignments:MapAccessor[Assignment] = MapAccessor { id =>
			assignmentService.getAssignmentById(id).orNull
		},
		@BeanProperty val departmentCodes:MapAccessor[Department] = MapAccessor { code =>
			moduleAndDepartmentService.getDepartmentByCode(code).orNull
		}
	)
}


/**
 * Implements a Java Map that only has a working get() method, to allow
 * you to expose some collection of objects as a String-keyed map.
 */
abstract class MapAccessor[T] extends JMap[String,T] {
	def fetch(key:String):T 
	override def get(key:Any) = fetch(key.asInstanceOf[String]) 
	override def put(key:String, value:T) = throw strop
	override def keySet = throw strop
	override def entrySet = throw strop
	override def values = throw strop
	override def putAll(map:JMap[_ <: String, _ <: T]) = throw strop
	override def size = 1
	override def isEmpty = false
	override def clear = throw strop
	override def containsKey(s:Any) = throw strop
	override def containsValue(v:Any) = throw strop
	override def remove(v:Any) = throw strop
		
	def strop = new UnsupportedOperationException
}

object MapAccessor {
	/**
	 * Creates a MapAccessor that uses the given function to resolve
	 * the string to an object.
	 */
	def apply[T](fn: String=>T) = new MapAccessor[T] {
		override def fetch(id:String) = fn(id)
	}
}

case class Return(val value:Any, val exception:Exception = null) {
	lazy val stringValue = value.toString
	lazy val isNone = value match {
		case None => true
		case _ => false
	}
	lazy val stackTrace = {
		val stringer = new StringWriter
		val writer = new PrintWriter(stringer)
		exception.printStackTrace(writer)
		writer.close
		stringer.toString
	}
	lazy val valueType = {
		value match {
			case Some(any:Any) => any.getClass.getSimpleName
			case any:Any => any.getClass.getSimpleName
			case _ => "?"
		}
	}
}