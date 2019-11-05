package uk.ac.warwick.tabula.data.model.forms

import freemarker.core.{HTMLOutputFormat, TemplateHTMLOutputModel}
import uk.ac.warwick.tabula.web.views.MarkdownRenderer

object FormattedHtml {
  /**
    * Return a formatted version of the text that can be inserted
    * WITHOUT escaping.
    */
  def apply(string: Option[String]): TemplateHTMLOutputModel = HTMLOutputFormat.INSTANCE.fromMarkup {
    string.map { s => MarkdownRenderer.renderMarkdown(scala.xml.Utility.escape(s)) }.getOrElse("")
  }

  def apply(string: String): TemplateHTMLOutputModel = apply(Option(string))
}