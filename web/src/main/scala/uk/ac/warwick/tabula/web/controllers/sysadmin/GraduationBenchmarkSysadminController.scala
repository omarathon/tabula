package uk.ac.warwick.tabula.web.controllers.sysadmin

import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation._
import uk.ac.warwick.tabula.commands._
import uk.ac.warwick.tabula.data.model._
import uk.ac.warwick.tabula.exams.grids.columns.ExamGridYearMarksToUse
import uk.ac.warwick.tabula.helpers.RequestLevelCache
import uk.ac.warwick.tabula.helpers.StringUtils._
import uk.ac.warwick.tabula.permissions.{Permissions, PermissionsTarget}
import uk.ac.warwick.tabula.services.exams.grids.{AutowiringNormalCATSLoadServiceComponent, AutowiringUpstreamRouteRuleServiceComponent, NormalCATSLoadServiceComponent, UpstreamRouteRuleServiceComponent}
import uk.ac.warwick.tabula.services.{AutowiringProfileServiceComponent, AutowiringProgressionServiceComponent, ProfileServiceComponent, ProgressionServiceComponent}
import uk.ac.warwick.tabula.system.permissions.{PermissionsChecking, PermissionsCheckingMethods, RequiresPermissionsChecking}
import uk.ac.warwick.tabula.web.Mav
import uk.ac.warwick.tabula.{AcademicYear, SprCode}

import scala.util.Try

@Controller
@RequestMapping(value = Array("/sysadmin/graduation-benchmark"))
class GraduationBenchmarkSysadminController extends BaseSysadminController {

  @ModelAttribute("command")
  def command(): GraduationBenchmarkSysadminCommand.Command = GraduationBenchmarkSysadminCommand()

  @GetMapping
  def form(): Mav = Mav("sysadmin/graduation-benchmark/form")

  @PostMapping
  def results(@ModelAttribute("command") command: GraduationBenchmarkSysadminCommand.Command): Mav =
    Mav("sysadmin/graduation-benchmark/results",
      "results" -> command.apply()
    )

}

object GraduationBenchmarkSysadminCommand {
  type Result = Seq[(StudentCourseYearDetails, Either[String, BigDecimal])]
  type Command = Appliable[Result] with GraduationBenchmarkSysadminCommandRequest

  def apply(): Command =
    new GraduationBenchmarkSysadminCommandInternal
      with ComposableCommand[Result]
      with ReadOnly with Unaudited
      with GraduationBenchmarkSysadminCommandRequest
      with GraduationBenchmarkSysadminCommandPermissions
      with AutowiringProgressionServiceComponent
      with AutowiringProfileServiceComponent
      with AutowiringNormalCATSLoadServiceComponent
      with AutowiringUpstreamRouteRuleServiceComponent
}

abstract class GraduationBenchmarkSysadminCommandInternal extends CommandInternal[GraduationBenchmarkSysadminCommand.Result] {
  self: GraduationBenchmarkSysadminCommandRequest
    with ProgressionServiceComponent
    with ProfileServiceComponent
    with NormalCATSLoadServiceComponent
    with UpstreamRouteRuleServiceComponent =>

  private def normalLoad(route: Route, yearOfStudy: Int): BigDecimal = RequestLevelCache.cachedBy("NormalCATSLoadService.find", s"${route.code}-$yearOfStudy") {
    normalCATSLoadService.find(route, AcademicYear.starting(2019), yearOfStudy).map(_.normalLoad)
      .getOrElse(Option(route).map(_.degreeType).getOrElse(DegreeType.Undergraduate).normalCATSLoad)
  }

  private def routeRules(route: Route, level: Option[Level]): Seq[UpstreamRouteRule] = RequestLevelCache.cachedBy("UpstreamRouteRulesService.find", s"${route.code}-${level.map(_.code).getOrElse("-")}") {
    level.map { l =>
      upstreamRouteRuleService.list(route, AcademicYear.starting(2019), l)
    }.getOrElse(Seq.empty)
  }

  override protected def applyInternal(): GraduationBenchmarkSysadminCommand.Result = {
    val universityIds = ids.split('\n').map(_.safeTrim).filter(_.hasText).map(SprCode.getUniversityId)
    profileService.getAllMembersWithUniversityIds(universityIds.toSeq)
      .collect { case s: StudentMember => s }
      .flatMap(_.freshStudentCourseYearDetailsForYear(AcademicYear.starting(2019)))
      .map { scyd =>
        val entity = scyd.studentCourseDetails.student.toExamGridEntity(scyd, basedOnLevel = true, mitCircs = Seq.empty)
        val entityYear = entity.validYears.find(_._2.studentCourseYearDetails.contains(scyd)).get._2
        val routeRulesPerYear: Map[Int, Seq[UpstreamRouteRule]] = entity.validYears.view.mapValues(ey => routeRules(ey.route, ey.level)).toMap

        scyd -> Try(progressionService.graduationBenchmark(
          entityYear.studentCourseYearDetails,
          entityYear.yearOfStudy,
          normalLoad(entityYear.route, entityYear.yearOfStudy),
          routeRulesPerYear,
          ExamGridYearMarksToUse.UploadedYearMarksIfAvailable,
          groupByLevel = true,
          entity.yearWeightings,
        )).recover { case t: Throwable => Left(t.getMessage) }.get
      }
  }

}

trait GraduationBenchmarkSysadminCommandRequest {
  var ids: String = _
}

trait GraduationBenchmarkSysadminCommandPermissions extends RequiresPermissionsChecking with PermissionsCheckingMethods {
  override def permissionsCheck(p: PermissionsChecking): Unit =
    p.PermissionCheck(Permissions.Profiles.Read.ModuleRegistration.Results, PermissionsTarget.Global)
}
