package uk.ac.warwick.courses.web.controllers
import org.springframework.web.servlet.ModelAndView
import collection.JavaConversions._

/**
 * ModelAndView builder. Takes a viewname and then any number of Pair objects (which awesomely can be
 * created with the syntax a->b).
 * 
 * Mav("my/viewname", 
 *   "objName" -> obj, 
 *   "user" -> myUser
 *   )
 */
object Mav {
  /**
   * Invoking this object as though it's a function will call apply().
   * 
   * As Map() accepts a repeated list of Pairs we can tell it to pass those through as a list of arguments 
   * by using (objects:_*)
   */
  def apply(view:String, objects:Pair[String,_]*) = new ModelAndView(view).addAllObjects(Map(objects:_*))
  
}