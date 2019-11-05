package uk.ac.warwick.tabula.commands.scheduling.imports

import uk.ac.warwick.tabula.AcademicYear
import uk.ac.warwick.tabula.commands.{Command, Description}
import uk.ac.warwick.tabula.data.model.AssessmentComponent
import uk.ac.warwick.tabula.permissions.Permissions
import uk.ac.warwick.tabula.services.AutowiringAssessmentMembershipServiceComponent

import scala.jdk.CollectionConverters._

class RemoveMissingAssessmentComponentCommand(assessmentComponent: AssessmentComponent) extends Command[Unit] with AutowiringAssessmentMembershipServiceComponent {

  PermissionCheck(Permissions.ImportSystemData)

  override def applyInternal(): Unit = {
    assessmentComponent.links.asScala foreach (link => assessmentMembershipService.delete(link))
    assessmentMembershipService.delete(assessmentComponent)
  }

  override def describe(d: Description): Unit = {
    d.properties(
      "links" -> assessmentComponent.links.asScala.map(_.toString),
      "moduleCode" -> assessmentComponent.moduleCode,
      "name" -> assessmentComponent.name,
      "sequence" -> assessmentComponent.sequence,
      "thisYearsMembers" -> assessmentComponent.upstreamAssessmentGroups(AcademicYear.now()).flatMap(_.members.asScala).map(_.universityId),
    )
  }
}
