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

class RawMemberRelationship {

	@BeanProperty var subjectUniversityId: String = _
	@BeanProperty var agentUniversityId: String = _
	@BeanProperty var agentName: String = _
	@BeanProperty var isValid = true
	@BeanProperty var warningMessage: String = _

	def this(subjectUniversityId: String, agentUniversityId: String, agentName: String) = {
		this();
		this.subjectUniversityId = subjectUniversityId
		this.agentUniversityId = agentUniversityId
		this.agentName = agentName
	}
}

@Service
class MemberRelationshipExtractor {

	/**
	 * Method for reading in a xlsx spreadsheet and converting it into a list of relationships
	 */
	def readXSSFExcelFile(file: InputStream): JList[RawMemberRelationship] = {
		val pkg = OPCPackage.open(file);
		val reader = new XSSFReader(pkg)
		val sst = reader.getSharedStringsTable()
		val memberRelationships: JList[RawMemberRelationship] = ArrayList()
		val parser = fetchSheetParser(sst, memberRelationships)
		for (sheet <- reader.getSheetsData) {
			val sheetSource = new InputSource(sheet)
			parser.parse(sheetSource)
			sheet close
		}
		memberRelationships
	}

	def fetchSheetParser(sst: SharedStringsTable, memberRelationships: JList[RawMemberRelationship]) = {
		val parser = XMLReaderFactory.createXMLReader("org.apache.xerces.parsers.SAXParser")
		val handler = new XslxSheetHandler(sst, memberRelationships)
		parser.setContentHandler(handler)
		parser
	}
}

