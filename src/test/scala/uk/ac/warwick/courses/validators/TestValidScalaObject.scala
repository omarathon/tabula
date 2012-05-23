package uk.ac.warwick.courses.validators
import scala.reflect.BeanProperty
import annotation.target.field
import org.hibernate.validator.constraints.NotEmpty

class TestValidScalaObject {
  def this(n:String) {
    this()
    name = n
  }
  
  @NotEmpty
  @BeanProperty var name:String =_
}
    
