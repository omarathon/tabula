package uk.ac.warwick.courses.validators
import org.springframework.validation.Validator
import org.springframework.validation.Errors

abstract class BaseValidator[T](globalValidator:Validator) extends Validator {
	override def supports(cls:Class[_]) = cls match {
	  case t:T => true
	  case _ => false
	}
  
	final override def validate(command:Any, errors:Errors) {
	  command match {
	    case command:T => {
	      globalValidator.validate(command, errors)
		  customValidate(command, errors)
	    }
	    case _ => throw new IllegalArgumentException("Validator does not support this class: " + command.getClass.getName)
	  }
	}
	
	def customValidate(command:T, errors:Errors)
}