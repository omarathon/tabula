package uk.ac.warwick.tabula.helpers

import org.apache.poi.xssf.usermodel.{XSSFWorkbook, XSSFSheet, XSSFCellStyle, XSSFRow}
import org.apache.poi.ss.usermodel.{Font, Cell}
import java.util.Date

trait SpreadsheetHelper {

	def dateCellStyle(workbook: XSSFWorkbook) : XSSFCellStyle = {
		val cellStyle = workbook.createCellStyle
		cellStyle.setDataFormat(workbook.createDataFormat().getFormat("dd/MM/yy"))
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

	def addDateCell(value: Date, row: XSSFRow, style: XSSFCellStyle) {
		val cell = addCell(row, Cell.CELL_TYPE_NUMERIC)
		cell.setCellStyle(style)
		cell.setCellValue(value)
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
