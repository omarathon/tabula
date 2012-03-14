package uk.ac.warwick.courses.helpers

import org.springframework.validation.Validator
import org.springframework.validation.Errors

/**
 * Version of Validator that handles the supports() method for you,
 * using an implicit ClassManifest to work out what T is. You just
 * have to implement `valid`.
 */
abstract class ClassValidator[T:ClassManifest] extends Validator {
	
	def valid(target:T, errors:Errors)
	
	final override def validate(target: Object, errors: Errors) = valid(target.asInstanceOf[T], errors)
	final override def supports(clazz: Class[_]) = classManifest[T].erasure.isAssignableFrom(clazz)

	def rejectValue(property:String, code:String)(implicit errors:Errors) =	errors.rejectValue(property, code)
		
}