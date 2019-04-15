package uk.ac.warwick.tabula.commands.mitcircs.submission

import org.joda.time.LocalDate
import org.springframework.validation.Errors
import play.api.libs.json._
import uk.ac.warwick.tabula.AcademicYear
import uk.ac.warwick.tabula.commands._
import uk.ac.warwick.tabula.commands.mitcircs.submission.MitCircsAffectedAssessmentsCommand._
import uk.ac.warwick.tabula.data.Transactions._
import uk.ac.warwick.tabula.data.model._
import uk.ac.warwick.tabula.helpers.DateTimeOrdering._
import uk.ac.warwick.tabula.permissions.Permissions
import uk.ac.warwick.tabula.services.{AssessmentMembershipServiceComponent, AutowiringAssessmentMembershipServiceComponent}
import uk.ac.warwick.tabula.system.permissions.{PermissionsChecking, PermissionsCheckingMethods, RequiresPermissionsChecking}

import scala.collection.JavaConverters._

case class UpstreamAffectedAssessment(module: Module, academicYear: AcademicYear, name: String, deadline: Option[LocalDate], assessmentComponent: AssessmentComponent, upstreamAssessmentGroups: Seq[UpstreamAssessmentGroup], tabulaAssignments: Seq[Assignment])
object UpstreamAffectedAssessment {
  import JodaWrites._

  implicit val writesAcademicYear: Writes[AcademicYear] = o => JsString(o.toString)

  implicit val writesModule: Writes[Module] = o => Json.obj(
    "code" -> o.code.toUpperCase,
    "name" -> o.name,
  )

  implicit val writesAssessmentComponent: Writes[AssessmentComponent] = o => Json.obj(
    "cats" -> o.cats,
    "assessmentGroup" -> o.assessmentGroup,
    "moduleCode" -> o.moduleCode,
    "sequence" -> o.sequence,
    "name" -> o.name,
    "inUse" -> o.inUse,
    "assessmentType" -> o.assessmentType.code,
    "marksCode" -> o.marksCode,
    "weighting" -> Int.unbox(o.weighting),
  )

  implicit val writesUpstreamAssessmentGroup: Writes[UpstreamAssessmentGroup] = o => Json.obj(
    "moduleCode" -> o.moduleCode,
    "assessmentGroup" -> o.assessmentGroup,
    "occurrence" -> o.occurrence,
    "sequence" -> o.sequence,
    "academicYear" -> o.academicYear,
  )

  implicit val writesAssignment: Writes[Assignment] = o => Json.obj(
    "id" -> o.id,
    "module" -> o.module,
    "academicYear" -> o.academicYear,
    "name" -> o.name,
    "openDate" -> o.openDate,
    "openEnded" -> Boolean.unbox(o.openEnded),
    "closeDate" -> o.closeDate,
  )

  val writesUpstreamAffectedAssessment: Writes[UpstreamAffectedAssessment] = Json.writes[UpstreamAffectedAssessment]
}

object MitCircsAffectedAssessmentsCommand {
  type Result = Seq[UpstreamAffectedAssessment]
  type Command = Appliable[Result] with MitCircsAffectedAssessmentsState with SelfValidating

  def apply(student: StudentMember): Command =
    new MitCircsAffectedAssessmentsCommandInternal(student)
      with ComposableCommand[Result]
      with MitCircsAffectedAssessmentsRequest
      with MitCircsAffectedAssessmentsValidation
      with MitCircsAffectedAssessmentsPermissions
      with AutowiringAssessmentMembershipServiceComponent
      with ReadOnly with Unaudited
}

trait MitCircsAffectedAssessmentsRequest {
  var startDate: LocalDate = _
  var endDate: LocalDate = _ // May be null
}

trait MitCircsAffectedAssessmentsState {
  def student: StudentMember
}

abstract class MitCircsAffectedAssessmentsCommandInternal(val student: StudentMember) extends CommandInternal[Result] with MitCircsAffectedAssessmentsState {
  self: MitCircsAffectedAssessmentsRequest with AssessmentMembershipServiceComponent =>

  override def applyInternal(): Result = transactional(readOnly = true) {
    val startYear = AcademicYear.forDate(startDate)
    val endYear = AcademicYear.forDate(Option(endDate).getOrElse(LocalDate.now()))

    val years = startYear.to(endYear)

    // Get all the UpstreamAssessmentGroups that the student is a member of between the two dates
    val upstreamAssessmentGroups =
      years.flatMap { year => assessmentMembershipService.getUpstreamAssessmentGroups(student, year, resitOnly = false) }

    // For each upstreamAssessmentGroup, get the related AssessmentComponent
    val assessmentComponents =
      upstreamAssessmentGroups
        .flatMap { uag =>
          assessmentMembershipService.getAssessmentComponent(uag).map { component =>
            UpstreamAffectedAssessment(component.module, uag.academicYear, component.name, None, component, Seq(uag), Nil)
          }
        }
        .groupBy { a => (a.assessmentComponent, a.academicYear) }
        .map { case (_, c) =>
          c.reduce[UpstreamAffectedAssessment] { case (a1, a2) =>
            a1.copy(upstreamAssessmentGroups = a1.upstreamAssessmentGroups ++ a2.upstreamAssessmentGroups)
          }
        }
        .toSeq
        .sortBy { a => (a.academicYear, a.assessmentComponent.moduleCode, a.assessmentComponent.sequence) }

    val tabulaAssignments =
      years.flatMap { year => assessmentMembershipService.getEnrolledAssignments(student.asSsoUser, Some(year)) }
        .filter(_.summative)
        .filter { assignment =>
          assignment.submissionDeadline(student.asSsoUser) match {
            case null => !assignment.openDate.isBefore(startDate.toDateTimeAtStartOfDay)
            case deadline =>
              !deadline.isBefore(startDate.toDateTimeAtStartOfDay) &&
              deadline.isBefore(Option(endDate).getOrElse(LocalDate.now()).plusDays(1).toDateTimeAtStartOfDay)
          }
        }
        .sortBy { a => (a.submissionDeadline(student.asSsoUser), a.openDate, a.name) }

    def matches(assignment: Assignment, component: UpstreamAffectedAssessment): Boolean = {
      val assessmentComponent = component.assessmentComponent
      val upstreamAssessmentGroups = component.upstreamAssessmentGroups

      upstreamAssessmentGroups.exists { upstreamAssessmentGroup =>
        assignment.academicYear == upstreamAssessmentGroup.academicYear &&
        assignment.assessmentGroups.asScala.exists { assessmentGroup =>
          assessmentGroup.assessmentComponent == assessmentComponent &&
          assessmentGroup.occurrence == upstreamAssessmentGroup.occurrence
        }
      }
    }

    assessmentComponents.map { component =>
      val assignments = tabulaAssignments.filter(matches(_, component))

      if (assignments.size == 1) {
        val assignment = assignments.head

        component.copy(
          tabulaAssignments = assignments,
          name = assignment.name,
          deadline = Option(assignment.submissionDeadline(student.asSsoUser)).map(_.toLocalDate),
        )
      } else {
        component.copy(tabulaAssignments = assignments)
      }
    }
  }
}

trait MitCircsAffectedAssessmentsPermissions extends RequiresPermissionsChecking with PermissionsCheckingMethods {
  self: MitCircsAffectedAssessmentsState =>

  override def permissionsCheck(p: PermissionsChecking): Unit =
    p.PermissionCheck(Permissions.Profiles.Read.Coursework, mandatory(student))
}

trait MitCircsAffectedAssessmentsValidation extends SelfValidating {
  self: MitCircsAffectedAssessmentsRequest =>

  override def validate(errors: Errors): Unit = {
    if(startDate == null) errors.rejectValue("startDate", "mitigatingCircumstances.startDate.required")
    else if(endDate != null && endDate.isBefore(startDate)) errors.rejectValue("endDate", "mitigatingCircumstances.endDate.after")
  }
}
