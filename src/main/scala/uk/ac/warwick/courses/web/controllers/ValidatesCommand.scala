package uk.ac.warwick.courses.web.controllers
import org.springframework.validation.Errors
import org.springframework.validation.Validator
import scala.reflect.BeanProperty
import uk.ac.warwick.courses.helpers.ClassValidator
import uk.ac.warwick.courses.commands.SelfValidating

/**
 * Methods for setting custom validator stuff.
 * 
 * Note that on its own, this just sets a validator property but doesn't
 * configure any validation to happen automatically. It's best to use this
 * by extending Controllerism instead, as that sets up the data binder to
 * use this validator for commands marked with @Valid.
 */
trait ValidatesCommand {

  @BeanProperty var validator:Validator =_
  
  /**
   * When specifying a Validator for this controller, whether to
   * keep the existing wired validator (which should be the globally autowired
   * one handling annotation-based validation). 
   */
  protected var keepOriginalValidator:Boolean = true
  
  type ValidatorMethod[T] = (T, Errors) => Unit  
  
  /**
   * Defines a validator for the command based on a single method, so
   * you don't have to create a separate validator class for it.
   * 
   * If there's an existing globally set validator (such as the annotation
   * processor), this validation will run in addition to it.
   */
  def validatesWith[T](fn:ValidatorMethod[T]) {
	if (validator != null) throw new IllegalStateException("Already set validator once")
	validator = new ClassValidator[T] {
		override def valid(target:T, errors:Errors) = fn(target, errors)
	}
  }
  
  /**
   * If the command object implements SelfValidating, this will
   * run its validation command when a @Valid object is requested.
   */
  def validatesSelf[T <: SelfValidating] {
	  validatesWith[T] { (cmd, errors) => cmd.validate(errors) }
  }
  
  /**
   * Like validatesWith but replaces the existing set validator (usually
   * the annotation processor).
   */
  def onlyValidatesWith[T] (fn:ValidatorMethod[T]) {
	  keepOriginalValidator = false
	  validatesWith(fn)
  }
  
  class CompositeValidator(val list:Validator*) extends Validator {
	  override def supports(cls:Class[_]) = list.find{_.supports(cls)}.isDefined
	  override def validate(target:Object, errors:Errors) =
	 	  for (v <- list) v.validate(target, errors)
  }
	
}