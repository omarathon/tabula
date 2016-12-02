package uk.ac.warwick.tabula.web.views

import org.pegdown.{PegDownProcessor, Extensions}

trait MarkdownRenderer {
	def renderMarkdown(source: String): String
}

trait MarkdownRendererImpl extends MarkdownRenderer {
	private val markdownOptions = Extensions.AUTOLINKS
	private val markdownMaxParseTimeMs = 5 * 1000 // 5 seconds (2 seconds is default)

	// PegDownProcessor not threadsafe, so we get a new one each time.
	def markdown = new PegDownProcessor(markdownOptions, markdownMaxParseTimeMs)

	override def renderMarkdown(source: String): String =
		markdown.markdownToHtml(source)
}