package uk.ac.warwick.tabula.commands.exams.grids

import org.apache.poi.xssf.usermodel.{XSSFCellStyle, XSSFWorkbook}
import uk.ac.warwick.tabula.AcademicYear
import uk.ac.warwick.tabula.data.model.StudentCourseYearDetails
import uk.ac.warwick.tabula.exams.grids.columns.ExamGridColumn

object GenerateExamGridExporter {

	sealed trait Style
	case object Header extends Style
	case object HeaderRotated extends Style
	case object Fail extends Style
	case object Overcat extends Style

	def apply(scyds: Seq[StudentCourseYearDetails], columns: Seq[Seq[ExamGridColumn]], academicYear: AcademicYear): XSSFWorkbook = {
		val workbook = new XSSFWorkbook()

		// Styles
		val cellStyleMap = getCellStyleMap(workbook)

		val sheet = workbook.createSheet(academicYear.toString.replace("/","-"))

		val indexedColumns = columns.map(_.zipWithIndex).zipWithIndex

		val headerRow = sheet.createRow(0)
		indexedColumns.foreach{case(columnSet, columnSetIndex) =>
			columnSet.foreach{case(column, columnIndex) =>
				val cell = headerRow.createCell(columnSetIndex + columnIndex)
				cell.setCellStyle(cellStyleMap(Header))
				cell.setCellValue(column.title)
			}
		}

		scyds.foreach(scyd => {
			val row = sheet.createRow(sheet.getLastRowNum + 1)
			indexedColumns.foreach{case(columnSet, columnSetIndex) =>
				columnSet.foreach{case(column, columnIndex) =>
					column.renderExcelCell(row, columnSetIndex + columnIndex, scyd, cellStyleMap)
				}
			}
		})

		workbook
	}

	private def getCellStyleMap(workbook: XSSFWorkbook): Map[GenerateExamGridExporter.Style, XSSFCellStyle] = {
		val headerStyle = {
			val cs = workbook.createCellStyle()
			val f = workbook.createFont()
			f.setBold(true)
			f.setFontHeight(10)
			cs.setFont(f)
			cs
		}

		Map(
			Header -> headerStyle
		)
	}

}
