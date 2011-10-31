package uk.ac.warwick.courses.data

import uk.ac.warwick.courses.ItemNotFoundException

package object convert 
{
  /*
   * Adds a method getOrDie to an Option object which will throw
   * a custom exception if it's None
   */
  @unchecked
  implicit def optionToThrowingOption[T](option:Option[T]) = new {
    def getOrDie:T = option match {
	  case None => throw new ItemNotFoundException() 
	  case Some(t:T) => t
	}
  }
}