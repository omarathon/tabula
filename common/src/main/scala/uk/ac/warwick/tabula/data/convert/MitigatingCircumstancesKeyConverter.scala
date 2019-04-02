package uk.ac.warwick.tabula.data.convert

import uk.ac.warwick.spring.Wire
import uk.ac.warwick.tabula.data.model.mitcircs.MitigatingCircumstancesSubmission
import uk.ac.warwick.tabula.services.mitcircs.MitCircsSubmissionService
import uk.ac.warwick.tabula.system.TwoWayConverter

import scala.util.Try

class MitigatingCircumstancesKeyConverter extends TwoWayConverter[String, MitigatingCircumstancesSubmission] {

  var service: MitCircsSubmissionService = Wire.auto[MitCircsSubmissionService]

  override def convertRight(key: String): MitigatingCircumstancesSubmission = Try(key.toLong).toOption.flatMap(service.getByKey).orNull

  override def convertLeft(submission: MitigatingCircumstancesSubmission): String = Option(submission).map(_.key.toString).orNull

}
