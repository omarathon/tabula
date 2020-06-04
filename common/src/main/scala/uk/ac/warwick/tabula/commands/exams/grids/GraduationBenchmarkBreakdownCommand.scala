package uk.ac.warwick.tabula.commands.exams.grids

import uk.ac.warwick.tabula.commands._
import uk.ac.warwick.tabula.data.model._
import uk.ac.warwick.tabula.permissions.{CheckablePermission, Permissions}
import uk.ac.warwick.tabula.services._
import uk.ac.warwick.tabula.services.exams.grids.{AutowiringUpstreamRouteRuleServiceComponent, UpstreamRouteRuleServiceComponent}
import uk.ac.warwick.tabula.system.permissions.{PermissionsChecking, PermissionsCheckingMethods, RequiresPermissionsChecking}
import uk.ac.warwick.tabula.{AcademicYear, ItemNotFoundException}

object GraduationBenchmarkBreakdownCommand {

  type Result = Either[UGGraduationBenchmarkBreakdown, PGGraduationBenchmarkBreakdown]
  type Command = Appliable[Result] with GraduationBenchmarkBreakdownCommandState

  def apply(studentCourseDetails: StudentCourseDetails, academicYear: AcademicYear) =
    new GraduationBenchmarkBreakdownCommandInternal(studentCourseDetails, academicYear)
      with ComposableCommand[Either[UGGraduationBenchmarkBreakdown, PGGraduationBenchmarkBreakdown]]
      with AutowiringAssessmentMembershipServiceComponent
      with AutowiringModuleRegistrationServiceComponent
      with AutowiringProgressionServiceComponent
      with AutowiringCourseAndRouteServiceComponent
      with AutowiringUpstreamRouteRuleServiceComponent
      with GraduationBenchmarkBreakdownPermissions
      with GraduationBenchmarkBreakdownCommandState
      with StudentModuleRegistrationAndComponents
      with ReadOnly with Unaudited
}

case class UGGraduationBenchmarkBreakdown (
  modules: Map[ModuleRegistration, Seq[ComponentAndMarks]],
  graduationBenchmark: BigDecimal,
  totalCats: BigDecimal,
  percentageOfAssessmentTaken: BigDecimal,
)

case class PGGraduationBenchmarkBreakdown (
  modules: Seq[ModuleRegistration],
  graduationBenchmark: BigDecimal,
  totalCats: BigDecimal,
  totalCatsTaken: BigDecimal,
  usedModulesWithCumulativeSums: Seq[(ModuleRegistration, CumulativeCatsAndMarks)],
  unusedModules: Seq[ModuleRegistration],
  catsConsidered: BigDecimal
)

case class CumulativeCatsAndMarks (
  cumulativeCats: BigDecimal,
  cumulativeMarks: BigDecimal
)


class GraduationBenchmarkBreakdownCommandInternal(val studentCourseDetails: StudentCourseDetails, val academicYear: AcademicYear)
  extends CommandInternal[Either[UGGraduationBenchmarkBreakdown, PGGraduationBenchmarkBreakdown]] with TaskBenchmarking {
  self: GraduationBenchmarkBreakdownCommandState
    with StudentModuleRegistrationAndComponents
    with ModuleRegistrationServiceComponent
    with ProgressionServiceComponent
    with CourseAndRouteServiceComponent
    with UpstreamRouteRuleServiceComponent
    with AssessmentMembershipServiceComponent =>

  override def applyInternal(): Either[UGGraduationBenchmarkBreakdown, PGGraduationBenchmarkBreakdown] = {
    val moduleRegistrations = studentCourseYearDetails.moduleRegistrations
    val modules = moduleRegistrations.map { mr => mr -> moduleRegistrationService.benchmarkComponentsAndMarks(mr) }.toMap
    if (studentCourseDetails.student.isUG) {
      Left(UGGraduationBenchmarkBreakdown(
        modules.filter{ case (_, components) => components.nonEmpty },
        moduleRegistrationService.benchmarkWeightedAssessmentMark(studentCourseYearDetails.moduleRegistrations),
        studentCourseYearDetails.totalCats,
        moduleRegistrationService.percentageOfAssessmentTaken(studentCourseYearDetails.moduleRegistrations),
      ))
    } else {
      val bestCats = progressionService.numberCatsToConsiderPG(studentCourseYearDetails)
      val (bestPGModules, catsConsidered) = progressionService.bestPGModules(moduleRegistrations, bestCats)
      Right(PGGraduationBenchmarkBreakdown(
        moduleRegistrations.sortBy(_.firstDefinedMark).reverse,
        progressionService.postgraduateBenchmark(studentCourseYearDetails, moduleRegistrations),
        bestCats,
        moduleRegistrations.map(mr => BigDecimal(mr.cats)).sum,
        {
          val (usedModulesWithCumulativeSums, _, _) = bestPGModules.foldLeft((Map.empty[ModuleRegistration, CumulativeCatsAndMarks], BigDecimal(0), BigDecimal(0))) {
            (acc, module) =>
              val (mapAcc, totalAcc, totalMarksAcc) = acc
              val newCatsTotal = totalAcc + module.cats
              val newMarksTotal = totalMarksAcc + (BigDecimal(module.firstDefinedMark.get) * BigDecimal(module.cats))
              val newMap = mapAcc + (module -> CumulativeCatsAndMarks(newCatsTotal, newMarksTotal))
              (newMap, newCatsTotal, newMarksTotal)
          }
          usedModulesWithCumulativeSums.toSeq.sortBy(m => m._1.cats )
        },
        (moduleRegistrations diff bestPGModules).sortBy(_.cats).reverse,
        catsConsidered,
      ))
    }
  }
}

trait GraduationBenchmarkBreakdownPermissions extends RequiresPermissionsChecking with PermissionsCheckingMethods {

  self: GraduationBenchmarkBreakdownCommandState =>

  override def permissionsCheck(p: PermissionsChecking): Unit = {
    p.PermissionCheckAny(
      Seq(CheckablePermission(Permissions.Department.ExamGrids, studentCourseYearDetails.enrolmentDepartment),
        CheckablePermission(Permissions.Department.ExamGrids, studentCourseYearDetails.studentCourseDetails.currentRoute))
    )
  }

}

trait GraduationBenchmarkBreakdownCommandState {
  def academicYear: AcademicYear
  def studentCourseDetails: StudentCourseDetails

  lazy val studentCourseYearDetails: StudentCourseYearDetails = studentCourseDetails.freshStudentCourseYearDetailsForYear(academicYear)
    .orElse(studentCourseDetails.freshOrStaleStudentCourseYearDetailsForYear(academicYear))
    .getOrElse(throw new ItemNotFoundException())
}
