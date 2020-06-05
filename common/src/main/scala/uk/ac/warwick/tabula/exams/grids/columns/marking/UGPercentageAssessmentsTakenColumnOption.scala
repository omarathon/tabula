package uk.ac.warwick.tabula.exams.grids.columns.marking

import org.springframework.stereotype.Component
import uk.ac.warwick.tabula.commands.exams.grids.ExamGridEntity
import uk.ac.warwick.tabula.data.model.CourseType.{PGT, UG}
import uk.ac.warwick.tabula.exams.grids.columns._
import uk.ac.warwick.tabula.services.AutowiringModuleRegistrationServiceComponent

import scala.math.BigDecimal.RoundingMode

@Component
class UGPercentageAssessmentsTakenColumnOption extends ChosenYearExamGridColumnOption with AutowiringModuleRegistrationServiceComponent {

  override val identifier: ExamGridColumnOption.Identifier = "percentageAssessmentsTaken"

  override val label: String = "Marking: Percentage of assessments taken used in graduation benchmark breakdown"

  override val sortOrder: Int = ExamGridColumnOption.SortOrders.PercentageAssessmentsTaken

  case class Column(state: ExamGridColumnState) extends ChosenYearExamGridColumn(state) with HasExamGridColumnCategory {

    override val title: String = "Percentage Assessments Taken"

    override val category: String = "Marking"

    override val excelColumnWidth: Int = ExamGridColumnOption.ExcelColumnSizes.Decimal

    override lazy val result: Map[ExamGridEntity, ExamGridColumnValue] = {
      state.entities.map(entity =>
        entity -> entity.validYears.get(state.yearOfStudy).map(entityYear => {
          entityYear.studentCourseYearDetails.flatMap(_.studentCourseDetails.courseType) match {
            case Some(PGT) =>
              ExamGridColumnValueMissing(s"Percentage of assessments taken isn't defined for PGTs")
            case Some(UG) =>
              val scyd = entityYear.studentCourseYearDetails.get
              ExamGridColumnValueDecimal(moduleRegistrationService.percentageOfAssessmentTaken(scyd.moduleRegistrations).setScale(1, RoundingMode.HALF_UP))
            case Some(ct) => ExamGridColumnValueMissing(s"Benchmarks aren't defined for ${ct.description} courses")
            case None => ExamGridColumnValueMissing(s"Could not find a course type for ${entity.universityId} for ${state.academicYear}")
          }
        }).getOrElse(ExamGridColumnValueMissing(s"Could not find course details for ${entity.universityId} for ${state.academicYear}"))
      ).toMap
    }
  }

  override def getColumns(state: ExamGridColumnState): Seq[ChosenYearExamGridColumn] = Seq(Column(state))
}
