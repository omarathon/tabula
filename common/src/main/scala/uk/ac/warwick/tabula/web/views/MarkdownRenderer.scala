package uk.ac.warwick.tabula.web.views

import org.commonmark.ext.autolink.AutolinkExtension
import org.commonmark.parser.Parser
import org.commonmark.renderer.html.HtmlRenderer
import uk.ac.warwick.tabula.JavaImports._

trait MarkdownRenderer {
	def renderMarkdown(source: String): String
}

trait MarkdownRendererImpl extends MarkdownRenderer {
	override def renderMarkdown(source: String): String = MarkdownRenderer.renderMarkdown(source)
}

object MarkdownRenderer {
	private val markdownParser = Parser.builder().extensions(JList(AutolinkExtension.create())).build()
	private val markdownRenderer = HtmlRenderer.builder().build()

	def renderMarkdown(source: String): String =
		markdownRenderer.render(markdownParser.parse(source))
}