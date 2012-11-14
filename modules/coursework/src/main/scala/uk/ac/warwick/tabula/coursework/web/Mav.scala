package uk.ac.warwick.tabula.coursework.web

import scala.collection.JavaConversions._
import scala.collection.mutable
import org.springframework.web.servlet.ModelAndView
import uk.ac.warwick.tabula.coursework.data.model
import org.springframework.web.servlet.View

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
class Mav() {

	def this(viewName: String) = {
		this()
		this.viewName = viewName
	}

	def this(view: View) = {
		this()
		this.view = view
	}

	var view: View = _
	var viewName: String = _

	var map = mutable.Map[String, Any]()

	// CSS classes to add to <body>. See #bodyClasses
	var classes: List[String] = Nil

	def addObjects(items: Pair[String, Any]*) = {
		map ++= items
		this
	}

	// Set the CSS body classes to these strings.
	def bodyClasses(c: String*) = {
		classes = c.toList
		this
	}

	/**
	 * Change to a non-default layout template.
	 *
	 * @see #noLayout
	 */
	def layout(name: String) = addObjects("renderLayout" -> name)

	/**
	 * Add a list of breadcrumbs to display in the navigation.
	 */
	def crumbs(pages: BreadCrumb*) = addObjects("breadcrumbs" -> pages.toSeq)

	/**
	 * Sets the layout parameter to "none" to
	 * render the template without any surrounding stuff.
	 *
	 * Also sets embedded->true so views can decide whether to
	 * render certain elements.
	 */
	def noLayout() = {
		addObjects("embedded" -> true)
		layout("none")
	}

	def noNavigation() = {
		addObjects("embedded" -> true)
		layout("nonav")
	}

	def embedded = {
		addObjects("embedded" -> true)
		layout("embedded")
	}

	/**
	 * CustomFreemarkerServlet will use this value if present to set the content type of the response.
	 */
	def contentType(contentType: String) = {
		addObjects("contentType" -> contentType)
		this
	}

	/** Removes layout and sets contentType to text/xml */
	def xml() = {
		noLayout()
		contentType("text/xml")
	}

	/**
	 * Changes to noLayout if the given thing is true.
	 */
	def noLayoutIf(bool: Boolean): Mav =
		if (bool) noLayout()
		else this

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
	
	override def toString = {
		val v = if (view != null) view else viewName
		"Mav("+v+", "+map+")"
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
	def apply(view: String, objects: Pair[String, _]*) = new Mav(view).addObjects(objects: _*)

	def apply(view: View, objects: Pair[String, _]*) = new Mav(view).addObjects(objects: _*)
	//def apply(what:String) = null

}