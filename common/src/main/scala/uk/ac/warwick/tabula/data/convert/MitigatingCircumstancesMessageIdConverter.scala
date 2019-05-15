package uk.ac.warwick.tabula.data.convert

import uk.ac.warwick.spring.Wire
import uk.ac.warwick.tabula.data.model.mitcircs.MitigatingCircumstancesMessage
import uk.ac.warwick.tabula.helpers.StringUtils._
import uk.ac.warwick.tabula.services.mitcircs.MitCircsSubmissionService
import uk.ac.warwick.tabula.system.TwoWayConverter

class MitigatingCircumstancesMessageIdConverter extends TwoWayConverter[String, MitigatingCircumstancesMessage] {

  var service: MitCircsSubmissionService = Wire[MitCircsSubmissionService]

  override def convertRight(id: String): MitigatingCircumstancesMessage =
    id.maybeText.flatMap(service.getMessageById).orNull

  override def convertLeft(message: MitigatingCircumstancesMessage): String =
    Option(message).map(_.id).orNull

}
