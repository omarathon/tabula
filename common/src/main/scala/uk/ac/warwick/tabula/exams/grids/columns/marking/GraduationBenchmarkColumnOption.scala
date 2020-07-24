package uk.ac.warwick.tabula.exams.grids.columns.marking

import org.springframework.stereotype.Component
import uk.ac.warwick.tabula.AutowiringTopLevelUrlComponent
import uk.ac.warwick.tabula.commands.exams.grids.ExamGridEntity
import uk.ac.warwick.tabula.data.model.CourseType.{PGT, UG}
import uk.ac.warwick.tabula.exams.grids.columns._
import uk.ac.warwick.tabula.exams.web.Routes
import uk.ac.warwick.tabula.services.{AutowiringModuleRegistrationServiceComponent, AutowiringProgressionServiceComponent}

@Component
class GraduationBenchmarkColumnOption extends ChosenYearExamGridColumnOption with AutowiringProgressionServiceComponent with AutowiringModuleRegistrationServiceComponent with AutowiringTopLevelUrlComponent {

  override val identifier: ExamGridColumnOption.Identifier = "graduationBenchmark"

  override val label: String = "Marking: Current year graduation benchmark"

  override val sortOrder: Int = ExamGridColumnOption.SortOrders.GraduationBenchmark

  case class Column(state: ExamGridColumnState) extends ChosenYearExamGridColumn(state) with HasExamGridColumnCategory {

    override val title: String = "Graduation benchmark"

    override val category: String = "Marking"

    override val excelColumnWidth: Int = ExamGridColumnOption.ExcelColumnSizes.Decimal

    override lazy val result: Map[ExamGridEntity, ExamGridColumnValue] = {
      state.entities.map(entity =>
        entity -> entity.validYears.get(state.yearOfStudy).map(entityYear => {
          entityYear.studentCourseYearDetails.flatMap(_.studentCourseDetails.courseType) match {
            case Some(PGT) =>
              val scyd = entityYear.studentCourseYearDetails.get
              ExamGridColumnGraduationBenchmarkDecimal(
                progressionService.postgraduateBenchmark(scyd, entityYear.moduleRegistrations),
                isActual = false,
                toplevelUrl+Routes.Grids.benchmarkdetails(scyd, state.yearMarksToUse, state.isLevelGrid)
              )
            case Some(UG) => progressionService.graduationBenchmark(
              entityYear.studentCourseYearDetails,
              entityYear.yearOfStudy,
              state.normalLoadLookup(entityYear),
              entity.validYears.view.mapValues(ey => state.routeRulesLookup(ey)).toMap,
              state.yearMarksToUse,
              state.isLevelGrid,
              entity.yearWeightings
            ).fold(ExamGridColumnValueMissing.apply, gb => ExamGridColumnGraduationBenchmarkDecimal(
              gb,
              isActual = false,
              toplevelUrl+Routes.Grids.benchmarkdetails(entityYear.studentCourseYearDetails.get, state.yearMarksToUse, state.isLevelGrid)
            ))
            case Some(ct) => ExamGridColumnValueMissing(s"Benchmarks aren't defined for ${ct.description} courses")
            case None => ExamGridColumnValueMissing(s"Could not find a course type for ${entity.universityId} for ${state.academicYear}")
          }
        }).getOrElse(ExamGridColumnValueMissing(s"Could not find course details for ${entity.universityId} for ${state.academicYear}"))
      ).toMap
    }
  }

  override def getColumns(state: ExamGridColumnState): Seq[ChosenYearExamGridColumn] = Seq(Column(state))
}
