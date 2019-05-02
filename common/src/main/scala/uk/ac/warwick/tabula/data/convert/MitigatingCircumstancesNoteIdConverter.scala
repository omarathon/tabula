package uk.ac.warwick.tabula.data.convert

import uk.ac.warwick.spring.Wire
import uk.ac.warwick.tabula.data.model.mitcircs.MitigatingCircumstancesNote
import uk.ac.warwick.tabula.helpers.StringUtils._
import uk.ac.warwick.tabula.services.mitcircs.MitCircsSubmissionService
import uk.ac.warwick.tabula.system.TwoWayConverter

class MitigatingCircumstancesNoteIdConverter extends TwoWayConverter[String, MitigatingCircumstancesNote] {

  var service: MitCircsSubmissionService = Wire[MitCircsSubmissionService]

  override def convertRight(id: String): MitigatingCircumstancesNote =
    id.maybeText.flatMap(service.getNoteById).orNull

  override def convertLeft(note: MitigatingCircumstancesNote): String =
    Option(note).map(_.id).orNull

}
