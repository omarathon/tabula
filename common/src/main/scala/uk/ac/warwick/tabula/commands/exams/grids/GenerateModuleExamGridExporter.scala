package uk.ac.warwick.tabula.commands.exams.grids


import org.apache.poi.ss.usermodel._
import org.apache.poi.ss.util.CellRangeAddress
import org.apache.poi.xssf.streaming.{SXSSFRow, SXSSFWorkbook}
import org.joda.time.DateTime
import uk.ac.warwick.tabula.AcademicYear
import uk.ac.warwick.tabula.commands.TaskBenchmarking
import uk.ac.warwick.tabula.commands.exams.grids.ExamGridExportStyles._
import uk.ac.warwick.tabula.data.model.MarkState.UnconfirmedActual
import uk.ac.warwick.tabula.data.model._
import uk.ac.warwick.tabula.services.ProgressionService

object GenerateModuleExamGridExporter extends TaskBenchmarking {

  private def createCell(row: SXSSFRow, colIndex: Int, colValue: String, cellStyle: Option[CellStyle]): Unit = {
    val cell = row.createCell(colIndex)
    cell.setCellValue(colValue)
    cellStyle match {
      case Some(style) => cell.setCellStyle(style)
      case _ =>
    }
  }

  private def getCellStyle(isActual: Boolean, styleMap: CellStyleMap, mark: BigDecimal, degreeType: DegreeType, isUnconfirmed: Boolean): Option[CellStyle] = {
    isActual match {
      case true => if (mark < ProgressionService.modulePassMark(degreeType)) Option(styleMap.getStyle(FailAndActualMark, isUnconfirmed)) else Option(styleMap.getStyle(ActualMark, isUnconfirmed))
      case false if mark < ProgressionService.modulePassMark(degreeType) => Option(styleMap.getStyle(Fail, isUnconfirmed))
      case _ => None
    }
  }


  def apply(
    department: Department,
    academicYear: AcademicYear,
    module: Module,
    examModuleGridResult: ModuleExamGridResult
  ): Workbook = {
    val workbook = new SXSSFWorkbook(null, -1)

    // Styles
    val cellStyleMap = new CellStyleMap(workbook)
    val entities = examModuleGridResult.gridStudentDetailRecords
    val aGroupAndSequenceAndOccurrences = examModuleGridResult.upstreamAssessmentGroupAndSequenceAndOccurrencesWithComponentName.map { case (aGroupAndSeqAndOcc, _) => aGroupAndSeqAndOcc }
    val sheet = workbook.createSheet(academicYear.toString.replace("/", "-"))
    sheet.trackColumnForAutoSizing(0)
    sheet.trackColumnForAutoSizing(1)

    ModuleExamGridSummaryAndKey.summaryAndKey(sheet, cellStyleMap, department, academicYear, module, entities.map(_.universityId).distinct.size)

    val headerRow = sheet.createRow(sheet.getLastRowNum + 1)
    val entityRows: Map[ModuleGridDetailRecord, SXSSFRow] = entities.map(entity => entity -> sheet.createRow(sheet.getLastRowNum + 1)).toMap
    val headerStyle = Option(cellStyleMap.getStyle(ExamGridExportStyles.Header))

    var currentColumnIndex = 2 // Move to the right of the key

    benchmarkTask("leftColumns") {
      createCell(headerRow, currentColumnIndex, "Name", headerStyle)
      createCell(headerRow, currentColumnIndex + 1, "ID", headerStyle)
      createCell(headerRow, currentColumnIndex + 2, "SCJ Code", headerStyle)
      createCell(headerRow, currentColumnIndex + 3, "Course", headerStyle)
      createCell(headerRow, currentColumnIndex + 4, "Route", headerStyle)
      createCell(headerRow, currentColumnIndex + 5, "Start Year", headerStyle)
      createCell(headerRow, currentColumnIndex + 6, "Credit", headerStyle)

      // 2 columns for for each component seq
      var cSeqColumnIndex = 0
      aGroupAndSequenceAndOccurrences.foreach { aGroupAndSequenceAndOcc =>
        cSeqColumnIndex = cSeqColumnIndex + 1
        createCell(headerRow, currentColumnIndex + 6 + cSeqColumnIndex, aGroupAndSequenceAndOcc, headerStyle)
        cSeqColumnIndex = cSeqColumnIndex + 1
        createCell(headerRow, currentColumnIndex + 6 + cSeqColumnIndex, aGroupAndSequenceAndOcc, headerStyle)
        sheet.addMergedRegion(new CellRangeAddress(headerRow.getRowNum, headerRow.getRowNum, currentColumnIndex + 6 + cSeqColumnIndex - 1, currentColumnIndex + 6 + cSeqColumnIndex))
      }

      createCell(headerRow, currentColumnIndex + 7 + cSeqColumnIndex, "Module Marks", headerStyle)
      createCell(headerRow, currentColumnIndex + 8 + cSeqColumnIndex, "Module Grade", headerStyle)

      //detail rows
      entities.foreach { entity =>
        val (mark, markStyle) = if (entity.moduleRegistration.passFail) {
          ("", None)
        } else if (entity.moduleRegistration.agreedMark.isDefined) {
          (entity.moduleRegistration.agreedMark.get.toString, getCellStyle(isActual = false, cellStyleMap, entity.moduleRegistration.agreedMark.get, entity.moduleRegistration.module.degreeType, entity.isUnconfirmed))
        } else if (entity.moduleRegistration.actualMark.isDefined) {
          (entity.moduleRegistration.actualMark.get.toString, getCellStyle(isActual = true, cellStyleMap, entity.moduleRegistration.actualMark.get, entity.moduleRegistration.module.degreeType, entity.isUnconfirmed))
        } else if (entity.moduleRegistration.agreedGrade.orElse(entity.moduleRegistration.actualGrade).contains(GradeBoundary.ForceMajeureMissingComponentGrade)) {
          ("-", Option(cellStyleMap.getStyle(ActualMark)).filter(_ => entity.moduleRegistration.agreedGrade.isEmpty))
        } else {
          ("X", None)
        }

        val (grade, gradeStyle) = if (entity.moduleRegistration.agreedGrade.isDefined) {
          (entity.moduleRegistration.agreedGrade.get, None)
        } else if (entity.moduleRegistration.actualGrade.isDefined) {
          (entity.moduleRegistration.actualGrade.get, Option(cellStyleMap.getStyle(ActualMark)))
        } else {
          ("X", None)
        }
        val row = entityRows(entity)
        val mr = entity.moduleRegistration
        val scd = mr.studentCourseDetails
        createCell(row, currentColumnIndex, entity.name, None)
        createCell(row, currentColumnIndex + 1, entity.universityId, None)
        createCell(row, currentColumnIndex + 2, scd.scjCode, None)
        createCell(row, currentColumnIndex + 3, scd.course.code, None)
        createCell(row, currentColumnIndex + 4, Option(scd.currentRoute).map(_.code.toUpperCase).getOrElse(""), None)
        createCell(row, currentColumnIndex + 5, mr.academicYear.startYear.toString, None)
        createCell(row, currentColumnIndex + 6, Option(mr.cats).map(_.toPlainString).getOrElse(""), None)

        cSeqColumnIndex = 0
        aGroupAndSequenceAndOccurrences.foreach { aGroupAndSequenceAndOcc =>
          cSeqColumnIndex = cSeqColumnIndex + 1
          val isUnconfirmed = entity.componentInfo.get(aGroupAndSequenceAndOcc).flatMap(_.markState).contains(UnconfirmedActual)
          val (cMark, cMarkStyle) = entity.componentInfo.get(aGroupAndSequenceAndOcc) match {
            case Some(cInfo) => if (cInfo.resitInfo.resitMark.isDefined) {
              if (cInfo.mark.isDefined) {
                // will use resit style because of the limitation of multiple style application to single excel cell for SXSSFWorkbook
                (s"[${cInfo.resitInfo.resitMark.get}(${cInfo.mark.get})]", getCellStyle(cInfo.resitInfo.isActualResitMark, cellStyleMap, cInfo.resitInfo.resitMark.map(BigDecimal(_)).orNull, mr.module.degreeType, isUnconfirmed))
              } else {
                (s"[${cInfo.resitInfo.resitMark.get}]", getCellStyle(cInfo.resitInfo.isActualResitMark, cellStyleMap, cInfo.resitInfo.resitMark.map(BigDecimal(_)).orNull, mr.module.degreeType, isUnconfirmed))
              }
            } else if (cInfo.mark.isDefined) {
              (cInfo.mark.get.toString, getCellStyle(cInfo.isActualMark, cellStyleMap, cInfo.mark.map(BigDecimal(_)).orNull, mr.module.degreeType, isUnconfirmed))
            } else if (cInfo.resitInfo.resitGrade.orElse(cInfo.grade).contains(GradeBoundary.ForceMajeureMissingComponentGrade)) {
              ("-", if ((cInfo.resitInfo.resitGrade.nonEmpty && cInfo.resitInfo.isActualResitGrade) || (cInfo.resitInfo.resitGrade.isEmpty && cInfo.grade.nonEmpty && cInfo.isActualGrade)) Option(cellStyleMap.getStyle(ActualMark, isUnconfirmed)) else None)
            } else {
              ("X", None)
            }
            case _ => ("", None)
          }
          createCell(row, currentColumnIndex + 6 + cSeqColumnIndex, cMark, cMarkStyle)
          val (cGrade, cGradeStyle) = entity.componentInfo.get(aGroupAndSequenceAndOcc) match {
            case Some(cInfo) => if (cInfo.resitInfo.resitGrade.isDefined) {
              if (cInfo.grade.isDefined) {
                (s"[${cInfo.resitInfo.resitGrade.get}(${cInfo.grade.get})]", if (cInfo.resitInfo.isActualResitGrade) Option(cellStyleMap.getStyle(ActualMark, isUnconfirmed)) else None)
              } else {
                (s"[${cInfo.resitInfo.resitGrade.get}]", if (cInfo.resitInfo.isActualResitGrade) Option(cellStyleMap.getStyle(ActualMark, isUnconfirmed)) else None)
              }
            } else if (cInfo.grade.isDefined) {
              (cInfo.grade.get, if (cInfo.isActualGrade) Option(cellStyleMap.getStyle(ActualMark, isUnconfirmed)) else None)
            } else {
              ("X", None)
            }
            case _ => ("", None)
          }

          cSeqColumnIndex = cSeqColumnIndex + 1
          createCell(row, currentColumnIndex + 6 + cSeqColumnIndex, cGrade, cGradeStyle)
        }

        createCell(row, currentColumnIndex + 7 + cSeqColumnIndex, mark, markStyle)
        createCell(row, currentColumnIndex + 8 + cSeqColumnIndex, grade, gradeStyle)
      }

    }
    workbook
  }

}

object ModuleExamGridSummaryAndKey {

  def summaryAndKey(
    sheet: Sheet,
    cellStyleMap: CellStyleMap,
    department: Department,
    academicYear: AcademicYear,
    module: Module,
    count: Int
  ): Unit = {
    def keyValueCells(key: String, value: String, rowIndex: Int) = {
      val row = sheet.createRow(rowIndex)
      val keyCell = row.createCell(0)
      keyCell.setCellValue(key)
      keyCell.setCellStyle(cellStyleMap.getStyle(ExamGridExportStyles.Header))
      val valueCell = row.createCell(1)
      valueCell.setCellValue(value)
      row
    }

    keyValueCells("Department:", department.name, 0)
    keyValueCells("Academic year:", academicYear.toString, 1)
    keyValueCells("Module:", s"${module.code.toUpperCase} ${module.name}", 2)
    keyValueCells("Student Count:", count.toString, 3)
    keyValueCells("Grid Generated:", DateTime.now.toString, 4)

    {
      val row = sheet.createRow(5)
      val keyCell = row.createCell(0)
      keyCell.setCellValue("#")
      keyCell.setCellStyle(cellStyleMap.getStyle(ExamGridExportStyles.Base, unconfirmed = true))
      val valueCell = row.createCell(1)
      valueCell.setCellValue("\tUnconfirmed marks (subject to change)")
    }
    {
      val row = sheet.createRow(6)
      val keyCell = row.createCell(0)
      keyCell.setCellValue("#")
      keyCell.setCellStyle(cellStyleMap.getStyle(Fail))
      val valueCell = row.createCell(1)
      valueCell.setCellValue("Failed module or component")
    }
    {
      val row = sheet.createRow(7)
      val keyCell = row.createCell(0)
      keyCell.setCellValue("#")
      keyCell.setCellStyle(cellStyleMap.getStyle(ActualMark))
      val valueCell = row.createCell(1)
      valueCell.setCellValue("Agreed mark missing, using actual")
    }
    {
      val row = sheet.createRow(8)
      val keyCell = row.createCell(0)
      keyCell.setCellValue("[# (#)]")
      val valueCell = row.createCell(1)
      valueCell.setCellValue("Resit mark (original mark)")
    }
    {
      val row = sheet.createRow(9)
      val keyCell = row.createCell(0)
      keyCell.setCellValue("[# (#)]")
      val valueCell = row.createCell(1)
      valueCell.setCellValue("Resit grade (original grade)")
    }
    {
      val row = sheet.createRow(10)
      val keyCell = row.createCell(0)
      keyCell.setCellValue("X")
      val valueCell = row.createCell(1)
      valueCell.setCellValue("Agreed mark and actual mark missing")
    }
    sheet.autoSizeColumn(0)
    sheet.autoSizeColumn(1)
  }
}
