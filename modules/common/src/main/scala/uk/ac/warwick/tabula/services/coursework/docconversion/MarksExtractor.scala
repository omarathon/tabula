package uk.ac.warwick.tabula.services.coursework.docconversion

import uk.ac.warwick.spring.Wire

import scala.collection.JavaConversions._
import org.apache.poi.openxml4j.opc.OPCPackage
import org.apache.poi.xssf.eventusermodel.{ReadOnlySharedStringsTable, XSSFReader}
import org.springframework.stereotype.Service
import org.xml.sax.InputSource
import uk.ac.warwick.tabula.JavaImports._
import java.io.InputStream

class MarkItem {

	var universityId: String = _
	var actualMark: String = _
	var actualGrade: String = _
	var isValid = true
	var isModified = false
	var isPublished = false
	var hasAdjustment = false

	def this(universityId: String, actualMark: String, actualGrade: String) = {
		this()
		this.universityId = universityId
		this.actualMark = actualMark
		this.actualGrade = actualGrade
	}
}

@Service
class MarksExtractor {

	/**
	 * Method for reading in a xlsx spreadsheet and converting it into a list of MarkItems
	 */
	def readXSSFExcelFile(file: InputStream): JList[MarkItem] = {
		val pkg = OPCPackage.open(file)
		val sst = new ReadOnlySharedStringsTable(pkg)
		val reader = new XSSFReader(pkg)
		val styles = reader.getStylesTable
		val markItems: JList[MarkItem] = JArrayList()
		val sheetHandler = new MarkItemXslxSheetHandler(styles, sst, markItems)
		val parser = sheetHandler.fetchSheetParser
		for (sheet <- reader.getSheetsData) {
			val sheetSource = new InputSource(sheet)
			parser.parse(sheetSource)
			sheet.close()
		}
		markItems.filterNot(markItem => markItem.universityId == null && markItem.actualMark == null && markItem.actualGrade == null)
	}
}

trait MarksExtractorComponent {
	val marksExtractor: MarksExtractor
}

trait AutowiringMarksExtractorComponent extends MarksExtractorComponent {
	val marksExtractor: MarksExtractor = Wire[MarksExtractor]
}
