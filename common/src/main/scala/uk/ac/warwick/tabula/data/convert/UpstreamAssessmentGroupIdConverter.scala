package uk.ac.warwick.tabula.data.convert

import org.springframework.beans.factory.annotation.Autowired
import uk.ac.warwick.tabula.data.model._
import uk.ac.warwick.tabula.services.AssessmentMembershipService
import uk.ac.warwick.tabula.system.TwoWayConverter

class UpstreamAssessmentGroupIdConverter extends TwoWayConverter[String, UpstreamAssessmentGroup] {

  @Autowired var service: AssessmentMembershipService = _

  // Converter used for binding request
  override def convertRight(id: String): UpstreamAssessmentGroup = service.getUpstreamAssessmentGroup(id).orNull

  // Formatter used for generating textual value in template
  override def convertLeft(uag: UpstreamAssessmentGroup): String = Option(uag).map(_.id).orNull

}
