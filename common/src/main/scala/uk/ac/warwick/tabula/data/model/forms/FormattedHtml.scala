package uk.ac.warwick.tabula.data.model.forms

import freemarker.core.{HTMLOutputFormat, TemplateHTMLOutputModel}
import uk.ac.warwick.tabula.web.views.MarkdownRenderer

trait FormattedHtml {
  /**
    * Return a formatted version of the text that can be inserted
    * WITHOUT escaping.
    */
  def formattedHtml(string: Option[String]): TemplateHTMLOutputModel = HTMLOutputFormat.INSTANCE.fromMarkup {
    string.map { s => MarkdownRenderer.renderMarkdown(scala.xml.Utility.escape(s)) }.getOrElse("")
  }

  def formattedHtml(string: String): TemplateHTMLOutputModel = formattedHtml(Option(string))
}