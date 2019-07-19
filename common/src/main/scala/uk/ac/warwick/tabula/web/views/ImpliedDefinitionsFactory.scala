package uk.ac.warwick.tabula.web.views

import org.apache.tiles.definition.UnresolvingLocaleDefinitionsFactory
import org.apache.tiles.request.Request
import org.apache.tiles.{Attribute, Definition}
import uk.ac.warwick.tabula.helpers.Logging

import scala.collection.JavaConverters._
import scala.util.Try

/**
  * DefinitionsFactory for Tiles, which first tries the default behaviour
  * of checking the XML files. If none is found and the request view is not
  * an absolute path, it generates a tiles definition on the fly, using a base
  * layout and using the view name to generate a Freemarker path to the body
  * template.
  *
  * e.g. "time/view" will use a body template of /WEB-INF/freemarker/time/view.ftl
  */
class ImpliedDefinitionsFactory extends UnresolvingLocaleDefinitionsFactory with Logging {

  final val FreemarkerRoot = "/WEB-INF/freemarker/"
  final val Extensions = Seq(
    ".ftlh",
    ".ftl"
  )
  final val LayoutAttribute = "renderLayout"
  final val DefaultLayout = "base"
  final val BodyTileAttribute = "body"

  /*
   * Return a definition if found in XML. Otherwise if it doesn't start with /,
   * generate our own definition based on the base layout
   */
  override def getDefinition(name: String, ctx: Request): Definition = {
    if (debugEnabled) logger.debug("Rendering " + name)
    super.getDefinition(name, ctx) match {
      case definition: Any => definition
      case _ if !name.startsWith("/") =>
        val template = Extensions
          .map(extension => FreemarkerRoot + name + extension)
          .find(f => Try(ctx.getApplicationContext.getResource(f) != null).getOrElse(false))

        new Definition(layoutDefinition(ctx)) {
          template.foreach { template =>
            addAll(Map(BodyTileAttribute -> new Attribute(template)).asJava)
          }
        }
      case _ => null // Let it be handled by FreemarkerServlet
    }
  }

  def layoutDefinition(ctx: Request): Definition = {
    val layout = layoutTemplate(ctx)
    if (debugEnabled) logger.debug("Using layout template " + layout)
    super.getDefinition(layout, ctx)
  }

  def layoutTemplate(ctx: Request): String =
    if (ctx.getAvailableScopes.asScala.contains(Request.REQUEST_SCOPE))
      ctx.getContext(Request.REQUEST_SCOPE).get(LayoutAttribute) match {
        case name: String => name.trim
        case _ => DefaultLayout
      }
    else DefaultLayout

}