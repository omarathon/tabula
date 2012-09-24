package uk.ac.warwick.courses.services.docconversion

import java.io.ByteArrayInputStream
import scala.collection.JavaConversions._
import org.apache.poi.openxml4j.opc.OPCPackage
import org.apache.poi.xssf.eventusermodel.XSSFReader
import org.apache.poi.xssf.model.SharedStringsTable
import org.springframework.stereotype.Service
import org.xml.sax.InputSource
import org.xml.sax.helpers.XMLReaderFactory
import uk.ac.warwick.courses.JavaImports._
import uk.ac.warwick.courses.helpers.ArrayList
import java.io.FileInputStream
import java.io.InputStream
import scala.reflect.BeanProperty

class MarkItem {

	@BeanProperty var universityId: String = _
	@BeanProperty var actualMark: String = _
	@BeanProperty var actualGrade: String = _
	@BeanProperty var isValid = true
	@BeanProperty var warningMessage: String = _

	def this(universityId: String, actualMark: String, actualGrade: String) = {
		this();
		this.universityId = universityId;
		this.actualMark = actualMark;
		this.actualGrade = actualGrade;
	}
}

@Service
class MarksExtractor {

	/**
	 * Method for reading in a xlsx spreadsheet and converting it into a list of MarkItems
	 */
	def readXSSFExcelFile(file: InputStream): JList[MarkItem] = {
		val pkg = OPCPackage.open(file);
		val reader = new XSSFReader(pkg)
		val sst = reader.getSharedStringsTable()
		val markItems: JList[MarkItem] = ArrayList()
		val parser = fetchSheetParser(sst, markItems)
		for (sheet <- reader.getSheetsData) {
			val sheetSource = new InputSource(sheet)
			parser.parse(sheetSource)
			sheet close
		}
		markItems
	}

	def fetchSheetParser(sst: SharedStringsTable, markItems: JList[MarkItem]) = {
		val parser = XMLReaderFactory.createXMLReader("org.apache.xerces.parsers.SAXParser")
		val handler = new XslxSheetHandler(sst, markItems)
		parser.setContentHandler(handler)
		parser
	}
}

