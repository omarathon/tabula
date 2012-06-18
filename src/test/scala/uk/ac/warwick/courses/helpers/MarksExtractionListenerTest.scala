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

class MarksExtractionListenerTest extends AppContextTestBase {
	
    @Test def readOLE2ExcelFile {
      val fin:ByteArrayInputStream =  new ByteArrayInputStream(resourceAsBytes("marks.xls"))
      val poifs:POIFSFileSystem  = new POIFSFileSystem(fin)
      val din:InputStream = poifs.createDocumentInputStream("Workbook")
      val req:HSSFRequest = new HSSFRequest
      val listener = new MarksExtractionListener()
      req addListenerForAllRecords(listener)
      val factory:HSSFEventFactory = new HSSFEventFactory
      factory.processEvents(req, din)
      din.close
      fin.close
      println(listener.markItems)
      
	
	}
    
    @Test def readXSSFExcelFile {
      val fin:ByteArrayInputStream =  new ByteArrayInputStream(resourceAsBytes("marks.xlsx"))
      val pkg:OPCPackage = OPCPackage.open(fin);
      val reader:XSSFReader  = new XSSFReader (pkg)
      val sst:SharedStringsTable = reader.getSharedStringsTable()
      val parser = fetchSheetParser(sst)
      
      val sheets = reader.getSheetsData
      while(sheets hasNext){
        val sheet = sheets.next()
        val sheetSource = new InputSource(sheet)
        parser.parse(sheetSource)
        sheet close
      }
	}
    
    def  fetchSheetParser(sst:SharedStringsTable) = {
      val parser:XMLReader = XMLReaderFactory.createXMLReader("org.apache.xerces.parsers.SAXParser")
      val handler:ContentHandler = new ExcelSheetHandler(sst);
      parser.setContentHandler(handler)
      parser
	}
    
    
}