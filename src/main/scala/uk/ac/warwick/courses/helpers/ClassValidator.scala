package uk.ac.warwick.courses.helpers

import org.springframework.validation.Validator
import org.springframework.validation.Errors

/**
 * Version of Validator that handles the supports() method for you
 * 
 */
abstract class ClassValidator[T] extends Validator {

	def valid(target:T, errors:Errors)
	
	override def validate(target: Object, errors: Errors) {
		valid(target.asInstanceOf[T], errors)
	}
	
	override def supports(clazz: Class[_]) = compatible(clazz)

	def compatible[T](clazz:Class[_])(implicit manifest:ClassManifest[T]) = 
		manifest.erasure.isAssignableFrom(clazz)

}