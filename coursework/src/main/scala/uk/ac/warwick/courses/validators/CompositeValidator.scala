package uk.ac.warwick.courses.validators

import org.springframework.validation.Errors
import org.springframework.validation.Validator
import uk.ac.warwick.courses.JavaImports._
import collection.JavaConversions._

class CompositeValidator(val list: Validator*) extends Validator {

	// Java compat
	def this(list: JList[Validator]) = this(list: _*)

	override def supports(cls: Class[_]) = list.exists { _.supports(cls) }
	override def validate(target: Object, errors: Errors) =
		for (v <- list if v.supports(target.getClass))
			v.validate(target, errors)
}