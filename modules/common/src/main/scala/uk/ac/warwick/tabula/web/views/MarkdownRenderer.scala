package uk.ac.warwick.tabula.web.views

import org.pegdown.{PegDownProcessor, Extensions}

trait MarkdownRenderer {
	def renderMarkdown(source: String): String
}

trait MarkdownRendererImpl extends MarkdownRenderer {
	private val markdownOptions = Extensions.AUTOLINKS
	private val markdown = new PegDownProcessor(markdownOptions)

	override def renderMarkdown(source: String) =
		markdown.markdownToHtml(source)
}