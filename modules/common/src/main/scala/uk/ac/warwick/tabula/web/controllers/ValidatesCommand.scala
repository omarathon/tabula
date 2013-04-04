package uk.ac.warwick.tabula.web.controllers
import org.springframework.validation.Errors
import org.springframework.validation.Validator
import scala.beans.BeanProperty
import uk.ac.warwick.tabula.validators.ClassValidator
import uk.ac.warwick.tabula.commands.SelfValidating
import scala.reflect.ClassTag

/**
 * Methods for setting custom validator stuff.
 *
 * Note that on its own, this just sets a validator property but doesn't
 * configure any validation to happen automatically. It's best to use this
 * by extending BaseController instead, as that sets up the data binder to
 * use this validator for commands marked with @Valid.
 */
trait ValidatesCommand {

	var validator: Validator = _

	/**
	 * When specifying a Validator for this controller, whether to
	 * keep the existing wired validator (which should be the globally autowired
	 * one handling annotation-based validation).
	 */
	protected var keepOriginalValidator: Boolean = true

	type ValidatorMethod[A] = (A, Errors) => Unit

	/**
	 * Defines a validator for the command based on a single method, so
	 * you don't have to create a separate validator class for it.
	 *
	 * If there's an existing globally set validator (such as the annotation
	 * processor), this validation will run in addition to it.
	 */
	@deprecated("Use validatesSelf[A <: SelfValidating] instead of this; validation logic should be tied in with the command", "31")
	def validatesWith[A : ClassTag](fn: ValidatorMethod[A]) = _validatesWith[A](fn)
	
	private def _validatesWith[A : ClassTag](fn: ValidatorMethod[A]) {
		if (validator != null) throw new IllegalStateException("Already set validator once")
		validator = new ClassValidator[A] {
			override def valid(target: A, errors: Errors) = fn(target, errors)
		}
	}

	/**
	 * If the command object implements SelfValidating, this will
	 * run its validation command when a @Valid object is requested.
	 */
	def validatesSelf[A <: SelfValidating : ClassTag] {
		_validatesWith[A] { (cmd, errors) => cmd.validate(errors) }
	}

	/**
	 * Like validatesWith but replaces the existing set validator (usually
	 * the annotation processor).
	 */
	@deprecated("Use validatesSelf[A <: SelfValidating] instead of this; validation logic should be tied in with the command", "31")
	def onlyValidatesWith[A](fn: ValidatorMethod[A])(implicit tag: ClassTag[A]) {
		keepOriginalValidator = false
		validatesWith(fn)
	}

}