package uk.ac.warwick.tabula.commands.exams.grids

import java.awt.Color

import org.apache.poi.ss.usermodel.{FontUnderline, HorizontalAlignment, VerticalAlignment}
import org.apache.poi.ss.util.CellRangeAddress
import org.apache.poi.xssf.usermodel.{XSSFSheet, XSSFCellStyle, XSSFColor, XSSFWorkbook}
import org.joda.time.DateTime
import uk.ac.warwick.tabula.AcademicYear
import uk.ac.warwick.tabula.data.model.{CourseYearWeighting, Route, Course, Department}
import uk.ac.warwick.tabula.exams.grids.columns.{ExamGridColumn, HasExamGridColumnCategory, HasExamGridColumnSecondaryValue, HasExamGridColumnSection}

object GenerateExamGridExporter {

	sealed trait Style
	case object Header extends Style
	case object HeaderRotated extends Style
	case object Rotated extends Style
	case object Fail extends Style
	case object Overcat extends Style
	case object Overridden extends Style
	case object ActualMark extends Style

	def apply(
		department: Department,
		academicYear: AcademicYear,
		course: Course,
		route: Route,
		yearOfStudy: Int,
		yearWeightings: Seq[CourseYearWeighting],
		normalLoad: BigDecimal,
		scyds: Seq[GenerateExamGridEntity],
		columns: Seq[ExamGridColumn]
	): XSSFWorkbook = {
		val workbook = new XSSFWorkbook()

		// Styles
		val cellStyleMap = getCellStyleMap(workbook)

		val sheet = workbook.createSheet(academicYear.toString.replace("/","-"))

		val indexedColumns = columns.zipWithIndex

		val categories = columns.collect{case c: HasExamGridColumnCategory => c}.groupBy(_.category)
		var currentSection = ""
		var columnOffset = 0 // How many section columns have been added (so how many to shift the columnIndex)

		summaryAndKey(sheet, cellStyleMap, department, academicYear, course, route, yearOfStudy, yearWeightings, normalLoad, scyds.size)

		if (categories.nonEmpty) {

			// Category row
			val categoryRow = sheet.createRow(sheet.getLastRowNum + 1)
			var currentSection = ""
			var currentCategory = ""
			var columnOffset = 0
			var maxCellWidth = 0
			indexedColumns.foreach { case (column, columnIndex) =>
				column match {
					case hasSection: HasExamGridColumnSection if hasSection.sectionIdentifier != currentSection =>
						currentSection = hasSection.sectionIdentifier
						columnOffset = columnOffset + 1
					case _ =>
				}
				column match {
					case hasCategory: HasExamGridColumnCategory =>
						if (currentCategory != hasCategory.category) {
							currentCategory = hasCategory.category
							val cell = categoryRow.createCell(columnIndex + columnOffset)
							cell.setCellValue(hasCategory.category)
							sheet.autoSizeColumn(columnIndex + columnOffset)
							maxCellWidth = Math.max(maxCellWidth, sheet.getColumnWidth(columnIndex + columnOffset))
							cell.setCellStyle(cellStyleMap(HeaderRotated))
							sheet.addMergedRegion(new CellRangeAddress(categoryRow.getRowNum, categoryRow.getRowNum, columnIndex + columnOffset, columnIndex + columnOffset + categories(hasCategory.category).size - 1))
						}
					case _ =>
						categoryRow.createCell(columnIndex + columnOffset) // Blank cell
				}
			}

			categoryRow.setHeight((maxCellWidth * 0.5).toShort)

			// Titles in categories
			val titlesInCategoriesRow = sheet.createRow(sheet.getLastRowNum + 1)
			currentSection = ""
			columnOffset = 0
			maxCellWidth = 0
			indexedColumns.foreach { case (column, columnIndex) =>
				column match {
					case hasSection: HasExamGridColumnSection if hasSection.sectionIdentifier != currentSection =>
						currentSection = hasSection.sectionIdentifier
						val cell = titlesInCategoriesRow.createCell(columnIndex + columnOffset)
						cell.setCellStyle(cellStyleMap(Header))
						cell.setCellValue(hasSection.sectionTitleLabel)
						columnOffset = columnOffset + 1
					case _ =>
				}
				column match {
					case hasCategory: HasExamGridColumnCategory =>
						val cell = titlesInCategoriesRow.createCell(columnIndex + columnOffset)
						cell.setCellValue(hasCategory.title)
						sheet.autoSizeColumn(columnIndex + columnOffset)
						maxCellWidth = Math.max(maxCellWidth, sheet.getColumnWidth(columnIndex + columnOffset))
						cell.setCellStyle(cellStyleMap(Rotated))
					case _ =>
						titlesInCategoriesRow.createCell(columnIndex + columnOffset) // Blank cell
				}
			}

			titlesInCategoriesRow.setHeight((maxCellWidth * 0.38).toShort)

		}

		// Uncategorized column headers and secondary values
		val headerRow = sheet.createRow(sheet.getLastRowNum + 1)
		currentSection = ""
		columnOffset = 0
		indexedColumns.foreach { case (column, columnIndex) =>
			column match {
				case hasSection: HasExamGridColumnSection if hasSection.sectionIdentifier != currentSection && hasSection.sectionSecondaryValueLabel.nonEmpty =>
					currentSection = hasSection.sectionIdentifier
					val cell = headerRow.createCell(columnIndex + columnOffset)
					cell.setCellStyle(cellStyleMap(Header))
					cell.setCellValue(hasSection.sectionSecondaryValueLabel)
					columnOffset = columnOffset + 1
				case _ =>
			}
			column match {
				case hasSecondary: HasExamGridColumnSecondaryValue =>
					val cell = headerRow.createCell(columnIndex + columnOffset)
					cell.setCellValue(hasSecondary.renderSecondaryValue)
				case hasCategory: HasExamGridColumnCategory =>
					headerRow.createCell(columnIndex + columnOffset) // Blank cell
				case _ =>
					val cell = headerRow.createCell(columnIndex + columnOffset)
					cell.setCellStyle(cellStyleMap(Header))
					cell.setCellValue(column.title)
			}
		}

		// Values per student and section labels
		scyds.zipWithIndex.foreach { case (scyd, scydIndex) =>
			val row = sheet.createRow(sheet.getLastRowNum + 1)
			currentSection = ""
			columnOffset = 0
			indexedColumns.foreach{case(column, columnIndex) =>
				column match {
					case hasSection: HasExamGridColumnSection if hasSection.sectionIdentifier != currentSection && hasSection.sectionValueLabel.nonEmpty =>
						currentSection = hasSection.sectionIdentifier
						if (scydIndex == 0) {
							val cell = row.createCell(columnIndex + columnOffset)
							cell.setCellStyle(cellStyleMap(Header))
							cell.setCellValue(hasSection.sectionValueLabel)
							sheet.addMergedRegion(new CellRangeAddress(row.getRowNum, row.getRowNum + scyds.size - 1, columnIndex + columnOffset, columnIndex + columnOffset))
						}
						columnOffset = columnOffset + 1
					case _ =>
				}
				column.renderExcelCell(row, columnIndex + columnOffset, scyd, cellStyleMap)
			}
		}

		(0 to columns.size + columnOffset).foreach(sheet.autoSizeColumn(_, true))

		workbook
	}

	private def summaryAndKey(
		sheet: XSSFSheet,
		cellStyleMap: Map[GenerateExamGridExporter.Style, XSSFCellStyle],
		department: Department,
		academicYear: AcademicYear,
		course: Course,
		route: Route,
		yearOfStudy: Int,
		yearWeightings: Seq[CourseYearWeighting],
		normalLoad: BigDecimal,
		studentCount: Int
	): Unit = {
		def keyValueCells(key: String, value: String, rowIndex: Int) = {
			val row = sheet.createRow(rowIndex)
			val keyCell = row.createCell(0)
			keyCell.setCellValue(key)
			keyCell.setCellStyle(cellStyleMap(Header))
			val valueCell = row.createCell(1)
			valueCell.setCellValue(value)
			row
		}
		keyValueCells("Department:", department.name, 0)
		keyValueCells("Academic year:", academicYear.toString, 1)
		keyValueCells("Course:", s"${course.code.toUpperCase} ${course.name}", 2)
		keyValueCells("Route:", s"${route.code.toUpperCase} ${route.name}", 3)
		keyValueCells("Year of study:", yearOfStudy.toString, 4)
		val yearWeightingRow = keyValueCells("Year weightings:", yearWeightings.map(cyw => s"Year ${cyw.yearOfStudy} = ${cyw.weightingAsPercentage}").mkString("\n"), 5)
		yearWeightingRow.setHeight((yearWeightingRow.getHeight * (yearWeightings.size - 1)).toShort)
		keyValueCells("Normal CAT load:", normalLoad.toString, 6)
		keyValueCells("Student Count:", studentCount.toString, 7)
		keyValueCells("Grid Generated:", DateTime.now.toString, 8)

		{
			val row = sheet.createRow(9)
			val keyCell = row.createCell(0)
			keyCell.setCellValue("#")
			keyCell.setCellStyle(cellStyleMap(Fail))
			val valueCell = row.createCell(1)
			valueCell.setCellValue("Failed module")
		}
		{
			val row = sheet.createRow(10)
			val keyCell = row.createCell(0)
			keyCell.setCellValue("#*")
			keyCell.setCellStyle(cellStyleMap(Overcat))
			val valueCell = row.createCell(1)
			valueCell.setCellValue("Used in overcatting calculation")
		}
		{
			val row = sheet.createRow(11)
			val keyCell = row.createCell(0)
			keyCell.setCellValue("#?")
			val valueCell = row.createCell(1)
			valueCell.setCellValue("Agreed mark missing (using actual mark)")
		}
		{
			val row = sheet.createRow(12)
			val keyCell = row.createCell(0)
			keyCell.setCellValue("?")
			val valueCell = row.createCell(1)
			valueCell.setCellValue("Agreed mark and actual mark missing")
		}
	}

	private def getCellStyleMap(workbook: XSSFWorkbook): Map[GenerateExamGridExporter.Style, XSSFCellStyle] = {
		val headerStyle = {
			val cs = workbook.createCellStyle()
			val boldFont = workbook.createFont()
			boldFont.setFontHeight(10)
			boldFont.setBold(true)
			cs.setFont(boldFont)
			cs.setVerticalAlignment(VerticalAlignment.CENTER)
			cs
		}

		val headerRotatedStyle = {
			val cs = workbook.createCellStyle()
			val boldFont = workbook.createFont()
			boldFont.setFontHeight(10)
			boldFont.setBold(true)
			cs.setFont(boldFont)
			cs.setRotation(90)
			cs.setAlignment(HorizontalAlignment.CENTER)
			cs
		}

		val rotatedStyle = {
			val cs = workbook.createCellStyle()
			cs.setRotation(90)
			cs.setAlignment(HorizontalAlignment.CENTER)
			cs
		}

		val failStyle = {
			val cs = workbook.createCellStyle()
			val redFont = workbook.createFont()
			redFont.setFontHeight(10)
			redFont.setColor(new XSSFColor(new Color(175, 39, 35)))
			redFont.setUnderline(FontUnderline.DOUBLE)
			cs.setFont(redFont)
			cs
		}

		val overcatStyle = {
			val cs = workbook.createCellStyle()
			val greenFont = workbook.createFont()
			greenFont.setFontHeight(10)
			greenFont.setColor(new XSSFColor(new Color(89, 110, 49)))
			cs.setFont(greenFont)
			cs
		}

		val overriddenStyle = {
			val cs = workbook.createCellStyle()
			val blueFont = workbook.createFont()
			blueFont.setFontHeight(10)
			blueFont.setColor(new XSSFColor(new Color(35, 155, 146)))
			cs.setFont(blueFont)
			cs
		}

		Map(
			Header -> headerStyle,
			HeaderRotated -> headerRotatedStyle,
			Rotated -> rotatedStyle,
			Fail -> failStyle,
			Overcat -> overcatStyle,
			Overridden -> overriddenStyle
		)
	}

}
