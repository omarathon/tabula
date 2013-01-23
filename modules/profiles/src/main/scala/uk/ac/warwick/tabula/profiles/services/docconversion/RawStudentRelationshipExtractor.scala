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
import uk.ac.warwick.tabula.helpers.ArrayList
import java.io.FileInputStream
import java.io.InputStream
import scala.reflect.BeanProperty
import org.apache.poi.xssf.eventusermodel.ReadOnlySharedStringsTable
import org.apache.poi.xssf.eventusermodel.XSSFSheetXMLHandler.SheetContentsHandler
import org.apache.poi.xssf.model.StylesTable
import uk.ac.warwick.tabula.helpers.Logging
import org.apache.poi.xssf.eventusermodel.XSSFSheetXMLHandler
import org.apache.poi.ss.util.CellReference
import uk.ac.warwick.tabula.data.model.Member
import uk.ac.warwick.util.core.StringUtils.hasText

class RawStudentRelationship {

	@BeanProperty var targetUniversityId: String = _
	@BeanProperty var agentUniversityId: String = _
	@BeanProperty var agentName: String = _
	@BeanProperty var isValid = true
	@BeanProperty var warningMessage: String = _
	
	@BeanProperty var agentMember: Member = _
	@BeanProperty var targetMember: Member = _
	
	def this(targetUniversityId: String, agentUniversityId: String, agentName: String) = {
		this();
		this.targetUniversityId = targetUniversityId
		this.agentUniversityId = agentUniversityId
		this.agentName = agentName
	}
	
	def getAgentNameIfNonMember(): String = {
		if (hasText(agentUniversityId)) ""
		else agentName
	}
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
		val rawStudentRelationships: JList[RawStudentRelationship] = ArrayList()
		val handler = new XslxParser(styles, sst, rawStudentRelationships)
		val parser = handler.fetchSheetParser
		for (sheet <- reader.getSheetsData) {
			val sheetSource = new InputSource(sheet)
			parser.parse(sheetSource)
			sheet close
		}
		rawStudentRelationships
	}
}

class XslxParser(var styles: StylesTable, var sst: ReadOnlySharedStringsTable, var rawStudentRelationships: JList[RawStudentRelationship])
	extends SheetContentsHandler with Logging {

	var isParsingHeader = true // flag to parse the first row for column headers
	var foundStudentInRow = false
	var foundTutorInRow = false
	
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
		if (row > 0) {
			isParsingHeader = false
			currentRawStudentRelationship = new RawStudentRelationship
			foundStudentInRow = false
			foundTutorInRow = false
		}
	}
	
	def cell(cellReference: String, formattedValue: String) = {
		val col = new CellReference(cellReference).getCol
		//logger.debug("cell: " + col.toString + ": " + formattedValue)
		
		isParsingHeader match {
			case true => {
				columnMap(col) = formattedValue
			}
			case false => {
				if (columnMap.containsKey(col)) {
					columnMap(col) match {
						case "student_id" => {
							currentRawStudentRelationship.targetUniversityId = formattedValue	
							foundStudentInRow = true
						}
						case "tutor_id" => {
							currentRawStudentRelationship.agentUniversityId = formattedValue
							foundTutorInRow = true
						}
						case "tutor_name" => {
							currentRawStudentRelationship.agentName = formattedValue
							foundTutorInRow = true
						}
						case _ => // ignore anything else
					}
				}
			}
		}
	}
	
	def endRow = {
		if (!isParsingHeader) 
			//if (foundStudentInRow) 
				rawStudentRelationships.add(currentRawStudentRelationship)
			//else if (foundTutorInRow) // TODO need to give some kind of warning
	}
}
