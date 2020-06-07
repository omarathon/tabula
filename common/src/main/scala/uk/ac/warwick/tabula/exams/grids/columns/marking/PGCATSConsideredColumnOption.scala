package uk.ac.warwick.tabula.exams.grids.columns.marking

import org.springframework.stereotype.Component
import uk.ac.warwick.tabula.commands.exams.grids.ExamGridEntity
import uk.ac.warwick.tabula.data.model.CourseType.{PGT, UG}
import uk.ac.warwick.tabula.exams.grids.columns._
import uk.ac.warwick.tabula.services.AutowiringProgressionServiceComponent

@Component
class PGCATSConsideredColumnOption extends ChosenYearExamGridColumnOption with AutowiringProgressionServiceComponent {

  override val identifier: ExamGridColumnOption.Identifier = "catsConsidered"

  override val label: String = "Marking: Number of CATS considered in graduation benchmark"

  override val sortOrder: Int = ExamGridColumnOption.SortOrders.CATSConsidered

  case class Column(state: ExamGridColumnState) extends ChosenYearExamGridColumn(state) with HasExamGridColumnCategory {

    override val title: String = "CATS Considered"

    override val category: String = "Marking"

    override val excelColumnWidth: Int = ExamGridColumnOption.ExcelColumnSizes.Decimal

    override lazy val result: Map[ExamGridEntity, ExamGridColumnValue] = {
      state.entities.map(entity =>
        entity -> entity.validYears.get(state.yearOfStudy).map(entityYear => {
          entityYear.studentCourseYearDetails.flatMap(_.studentCourseDetails.courseType) match {
            case Some(PGT) =>
              val scyd = entityYear.studentCourseYearDetails.get
              val maxCATSToConsider = progressionService.numberCatsToConsiderPG(scyd)
              val (_, catsConsidered) = progressionService.bestPGModules(scyd.moduleRegistrations, maxCATSToConsider)
              ExamGridColumnValueDecimal(catsConsidered)
            case Some(UG) =>
              ExamGridColumnValueMissing(s"CATS considered isn't defined for UGs")
            case Some(ct) => ExamGridColumnValueMissing(s"Benchmarks aren't defined for ${ct.description} courses")
            case None => ExamGridColumnValueMissing(s"Could not find a course type for ${entity.universityId} for ${state.academicYear}")
          }
        }).getOrElse(ExamGridColumnValueMissing(s"Could not find course details for ${entity.universityId} for ${state.academicYear}"))
      ).toMap
    }
  }

  override def getColumns(state: ExamGridColumnState): Seq[ChosenYearExamGridColumn] = Seq(Column(state))
}
