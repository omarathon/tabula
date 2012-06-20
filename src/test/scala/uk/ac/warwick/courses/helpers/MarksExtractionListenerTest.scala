package uk.ac.warwick.courses.helpers

import java.io.ByteArrayInputStream
import java.io.InputStream
import scala.collection.JavaConversions._
import org.apache.poi.hssf.eventusermodel.HSSFEventFactory
import org.apache.poi.hssf.eventusermodel.HSSFRequest
import org.apache.poi.hssf.usermodel.HSSFSheet
import org.apache.poi.hssf.usermodel.HSSFWorkbook
import org.apache.poi.poifs.filesystem.POIFSFileSystem
import org.apache.poi.ss.usermodel.Row
import org.apache.poi.ss.usermodel.Cell
import org.junit.Test
import uk.ac.warwick.courses.JavaImports.JList
import uk.ac.warwick.courses.AppContextTestBase
import org.apache.poi.openxml4j.opc.OPCPackage
import org.apache.poi.xssf.eventusermodel.XSSFReader
import org.apache.poi.xssf.model.SharedStringsTable
import org.xml.sax.XMLReader
import org.xml.sax.helpers.XMLReaderFactory
import org.xml.sax.ContentHandler
import org.xml.sax.InputSource
import uk.ac.warwick.courses.TestBase
import uk.ac.warwick.courses.helpers.Closeables._

class MarksExtractionListenerTest extends TestBase {
	
    @Test def readOLE2ExcelFile {
      closeThis(new ByteArrayInputStream(resourceAsBytes("marks.xls"))) { fin => 
      	  val poifs = new POIFSFileSystem(fin)
	      closeThis(poifs.createDocumentInputStream("Workbook")) { din =>
		      val req = new HSSFRequest
		      val listener = new MarksExtractionListener()
		      req.addListenerForAllRecords(listener)
		      val factory = new HSSFEventFactory
		      factory.processEvents(req, din)
		      println(listener.markItems)
      	  }
      }
	}
    
    @Test def readXSSFExcelFile {
      val fin =  new ByteArrayInputStream(resourceAsBytes("marks.xlsx"))
      val pkg = OPCPackage.open(fin)
      val reader = new XSSFReader(pkg)
      val sst = reader.getSharedStringsTable()
      val parser = fetchSheetParser(sst)
      
      for (sheet <- reader.getSheetsData) {
        val sheetSource = new InputSource(sheet)
        parser.parse(sheetSource)
        sheet close
      }
	}
    
    def  fetchSheetParser(sst:SharedStringsTable) = {
      val parser = XMLReaderFactory.createXMLReader("org.apache.xerces.parsers.SAXParser")
      val handler = new ExcelSheetHandler(sst)
      parser.setContentHandler(handler)
      parser
	}
    
    
}