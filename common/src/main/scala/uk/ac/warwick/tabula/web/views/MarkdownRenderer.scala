package uk.ac.warwick.tabula.web.views

import java.net.URI

import com.google.common.base.Predicate
import org.owasp.html.{ElementPolicy, HtmlPolicyBuilder, PolicyFactory, Sanitizers}
import org.commonmark.ext.autolink.AutolinkExtension
import org.commonmark.parser.Parser
import org.commonmark.renderer.html.HtmlRenderer
import uk.ac.warwick.tabula.JavaImports._

import scala.util.Try

trait MarkdownRenderer {
  def renderMarkdown(source: String): String
}

trait MarkdownRendererImpl extends MarkdownRenderer {
  override def renderMarkdown(source: String): String = MarkdownRenderer.renderMarkdown(source)
}

object MarkdownRenderer {

  val warwickUrlPredicate: Predicate[String] = (input: String) => {
    Try {
      val uri: URI = new URI(input)
      val host: String = uri.getHost
      !uri.isAbsolute || host.endsWith(".warwick.ac.uk") || host == "warwick.ac.uk"
    }.getOrElse(false)
  }

  private val addTargetBlankOnLinks: ElementPolicy = (elementName: String, attrs: JList[String]) => {
    if (attrs.isEmpty || !attrs.contains("href")) {
      // Reject the element
      null
    } else {
      if (!attrs.contains("target")) {
        attrs.add("target")
        attrs.add("_blank")
      }

      elementName
    }
  }

  private val sanitisePolicy: PolicyFactory = new HtmlPolicyBuilder()
    .allowElements(addTargetBlankOnLinks, "a")
    .allowUrlProtocols("https", "http")
    .allowAttributes("title", "target").onElements("a")
    .allowAttributes("href").matching(warwickUrlPredicate).onElements("a")
    .requireRelNofollowOnLinks()
    .allowCommonBlockElements
    .allowCommonInlineFormattingElements
    .disallowElements("script") // this should not be needed, but just being explicit here
    .toFactory

  private val markdownParser = Parser.builder().extensions(JList(AutolinkExtension.create())).build()
  private val markdownRenderer = HtmlRenderer.builder().build()

  def renderMarkdown(source: String): String = {
    sanitisePolicy.sanitize(markdownRenderer.render(markdownParser.parse(source)))
  }
}
