package uk.ac.warwick.tabula.profiles.services.docconversion

import java.io.ByteArrayInputStream
import scala.collection.JavaConversions._
import org.apache.poi.openxml4j.opc.OPCPackage
import org.apache.poi.xssf.eventusermodel.XSSFReader
import org.apache.poi.xssf.model.SharedStringsTable
import org.springframework.stereotype.Service
import org.xml.sax.InputSource
import org.xml.sax.helpers.XMLReaderFactory
import uk.ac.warwick.tabula.JavaImports._
import java.io.FileInputStream
import java.io.InputStream
import org.apache.poi.xssf.eventusermodel.ReadOnlySharedStringsTable
import org.apache.poi.xssf.eventusermodel.XSSFSheetXMLHandler.SheetContentsHandler
import org.apache.poi.xssf.model.StylesTable
import uk.ac.warwick.tabula.helpers.Logging
import org.apache.poi.xssf.eventusermodel.XSSFSheetXMLHandler
import org.apache.poi.ss.util.CellReference
import uk.ac.warwick.tabula.data.model.Member
import uk.ac.warwick.tabula.helpers.StringUtils._
import uk.ac.warwick.tabula.UniversityId

class RawStudentRelationship {

	var targetUniversityId: String = _
	var agentUniversityId: String = _

	def this(targetUniversityId: String, agentUniversityId: String) = {
		this();
		this.targetUniversityId = targetUniversityId
		this.agentUniversityId = agentUniversityId
	}
	
	override def toString() = "Student=%s, Agent=%s".format(targetUniversityId, agentUniversityId)
}

object RawStudentRelationshipExtractor {
	val AcceptedFileExtensions = Seq(".xlsx")
}

@Service
class RawStudentRelationshipExtractor {

	/**
	 * Method for reading in a xlsx spreadsheet and converting it into a list of relationships
	 */
	def readXSSFExcelFile(file: InputStream): JList[RawStudentRelationship] = {
		val pkg = OPCPackage.open(file);
		val sst = new ReadOnlySharedStringsTable(pkg)
		val reader = new XSSFReader(pkg)
		val styles = reader.getStylesTable
		val rawStudentRelationships: JList[RawStudentRelationship] = JArrayList()
		val handler = new XslxParser(styles, sst, rawStudentRelationships)
		val parser = handler.fetchSheetParser
		for (sheet <- reader.getSheetsData) {
			val sheetSource = new InputSource(sheet)
			parser.parse(sheetSource)
			sheet.close()
		}
		rawStudentRelationships
	}
}

class XslxParser(val styles: StylesTable, val sst: ReadOnlySharedStringsTable, val rawStudentRelationships: JList[RawStudentRelationship])
	extends SheetContentsHandler with Logging {

	var isParsingHeader = true // flag to parse the first row for column headers
	var foundStudentInRow = false
	var foundAgentInRow = false

	var columnMap = scala.collection.mutable.Map[Short, String]()
	var currentRawStudentRelationship: RawStudentRelationship = _
	val xssfHandler = new XSSFSheetXMLHandler(styles, sst, this, false)

	def fetchSheetParser = {
		val parser = XMLReaderFactory.createXMLReader("org.apache.xerces.parsers.SAXParser")
		parser.setContentHandler(xssfHandler)
		parser
	}

	// implement SheetContentsHandler
	def headerFooter(text: String, isHeader: Boolean, tagName: String) = {
		// don't care about handling this, but required for interface
	}

	def startRow(row: Int) = {
		logger.debug("startRow: " + row.toString)
		isParsingHeader = (row == 0)
		currentRawStudentRelationship = new RawStudentRelationship
		foundStudentInRow = false
		foundAgentInRow = false
	}

	def cell(cellReference: String, formattedValue: String) = {
		val col = new CellReference(cellReference).getCol

		if (isParsingHeader) columnMap(col) = formattedValue
		else if (columnMap.containsKey(col)) {
			columnMap(col) match {
				case "student_id" => {
					currentRawStudentRelationship.targetUniversityId = UniversityId.zeroPad(formattedValue)
					foundStudentInRow = true
				}
				case "agent_id" => {
					if (formattedValue.hasText && formattedValue != "ERROR:#N/A") {
						currentRawStudentRelationship.agentUniversityId = UniversityId.zeroPad(formattedValue)
						foundAgentInRow = true
					}
				}
				case _ => // ignore anything else
			}
		}
	}

	def endRow = if (!isParsingHeader && foundStudentInRow) rawStudentRelationships.add(currentRawStudentRelationship)
}
