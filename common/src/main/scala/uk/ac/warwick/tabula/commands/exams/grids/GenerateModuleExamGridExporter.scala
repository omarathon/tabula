package uk.ac.warwick.tabula.commands.exams.grids

import java.awt.Color

import org.apache.poi.ss.usermodel._
import org.apache.poi.ss.util.CellRangeAddress
import org.apache.poi.xssf.streaming.{SXSSFRow, SXSSFWorkbook}
import org.apache.poi.xssf.usermodel.{XSSFColor, XSSFFont}
import org.joda.time.DateTime
import uk.ac.warwick.tabula.AcademicYear
import uk.ac.warwick.tabula.commands.TaskBenchmarking
import uk.ac.warwick.tabula.data.model._
import uk.ac.warwick.tabula.services.ProgressionService

object GenerateModuleExamGridExporter extends TaskBenchmarking {

	import ModuleExamGridExportStyles._

	private def createCell(row: SXSSFRow, colIndex: Int, colValue: String, cellStyle: Option[CellStyle]): Unit = {
		val cell = row.createCell(colIndex)
		cell.setCellValue(colValue)
		cellStyle match {
			case Some(style) => cell.setCellStyle(style)
			case _ =>
		}
	}

	private def getCellStyle(isActual: Boolean, styleMap: Map[Style, CellStyle], mark: BigDecimal, degreeType: DegreeType): Option[CellStyle] = {
		isActual match {
			case true => if (mark < ProgressionService.modulePassMark(degreeType)) Option(styleMap(FailAndActualMark)) else Option(styleMap(ActualMark))
			case false if mark < ProgressionService.modulePassMark(degreeType) => Option(styleMap(Fail))
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
		val cellStyleMap = getCellStyleMap(workbook)
		val entities = examModuleGridResult.gridStudentDetailRecords
		val aGroupAndSequenceAndOccurrences = examModuleGridResult.upstreamAssessmentGroupAndSequenceAndOccurrencesWithComponentName.map { case (aGroupAndSeqAndOcc, _) => aGroupAndSeqAndOcc }
		val sheet = workbook.createSheet(academicYear.toString.replace("/", "-"))
		sheet.trackAllColumnsForAutoSizing()

		ModuleExamGridSummaryAndKey.summaryAndKey(sheet, cellStyleMap, department, academicYear, module, entities.map(_.universityId).distinct.size)

		val headerRow = sheet.createRow(sheet.getLastRowNum + 1)
		val entityRows: Map[ModuleGridDetailRecord, SXSSFRow] = entities.map(entity => entity -> sheet.createRow(sheet.getLastRowNum + 1)).toMap
		val headerStyle = Option(cellStyleMap(Header))

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
				val (mark, markStyle) = if (Option(entity.moduleRegistration.agreedMark).isDefined) {
					(entity.moduleRegistration.agreedMark.toString, getCellStyle(isActual = false, cellStyleMap, entity.moduleRegistration.agreedMark, entity.moduleRegistration.module.degreeType))
				} else if (Option(entity.moduleRegistration.actualMark).isDefined) {
					(entity.moduleRegistration.actualMark.toString, getCellStyle(isActual = true, cellStyleMap, entity.moduleRegistration.actualMark, entity.moduleRegistration.module.degreeType))
				} else {
					("X", None)
				}

				val (grade, gradeStyle) = if (Option(entity.moduleRegistration.agreedGrade).isDefined) {
					(entity.moduleRegistration.agreedGrade, None)
				} else if (Option(entity.moduleRegistration.actualGrade).isDefined) {
					(entity.moduleRegistration.actualGrade, Option(cellStyleMap(ActualMark)))
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
				createCell(row, currentColumnIndex + 4, scd.currentRoute.code.toUpperCase, None)
				createCell(row, currentColumnIndex + 5, mr.academicYear.startYear.toString, None)
				createCell(row, currentColumnIndex + 6, mr.cats.toString, None)

				cSeqColumnIndex = 0
				aGroupAndSequenceAndOccurrences.foreach { aGroupAndSequenceAndOcc =>
					cSeqColumnIndex = cSeqColumnIndex + 1
					var (cMark, cMarkStyle) = entity.componentInfo.get(aGroupAndSequenceAndOcc) match {
						case Some(cInfo) => if (Option(cInfo.resitInfo.resitMark).isDefined) {
							if (Option(cInfo.mark).isDefined) {
								//will use resit style because of the limitation of multiple sytle application to single excel cell for SXSSFWorkbook
								(s"[${cInfo.resitInfo.resitMark.toString}(${cInfo.mark.toString})]", getCellStyle(cInfo.resitInfo.isActualResitMark, cellStyleMap, cInfo.resitInfo.resitMark, mr.module.degreeType))
							} else {
								(s"[${cInfo.resitInfo.resitMark.toString}]", getCellStyle(cInfo.resitInfo.isActualResitMark, cellStyleMap, cInfo.resitInfo.resitMark, mr.module.degreeType))
							}
						} else if (Option(cInfo.mark).isDefined) {
							(cInfo.mark.toString, getCellStyle(cInfo.isActualMark, cellStyleMap, cInfo.mark, mr.module.degreeType))
						} else {
							("X", None)
						}
						case _ => ("", None)
					}
					createCell(row, currentColumnIndex + 6 + cSeqColumnIndex, cMark, cMarkStyle)
					var (cGrade, cGradeStyle) = entity.componentInfo.get(aGroupAndSequenceAndOcc) match {
						case Some(cInfo) => if (Option(cInfo.resitInfo.resitGrade).isDefined) {
							if (Option(cInfo.grade).isDefined) {
								(s"[${cInfo.resitInfo.resitGrade.toString}(${cInfo.grade.toString})]", if (cInfo.resitInfo.isActualResitGrade) Option(cellStyleMap(ActualMark)) else None)
							} else {
								(s"[${cInfo.resitInfo.resitGrade.toString}]", if (cInfo.resitInfo.isActualResitGrade) Option(cellStyleMap(ActualMark)) else None)
							}
						} else  if (Option(cInfo.grade).isDefined) {
							(cInfo.grade.toString, if (cInfo.isActualGrade) Option(cellStyleMap(ActualMark)) else None)
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

object ModuleExamGridExportStyles {

	sealed trait Style

	case object Header extends Style

	case object Fail extends Style

	case object ActualMark extends Style

	case object FailAndActualMark extends Style

	case object BoldText extends Style


	def getCellStyleMap(workbook: Workbook): Map[Style, CellStyle] = {
		val headerStyle = {
			val cs = workbook.createCellStyle()
			val boldFont = workbook.createFont()
			boldFont.setFontHeightInPoints(10)
			boldFont.setBold(true)
			cs.setFont(boldFont)
			cs.setVerticalAlignment(VerticalAlignment.CENTER)
			cs
		}


		val failStyle = {
			val cs = workbook.createCellStyle()
			val redFont = workbook.createFont().asInstanceOf[XSSFFont]
			redFont.setFontHeightInPoints(10)
			redFont.setColor(new XSSFColor(new Color(175, 39, 35)))
			redFont.setUnderline(FontUnderline.DOUBLE)
			cs.setFont(redFont)
			cs
		}

		val actualMarkStyle = {
			val cs = workbook.createCellStyle()
			val blueFont = workbook.createFont().asInstanceOf[XSSFFont]
			blueFont.setFontHeightInPoints(10)
			blueFont.setColor(new XSSFColor(new Color(35, 155, 146)))
			blueFont.setItalic(true)
			cs.setFont(blueFont)
			cs
		}

		val failAndActualMarkStyle = {
			val cs = workbook.createCellStyle()
			val redFont = workbook.createFont().asInstanceOf[XSSFFont]
			redFont.setFontHeightInPoints(10)
			redFont.setColor(new XSSFColor(new Color(175, 39, 35)))
			redFont.setUnderline(FontUnderline.DOUBLE)
			redFont.setItalic(true)
			cs.setFont(redFont)
			cs
		}

		val boldText = {
			val cs = workbook.createCellStyle()
			val boldFont = workbook.createFont()
			boldFont.setFontHeightInPoints(10)
			boldFont.setBold(true)
			cs.setFont(boldFont)
			cs
		}


		Map(
			Header -> headerStyle,
			Fail -> failStyle,
			ActualMark -> actualMarkStyle,
			FailAndActualMark -> failAndActualMarkStyle,
			BoldText -> boldText
		)
	}
}

object ModuleExamGridSummaryAndKey {

	def summaryAndKey(
		sheet: Sheet,
		cellStyleMap: Map[ModuleExamGridExportStyles.Style, CellStyle],
		department: Department,
		academicYear: AcademicYear,
		module: Module,
		count: Int
	): Unit = {
		def keyValueCells(key: String, value: String, rowIndex: Int) = {
			val row = sheet.createRow(rowIndex)
			val keyCell = row.createCell(0)
			keyCell.setCellValue(key)
			keyCell.setCellStyle(cellStyleMap(ModuleExamGridExportStyles.Header))
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
			keyCell.setCellStyle(cellStyleMap(ModuleExamGridExportStyles.Fail))
			val valueCell = row.createCell(1)
			valueCell.setCellValue("Failed module or component")
		}
		{
			val row = sheet.createRow(6)
			val keyCell = row.createCell(0)
			keyCell.setCellValue("#")
			keyCell.setCellStyle(cellStyleMap(ModuleExamGridExportStyles.ActualMark))
			val valueCell = row.createCell(1)
			valueCell.setCellValue("Agreed mark missing, using actual")
		}
		{
			val row = sheet.createRow(7)
			val keyCell = row.createCell(0)
			keyCell.setCellValue("[# (#)]")
			val valueCell = row.createCell(1)
			valueCell.setCellValue("Resit mark (original mark)")
		}
		{
			val row = sheet.createRow(8)
			val keyCell = row.createCell(0)
			keyCell.setCellValue("[# (#)]")
			val valueCell = row.createCell(1)
			valueCell.setCellValue("Resit grade (original grade)")
		}
		{
			val row = sheet.createRow(9)
			val keyCell = row.createCell(0)
			keyCell.setCellValue("X")
			val valueCell = row.createCell(1)
			valueCell.setCellValue("Agreed mark and actual mark missing")
		}
		sheet.autoSizeColumn(0)
		sheet.autoSizeColumn(1)
	}
}