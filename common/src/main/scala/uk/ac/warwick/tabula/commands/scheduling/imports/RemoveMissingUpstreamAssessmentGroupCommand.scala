package uk.ac.warwick.tabula.commands.scheduling.imports

import uk.ac.warwick.tabula.commands.{Command, Description}
import uk.ac.warwick.tabula.data.Transactions.transactional
import uk.ac.warwick.tabula.data.model.UpstreamAssessmentGroup
import uk.ac.warwick.tabula.permissions.Permissions
import uk.ac.warwick.tabula.services.AutowiringAssessmentMembershipServiceComponent

import scala.collection.JavaConverters._

class RemoveMissingUpstreamAssessmentGroupCommand(upstreamAssessmentGroup: UpstreamAssessmentGroup) extends Command[Unit] with AutowiringAssessmentMembershipServiceComponent {

  PermissionCheck(Permissions.ImportSystemData)

  override def applyInternal(): Unit = {
    transactional() {
      assessmentMembershipService.delete(upstreamAssessmentGroup)
    }
  }

  override def describe(d: Description): Unit =
    d.property("upstreamAssessmentGroup" -> upstreamAssessmentGroup.toString)
     .studentIds(upstreamAssessmentGroup.members.asScala.map(_.universityId))
}
