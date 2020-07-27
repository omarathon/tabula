package uk.ac.warwick.tabula.api.web.helpers

import uk.ac.warwick.tabula.DateFormats
import uk.ac.warwick.tabula.data.model.forms.ExtensionState
import uk.ac.warwick.tabula.helpers.cm2.ExtensionListItem

trait ExtensionToJsonConvertor {
  def jsonExtension(enhancedExtension: ExtensionListItem): Map[String, Any] = {
    val extension = enhancedExtension.extension

    Map(
      "id" -> extension.id,
      "state" -> extension.state.description,
      "expired" -> (extension.state == ExtensionState.Approved && !enhancedExtension.within),
      "expiryDate" -> extension.expiryDate.map(DateFormats.IsoDateTime.print).orNull,
      "requestedExpiryDate" -> extension.requestedExpiryDate.map(DateFormats.IsoDateTime.print).orNull
    )
  }
}
