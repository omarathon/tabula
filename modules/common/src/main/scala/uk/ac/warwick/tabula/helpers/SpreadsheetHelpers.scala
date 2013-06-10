package uk.ac.warwick.tabula.helpers

import org.apache.poi.xssf.usermodel.{XSSFWorkbook, XSSFSheet, XSSFCellStyle, XSSFRow}
import org.apache.poi.ss.usermodel.{Font, Cell}
import uk.ac.warwick.tabula.DateFormats
import org.apache.poi.ss.util.WorkbookUtil
import uk.ac.warwick.tabula.data.model.Department
import org.joda.time.DateTime
import org.joda.time.LocalDate

object SpreadsheetHelpers {
	
	val MaxDepartmentNameLength = 31 - 11

	// trim the department name down to 20 characters. Excel sheet names must be 31 chars or less so
	def trimmedDeptName(department: Department) = {
		if (department.name.length > MaxDepartmentNameLength)
			department.name.substring(0, MaxDepartmentNameLength)
		else
			department.name
	}

	// replace unsafe characters with spaces
	def safeDeptName(department: Department)  = WorkbookUtil.createSafeSheetName(trimmedDeptName(department))


	def dateCellStyle(workbook: XSSFWorkbook) : XSSFCellStyle = {
		val cellStyle = workbook.createCellStyle
		cellStyle.setDataFormat(workbook.createDataFormat().getFormat(DateFormats.CSVDatePattern))
		cellStyle
	}

	def percentageCellStyle(workbook: XSSFWorkbook) : XSSFCellStyle = {
		val cellStyle = workbook.createCellStyle
		cellStyle.setDataFormat(workbook.createDataFormat().getFormat("0.00%"))
		cellStyle
	}

	def headerStyle(workbook: XSSFWorkbook) : XSSFCellStyle = {
		val cellStyle = workbook.createCellStyle
		val font = workbook.createFont()
		font.setBoldweight(Font.BOLDWEIGHT_BOLD)
		cellStyle.setFont(font)
		cellStyle
	}

	def getNextCellNum(row: XSSFRow):Short = if(row.getLastCellNum == -1) 0 else row.getLastCellNum

	def addCell(row: XSSFRow, cellType: Int) = {
		val cell = row.createCell(getNextCellNum(row))
		cell.setCellType(cellType)
		cell
	}

	def addStringCell(value: String, row: XSSFRow) {
		val cell = addCell(row, Cell.CELL_TYPE_STRING)
		cell.setCellValue(value)
	}

	def addStringCell(value: String, row: XSSFRow, style: XSSFCellStyle) {
		val cell = addCell(row, Cell.CELL_TYPE_STRING)
		cell.setCellStyle(style)
		cell.setCellValue(value)
	}

	def addNumericCell(value: Double, row: XSSFRow) {
		val cell = addCell(row, Cell.CELL_TYPE_NUMERIC)
		cell.setCellValue(value)
	}

	def addNumericCell(value: Double, row: XSSFRow, style: XSSFCellStyle) {
		val cell = addCell(row, Cell.CELL_TYPE_NUMERIC)
		cell.setCellStyle(style)
		cell.setCellValue(value)
	}
	
	def addDateCell(value: DateTime, row: XSSFRow, style: XSSFCellStyle) {
		addDateCell(value.toLocalDate, row, style)
	}

	def addDateCell(value: LocalDate, row: XSSFRow, style: XSSFCellStyle) {
		val cell = addCell(row, Cell.CELL_TYPE_NUMERIC)
		cell.setCellStyle(style)
		cell.setCellValue(Option(value).map { _.toDate }.orNull)
	}

	def addPercentageCell(num:Double, total:Double, row: XSSFRow, workbook: XSSFWorkbook) {
		if (total == 0)
			addStringCell("N/A", row)
		else
			addNumericCell((num / total), row, percentageCellStyle(workbook))
	}

	def formatWorksheet(sheet: XSSFSheet, cols: Int) {
		(0 to cols).map(sheet.autoSizeColumn(_))
	}
}
