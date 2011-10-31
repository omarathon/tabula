package uk.ac.warwick.courses.validators;

import javax.validation.ConstraintValidator
import javax.validation.ConstraintValidatorContext
import org.springframework.expression.ExpressionParser
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.expression.Expression
import org.springframework.expression.spel.standard.SpelExpressionParser

/**
 * Hibernate validators has @ScriptAssert which lets you make scripted validation checks.
 * Spring has SpEL which is a good script language, but it's not in the Java Scripting JSR
 * so use this thing we made.
 */
class SpelAssertValidator extends ConstraintValidator[SpelAssert, Object] {
  
	@Autowired var parser:ExpressionParser =_
	var expression: Expression =_
  
	override def initialize(annotation:SpelAssert) {
	  expression = parser.parseExpression(annotation.value())
	}
	
	override def isValid(value:Object, ctx:ConstraintValidatorContext) = value match {
	  case value:Object => expression.getValue(value, classOf[Boolean])
	  case _ => true // nothing to run against
	}
}
