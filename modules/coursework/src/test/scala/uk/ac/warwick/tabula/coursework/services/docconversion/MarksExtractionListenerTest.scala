package uk.ac.warwick.tabula.helpers

import java.io.ByteArrayInputStream
import org.apache.poi.hssf.eventusermodel.HSSFEventFactory
import org.apache.poi.hssf.eventusermodel.HSSFRequest
import org.apache.poi.poifs.filesystem.POIFSFileSystem
import org.junit.Test
import uk.ac.warwick.tabula.coursework.TestBase
import uk.ac.warwick.tabula.helpers.Closeables.closeThis
import uk.ac.warwick.tabula.coursework.services.docconversion.XslSheetHandler
import uk.ac.warwick.tabula.coursework.services.docconversion.MarksExtractor

class MarksExtractionListenerTest extends TestBase with Logging {
	
    @Test def readOLE2ExcelFile {
      closeThis(new ByteArrayInputStream(resourceAsBytes("marks.xls"))) { fin => 
        val poifs = new POIFSFileSystem(fin)
        closeThis(poifs.createDocumentInputStream("Workbook")) { din =>
		  val req = new HSSFRequest
		  val listener = new XslSheetHandler()
		  req addListenerForAllRecords(listener)
		  val factory = new HSSFEventFactory
		  factory.processEvents(req, din)
		  listener.markItems.size should be (11) //header not skipped
        }
      }
	}
    
    @Test def readXSSFExcelFile {
      val fin =  new ByteArrayInputStream(resourceAsBytes("marks.xlsx"))
      val marksExtractor = new MarksExtractor()
      val marksList = marksExtractor.readXSSFExcelFile(fin)
      marksList.size should be (10)
	}

}