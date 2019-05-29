package uk.ac.warwick.tabula.data.convert

import uk.ac.warwick.spring.Wire
import uk.ac.warwick.tabula.data.model.mitcircs.MitigatingCircumstancesPanel
import uk.ac.warwick.tabula.services.mitcircs.MitCircsPanelService
import uk.ac.warwick.tabula.system.TwoWayConverter

class MitigatingCircumstancesPanelConverter extends TwoWayConverter[String, MitigatingCircumstancesPanel] {
  var service: MitCircsPanelService = Wire.auto[MitCircsPanelService]
  override def convertRight(id: String): MitigatingCircumstancesPanel = Option(id).flatMap(service.get).orNull
  override def convertLeft(panel: MitigatingCircumstancesPanel): String = Option(panel).map(_.id).orNull
}
