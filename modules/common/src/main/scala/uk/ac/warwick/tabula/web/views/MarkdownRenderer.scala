package uk.ac.warwick.tabula.web.views

import org.pegdown.{PegDownProcessor, Extensions}

trait MarkdownRenderer {
	def renderMarkdown(source: String): String
}

trait MarkdownRendererImpl extends MarkdownRenderer {
	private val markdownOptions = Extensions.AUTOLINKS
	// PegDownProcessor not threadsafe, so we get a new one each time.
	def markdown = new PegDownProcessor(markdownOptions)

	override def renderMarkdown(source: String) =
		markdown.markdownToHtml(source)
}