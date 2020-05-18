package uk.ac.warwick.tabula.exams.grids.columns.marking

import org.springframework.stereotype.Component
import uk.ac.warwick.tabula.commands.exams.grids.ExamGridEntity
import uk.ac.warwick.tabula.data.model.CourseType._
import uk.ac.warwick.tabula.exams.grids.columns._
import uk.ac.warwick.tabula.services.{AutowiringCourseAndRouteServiceComponent, AutowiringModuleRegistrationServiceComponent}

@Component
class BenchmarkWeightedAssessmentMarkColumnOption extends ChosenYearExamGridColumnOption with AutowiringModuleRegistrationServiceComponent with AutowiringCourseAndRouteServiceComponent {

  override val identifier: ExamGridColumnOption.Identifier = "benchmarkWeightedAssessmentMark"

  override val label: String = "Marking: Benchmark weighted assessment mark"

  override val sortOrder: Int = ExamGridColumnOption.SortOrders.BenchmarkWeightedAssessmentMark

  case class Column(state: ExamGridColumnState) extends ChosenYearExamGridColumn(state) with HasExamGridColumnCategory {

    override val title: String = "Benchmark weighted assessment mark"

    override val category: String = s"Year ${state.yearOfStudy} Marks"

    override val excelColumnWidth: Int = ExamGridColumnOption.ExcelColumnSizes.Decimal

    override lazy val result: Map[ExamGridEntity, ExamGridColumnValue] = {
      state.entities.map(entity =>
        entity -> entity.validYears.get(state.yearOfStudy).map(entityYear => {
          entityYear.studentCourseYearDetails.flatMap(_.studentCourseDetails.courseType) match {
            case Some(UG) => ExamGridColumnValueDecimal(moduleRegistrationService.benchmarkWeightedAssessmentMark(entityYear.moduleRegistrations))
            case Some(ct) => ExamGridColumnValueMissing(s"Benchmark weighted assessment marks are only calculated for undergraduates")
            case None => ExamGridColumnValueMissing(s"Could not find a course type for ${entity.universityId} for ${state.academicYear}")
          }
        }).getOrElse(ExamGridColumnValueMissing(s"Could not find course details for ${entity.universityId} for ${state.academicYear}"))
      ).toMap
    }
  }

  override def getColumns(state: ExamGridColumnState): Seq[ChosenYearExamGridColumn] = Seq(Column(state))
}
