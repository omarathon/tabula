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
   * apply() is called when you call the object like Mav(..)
   * 
   * objects is a repeated list of Pairs. The Map()
   * constructor accepts the same kind of pair list, so "objects:_*" is used to pass the argument
   * list through as separate arguments rather than as a single list argument.
   */
  def apply(view:String, objects:Pair[String,_]*) = new ModelAndView(view).addAllObjects(Map(objects:_*))
  
}