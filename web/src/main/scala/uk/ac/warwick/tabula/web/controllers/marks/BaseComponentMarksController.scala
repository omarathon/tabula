package uk.ac.warwick.tabula.web.controllers.marks

import org.springframework.web.bind.annotation.{ModelAttribute, PathVariable}
import uk.ac.warwick.tabula.commands.marks.ClearRecordedModuleMarks
import uk.ac.warwick.tabula.commands.marks.MarksDepartmentHomeCommand.StudentModuleMarkRecord
import uk.ac.warwick.tabula.commands.{MemberOrUser, SelfValidating}
import uk.ac.warwick.tabula.data.model.{Member, ModuleRegistration, UpstreamAssessmentGroup}
import uk.ac.warwick.tabula.services.marks.AutowiringModuleRegistrationMarksServiceComponent
import uk.ac.warwick.tabula.services.{AutowiringModuleRegistrationServiceComponent, AutowiringProfileServiceComponent, AutowiringUserLookupComponent}
import uk.ac.warwick.tabula.web.controllers.BaseController
import uk.ac.warwick.userlookup.User

import scala.jdk.CollectionConverters._

/**
 * Common base controller for component marks. Must have a RequestMapping with the following @PathVariables:
 *
 * - @PathVariable assessmentComponent: AssessmentComponent
 * - @PathVariable upstreamAssessmentGroup: UpstreamAssessmentGroup
 */
abstract class BaseComponentMarksController extends BaseController
  with AutowiringProfileServiceComponent
  with AutowiringUserLookupComponent
  with AutowiringModuleRegistrationServiceComponent
  with AutowiringModuleRegistrationMarksServiceComponent {

  validatesSelf[SelfValidating]

  @ModelAttribute("membersByUniversityId")
  def membersByUniversityId(@PathVariable upstreamAssessmentGroup: UpstreamAssessmentGroup): Map[String, MemberOrUser] = {
    val universityIds: Seq[String] = upstreamAssessmentGroup.members.asScala.map(_.universityId).toSeq
    val members: Map[String, Member] = profileService.getAllMembersWithUniversityIds(universityIds).map(m => m.universityId -> m).toMap
    val missingUniversityIds: Seq[String] = universityIds.filterNot(members.contains)

    if (missingUniversityIds.nonEmpty) {
      val users: Map[String, User] = userLookup.usersByWarwickUniIds(missingUniversityIds)

      members.view.mapValues(MemberOrUser(_)).toMap ++ users.view.mapValues(MemberOrUser(_)).toMap
    } else {
      members.view.mapValues(MemberOrUser(_)).toMap
    }
  }

  @ModelAttribute("willClearModuleMarksForUniversityId")
  def willClearModuleMarksForUniversityId(@PathVariable upstreamAssessmentGroup: UpstreamAssessmentGroup): Map[String, Option[StudentModuleMarkRecord]] = {
    val universityIds: Seq[String] = upstreamAssessmentGroup.members.asScala.map(_.universityId).toSeq
    val moduleRegistrations: Seq[ModuleRegistration] =
      moduleRegistrationService.getByModuleOccurrence(upstreamAssessmentGroup.moduleCode, upstreamAssessmentGroup.academicYear, upstreamAssessmentGroup.occurrence)

    universityIds.map { universityId =>
      universityId ->
        moduleRegistrations.find(_.studentCourseDetails.student.universityId == universityId).flatMap { moduleRegistration =>
          ClearRecordedModuleMarks.shouldClear(moduleRegistration)(moduleRegistrationMarksService)
        }
    }.toMap
  }

}
