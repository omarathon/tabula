package uk.ac.warwick.courses

import scala.collection.JavaConversions._

import org.apache.tiles.context.TilesRequestContext
import org.apache.tiles.definition._
import uk.ac.warwick.courses.helpers.Logging
import org.apache.tiles.Definition
import org.apache.tiles.Attribute
import scala.collection.mutable

/**
 * DefinitionsFactory for Tiles, which first tries the default behaviour
 * of checking the XML files. If none is found and the request view is not
 * an absolute path, it generates a tiles definition on the fly, using a base
 * layout and using the view name to generate a Freemarker path to the body
 * template.
 * 
 * e.g. "time/view" will use a body template of /WEB-INF/freemarker/time/view.ftl
 */
class SmartDefinitionsFactory extends UrlDefinitionsFactory with Logging {

  /*
   * Return a definition if found in XML. Otherwise if it doesn't start with /,
   * generate our own definition based on the base layout
   */
  override def getDefinition(name: String, ctx: TilesRequestContext) = {
    logger info("Rendering " + name)
    val request = ctx.getRequestScope()
    val name2 = name // TODO figure out why using name directly ends up null
    super.getDefinition(name, ctx) match {
      case definition:Any => definition
      case _ if !name2.startsWith("/") => new Definition(layoutDefinition(ctx)) {
        addAll(Map(
            "body" -> new Attribute("/WEB-INF/freemarker/"+name2+".ftl")
        ))
  	  }
      case _ => null
    }
  }
  
  def layoutDefinition(ctx:TilesRequestContext) = {
    val layout = layoutTemplate(ctx)
    logger info("Using layout template " + layout)
    super.getDefinition(layout, ctx)
  }
  
  def layoutTemplate(ctx:TilesRequestContext) = {
    if (ctx.getRequestScope != null) {
      ctx.getRequestScope.get("renderLayout") match {
	      case name:String => name.trim
	      case _ => "base"
	    }
    } else {
     	"base"
    }
  }
  
}