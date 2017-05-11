package uk.ac.warwick.tabula.data.model.forms

import uk.ac.warwick.tabula.web.views.MarkdownRendererImpl

trait FormattedHtml extends MarkdownRendererImpl {
	/**
	 * Return a formatted version of the text that can be inserted
	 * WITHOUT escaping.
	 */
	def formattedHtml(string: Option[String]): String = string.map { s => renderMarkdown(scala.xml.Utility.escape(s)) }.getOrElse("")
	def formattedHtml(string: String): String = formattedHtml(Option(string))
}