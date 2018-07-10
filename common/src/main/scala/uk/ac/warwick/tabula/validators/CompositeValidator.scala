package uk.ac.warwick.tabula.validators

import org.springframework.validation.Errors
import org.springframework.validation.Validator
import uk.ac.warwick.tabula.JavaImports._
import scala.collection.JavaConverters._

class CompositeValidator(val list: Validator*) extends Validator {

	// Java compat
	def this(list: JList[Validator]) = this(list.asScala: _*)

	override def supports(cls: Class[_]): Boolean = list.exists { _.supports(cls) }
	override def validate(target: Object, errors: Errors): Unit =
		for (v <- list if v.supports(target.getClass))
			v.validate(target, errors)
}