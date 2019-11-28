package uk.ac.warwick.tabula.helpers

import org.joda.time.ReadablePartial
import uk.ac.warwick.tabula.JavaImports._
import uk.ac.warwick.tabula.web.views.BaseTemplateMethodModelEx

object TimeBuilder {

  private val formatterMap = new DateTimeFormatterCache

  /* called with just a DateTime - use the default arguments */
  def format(time: ReadablePartial): String =
    format(time = time,
      twentyFourHour = true,
      includeSeconds = false)

  /* everything is specified, including whether minutes should be included */
  def format(time: ReadablePartial,
    twentyFourHour: Boolean,
    includeSeconds: Boolean): String = {
    val pattern = new StringBuilder

    if (twentyFourHour) pattern.append("HH:mm")
    else pattern.append("h:mm")

    if (includeSeconds) pattern.append(":ss")

    if (!twentyFourHour) pattern.append("a")

    // We convert the output to lowercase because we don't want AM/PM, we want am/pm
    (formatterMap.retrieve(pattern.toString()) print time).trim().toLowerCase
  }

}

class TimeBuilder extends BaseTemplateMethodModelEx {

  import TimeBuilder.format

  /** For Freemarker */
  override def execMethod(args: Seq[_]): String = {
    val time = args.head match {
      case partial: ReadablePartial => partial
      case _ => throw new IllegalArgumentException("Bad time argument")
    }

    args.tail match {
      case Seq(twentyFourHour: JBoolean, secs: JBoolean) =>
        format(time = time,
          twentyFourHour = twentyFourHour,
          includeSeconds = secs)
      case _ => throw new IllegalArgumentException("Bad args")
    }
  }
}
