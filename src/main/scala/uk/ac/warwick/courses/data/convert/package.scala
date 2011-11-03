package uk.ac.warwick.courses.data

import uk.ac.warwick.courses.ItemNotFoundException

package object convert 
{
  /*
   * Adds a method getOrDie to an Option object which will throw
   * a custom exception if it's None
   */
  @SuppressWarnings(Array("unchecked"))
  implicit def optionToThrowingOption[T](option:Option[T])(implicit m:Manifest[T]) = new {
    def getOrDie:T = option match {
      case Some(t:Any) if m.erasure.isInstance(t) => t.asInstanceOf[T]
	  case _ => throw new ItemNotFoundException() 
	}
  }
}