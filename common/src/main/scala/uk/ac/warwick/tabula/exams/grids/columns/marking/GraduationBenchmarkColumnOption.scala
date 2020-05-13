package uk.ac.warwick.tabula.exams.grids.columns.marking

import org.springframework.stereotype.Component
import uk.ac.warwick.tabula.commands.exams.grids.{ExamGridEntity, ExamGridEntityYear}
import uk.ac.warwick.tabula.exams.grids.columns._
import uk.ac.warwick.tabula.services.{AutowiringCourseAndRouteServiceComponent, AutowiringModuleRegistrationServiceComponent}

@Component
class GraduationBenchmarkColumnOption extends ChosenYearExamGridColumnOption with AutowiringModuleRegistrationServiceComponent with AutowiringCourseAndRouteServiceComponent {

  override val identifier: ExamGridColumnOption.Identifier = "graduationBenchmark"

  override val label: String = "Marking: Current year graduation benchmark"

  override val sortOrder: Int = ExamGridColumnOption.SortOrders.GraduationBenchmark

  case class Column(state: ExamGridColumnState) extends ChosenYearExamGridColumn(state) with HasExamGridColumnCategory {

    override val title: String = "Graduation benchmark"

    override val category: String = s"Year ${state.yearOfStudy} Marks"

    override val excelColumnWidth: Int = ExamGridColumnOption.ExcelColumnSizes.Decimal

    override lazy val result: Map[ExamGridEntity, ExamGridColumnValue] = {
      state.entities.map(entity =>
        entity -> entity.validYears.get(state.yearOfStudy).map(entityYear => {
          ExamGridColumnValueDecimal(moduleRegistrationService.graduationBenchmark(entityYear.moduleRegistrations))
        }).getOrElse(ExamGridColumnValueMissing(s"Could not find course details for ${entity.universityId} for ${state.academicYear}"))
      ).toMap
    }
  }

  override def getColumns(state: ExamGridColumnState): Seq[ChosenYearExamGridColumn] = Seq(Column(state))
}
