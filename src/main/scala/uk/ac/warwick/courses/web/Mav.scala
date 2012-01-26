package uk.ac.warwick.courses.web
import org.springframework.web.servlet.ModelAndView
import collection.JavaConversions._
import collection.mutable

/**
 * Scala-y modelandview object. There is a return value processor
 * that teaches Spring what to do with a Mav returned from a controller
 * method, so you can return these from methods.
 * 
 * Mav("my/viewname", 
 *   "objName" -> obj, 
 *   "user" -> myUser
 *   )
 */
class Mav(var viewName:String) {
	var map = mutable.Map[String,Any]()
	var classes:List[String] = Nil
	
	def addObjects(items:Pair[String,Any]*) = {
		map ++= items
		this
	}
	
	def bodyClasses(c:String*) = {
	 	classes = c.toList
	 	this
	}
	
	/**
	 * Change to a non-default layout template.
	 * 
	 * @see #noLayout
	 */
	def layout(name:String) = {
		map += "renderLayout" -> name
		this
	}
	
	/**
	 * Sets the layout parameter to "none" to
	 * render the template without any surrounding stuff.
	 */
	def noLayout = layout("none")
	
	def toModel = {
		map ++ bodyClassesItem
	}
	
	private def bodyClassesItem = classes match {
		case Nil => Nil
		case _ => List("bodyClasses" -> classes.mkString(" "))
	}
		
	def toModelAndView = {
		val mav = new ModelAndView(viewName)
		mav.addAllObjects(toModel)
		mav
	}
}

/**
 * ModelAndView builder. Takes a viewname and then any number of Pair objects (which awesomely can be
 * created with the syntax a->b).
 * 
 */
object Mav {
  /**
   * apply() is called when you call the object like Mav(..)
   * 
   * objects is a repeated list of Pairs. The Map()
   * constructor accepts the same kind of pair list, so "objects:_*" is used to pass the argument
   * list through as separate arguments rather than as a single list argument.
   */
  def apply(view:String, objects:Pair[String,_]*) = new Mav(view).addObjects(objects:_*)

  //def apply(what:String) = null
  
}