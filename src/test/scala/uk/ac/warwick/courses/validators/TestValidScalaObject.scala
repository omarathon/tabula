package uk.ac.warwick.courses.validators
import scala.reflect.BeanProperty
import annotation.target.field

class TestValidScalaObject {
  def this(n:String) {
    this()
    name = n
  }
  
  @HasLetter(letter='e')
  @BeanProperty var name:String =_
}
    
