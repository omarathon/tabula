package uk.ac.warwick.tabula.helpers

import com.google.i18n.phonenumbers.{NumberParseException, PhoneNumberUtil}
import com.google.i18n.phonenumbers.PhoneNumberUtil.PhoneNumberFormat._
import uk.ac.warwick.tabula.web.views.BaseTemplateMethodModelEx

/**
  * Formats a phone number into a local format (for GB) or international format (for non-GB)
  */
object PhoneNumberFormatter {

  private val util = PhoneNumberUtil.getInstance()
  private val homeRegion = "GB"
  private val homeCountryCode = util.getCountryCodeForRegion(homeRegion)

  /** Print phone number in this format for UK:
    *
    * 07580 123456
    *
    * or this format if non-UK:
    *
    * +33 1234 576 8941
    */
  def format(unformatted: String): String = {
    try {
      val number = util.parseAndKeepRawInput(unformatted, homeRegion)

      val format =
        if (number.getCountryCode == homeCountryCode) NATIONAL
        else INTERNATIONAL

      util.format(number, format)
    } catch {
      // We don't understand how to parse this number
      case e: NumberParseException => unformatted
    }
  }
}

/**
  * Companion class for FreeMarker
  */
class PhoneNumberFormatter extends BaseTemplateMethodModelEx {

  import PhoneNumberFormatter.format

  /** Single argument method */
  override def execMethod(args: Seq[_]): String =
    args match {
      case Seq(unformatted: String) => format(unformatted)
      case _ => throw new IllegalArgumentException("Bad args")
    }
}
