package uk.ac.warwick.tabula.commands.exams.grids

import java.awt.Color

import org.apache.poi.ss.usermodel.{FontUnderline, HorizontalAlignment, VerticalAlignment}
import org.apache.poi.ss.util.CellRangeAddress
import org.apache.poi.xssf.usermodel.{XSSFSheet, XSSFCellStyle, XSSFColor, XSSFWorkbook}
import org.joda.time.DateTime
import uk.ac.warwick.tabula.AcademicYear
import uk.ac.warwick.tabula.data.model._
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
		allPreviousYearsScyds: Option[Seq[(AcademicYear, Seq[GenerateExamGridEntity])]],
		columnsByYear: Seq[(AcademicYear, Seq[ExamGridColumn])]
	): XSSFWorkbook = {
		val workbook = new XSSFWorkbook()

		// Styles
		val cellStyleMap = getCellStyleMap(workbook)

		val sheet = workbook.createSheet(academicYear.toString.replace("/","-"))

		var currentSection = ""
		var columnOffset = 0 // How many section columns have been added (so how many to shift the columnIndex)

		var scydHeadingRowNum = 0

		var categoriesRowColumnOffset = 0
		var titlesInCategoriesRowColumnOffset = 0
		var headersRowColumnOffset = 0

		summaryAndKey(sheet, cellStyleMap, department, academicYear, course, route, yearOfStudy, yearWeightings, normalLoad, scyds.size, allPreviousYearsScyds.isEmpty)

		if (allPreviousYearsScyds.isDefined) {
			scydHeadingRowNum = sheet.getLastRowNum + 1
			sheet.createRow(scydHeadingRowNum)
		}

		val categoryRow = sheet.createRow(sheet.getLastRowNum + 1)
		val titlesInCategoriesRow = sheet.createRow(sheet.getLastRowNum + 1)
		val headerRow = sheet.createRow(sheet.getLastRowNum + 1)

		// once per each academicYear
		columnsByYear.foreach { case (thisAcademicYear, columns) =>

			val indexedColumns = columns.zipWithIndex
			val categories = columns.collect{case c: HasExamGridColumnCategory => c}.groupBy(_.category)

			if (categories.nonEmpty) {
				var currentSection = ""
				var currentCategory = ""
				columnOffset = categoriesRowColumnOffset
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
					categoriesRowColumnOffset = columnIndex + columnOffset + 1
				}

				categoryRow.setHeight((maxCellWidth * 0.5).toShort)

				// Titles in categories
				currentSection = ""
				columnOffset = titlesInCategoriesRowColumnOffset
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
					titlesInCategoriesRowColumnOffset =  columnIndex + columnOffset + 1
				}

				titlesInCategoriesRow.setHeight((maxCellWidth * 0.38).toShort)

			}

			// Uncategorized column headers and secondary values
			currentSection = ""
			columnOffset = headersRowColumnOffset
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
				headersRowColumnOffset =  columnIndex + columnOffset + 1
			}


			(0 to columnsByYear.size + columnOffset).foreach(sheet.autoSizeColumn(_, true))
		}

		val thisYearsIndexedColumns = columnsByYear.find(_._1 == academicYear).get._2.zipWithIndex
		var currentColumn = 0
		// Values per student and section labels
		scyds.zipWithIndex.foreach { case (scyd, scydIndex) =>
			currentSection = ""
			columnOffset = 0
			val row = sheet.createRow(sheet.getLastRowNum + 1)
			thisYearsIndexedColumns.foreach { case (column, columnIndex) =>
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
				currentColumn = columnIndex + columnOffset
				column.renderExcelCell(row, columnIndex + columnOffset, scyd, cellStyleMap)
			}
			currentColumn = currentColumn + 1

			if (allPreviousYearsScyds.isDefined) {
				var previousYearsColumnOffset = 0
				allPreviousYearsScyds.get.foreach { case (previousAcademicYear, previousYearScyds) =>
					previousYearScyds.zipWithIndex.foreach { case (prevYearScyd, _ ) =>

						columnsByYear.filter {case (columnsYear, _) => columnsYear == previousAcademicYear}.foreach { case (_, thisYearsColumns) =>
							val previndexedColumns = thisYearsColumns.zipWithIndex

							previndexedColumns.foreach { case (col, columnIndex) =>
								col.renderExcelCell(row, columnIndex + currentColumn + 1, prevYearScyd, cellStyleMap)
								previousYearsColumnOffset = columnIndex + currentColumn
							}
							currentColumn = previousYearsColumnOffset + 1
							// only add scyd heading row data once
							if (scydIndex == 0){
								val cell = sheet.getRow(scydHeadingRowNum).createCell(currentColumn - previndexedColumns.size)
								cell.setCellValue(s"${prevYearScyd.studentCourseYearDetails.get.academicYear} Modules ( ${prevYearScyd.studentCourseYearDetails.get.studentCourseDetails.scjCode})")
								sheet.addMergedRegion(new CellRangeAddress(scydHeadingRowNum, scydHeadingRowNum, currentColumn - previndexedColumns.size, currentColumn))
							}
						}
						currentColumn = currentColumn + 1 // blank cell after each year
					}
				}
			}
		}
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
		count: Int,
		showStudentCount: Boolean
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
		if (showStudentCount) {
			keyValueCells("Student Count:", count.toString, 7)
		} else {
			keyValueCells("Count:", count.toString, 7)
		}
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
