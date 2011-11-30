package uk.ac.warwick.courses.web.controllers
import org.springframework.validation.Errors
import org.springframework.validation.Validator
import scala.reflect.BeanProperty
import uk.ac.warwick.courses.helpers.ClassValidator

trait ValidatesCommand {

  @BeanProperty var validator:Validator =_
  
  type ValidatorMethod[T] = (T, Errors) => Unit  
  
  /**
   * Defines a validator for the command based on a single method, so
   * you don't have to create a separate validator class for it.
   */
  def validatesWith[T](fn:ValidatorMethod[T]) {
	validator = new ClassValidator[T] {
		override def valid(target:T, errors:Errors) = fn(target, errors)
	}
  }
	
}