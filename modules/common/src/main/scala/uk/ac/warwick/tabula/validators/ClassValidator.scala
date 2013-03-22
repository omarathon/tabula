package uk.ac.warwick.tabula.validators

import org.springframework.validation.Validator
import org.springframework.validation.Errors
import scala.reflect._

/**
 * Version of Validator that handles the supports() method for you,
 * using an implicit ClassManifest to work out what T is. You just
 * have to implement `valid`.
 */
abstract class ClassValidator[A : ClassTag] extends Validator {

	def valid(target: A, errors: Errors)

	final override def validate(target: Object, errors: Errors) {
		valid(target.asInstanceOf[A], errors)
	}

	final override def supports(clazz: Class[_]) = compatible(clazz)
	private def compatible(clazz: Class[_]) = classTag[A].runtimeClass.isAssignableFrom(clazz)

	def rejectValue(property: String, code: String)(implicit errors: Errors) =
		errors.rejectValue(property, code)

}