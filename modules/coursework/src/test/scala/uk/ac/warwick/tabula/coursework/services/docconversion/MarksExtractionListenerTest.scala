package uk.ac.warwick.tabula.coursework.services.docconversion

import java.io.ByteArrayInputStream
import org.apache.poi.hssf.eventusermodel.HSSFEventFactory
import org.apache.poi.hssf.eventusermodel.HSSFRequest
import org.apache.poi.poifs.filesystem.POIFSFileSystem
import uk.ac.warwick.tabula.TestBase
import uk.ac.warwick.tabula.helpers.Closeables.closeThis
import org.apache.poi.openxml4j.exceptions.InvalidFormatException
import uk.ac.warwick.tabula.helpers.Logging

// scalastyle:off magic.number
class MarksExtractionListenerTest extends TestBase with Logging {
	
	@Test def readOLE2ExcelFile() {
		closeThis(new ByteArrayInputStream(resourceAsBytes("marks.xls"))) { fin =>
			closeThis(new POIFSFileSystem(fin).createDocumentInputStream("Workbook")) { din =>
				val req = new HSSFRequest
				val listener = new XslSheetHandler()
				req.addListenerForAllRecords(listener)
				val factory = new HSSFEventFactory
				factory.processEvents(req, din)
				listener.markItems.size should be (11) //header not skipped
			}
		}
	}

	// XLSX files are zips, but uploading any other zip gives a specific error
	// "Package should contain a content type part [M1.13]".
	@Test(expected=classOf[InvalidFormatException])
	def unexpectedZipFile() {
		val fin =  new ByteArrayInputStream(resourceAsBytes("feedback1.zip"))
		val marksExtractor = new MarksExtractor()
		val marksList = marksExtractor.readXSSFExcelFile(fin)
		marksList.size should be (10)
	}

	@Test def readXSSFExcelFile() {
		val fin =  new ByteArrayInputStream(resourceAsBytes("marks.xlsx"))
		val marksExtractor = new MarksExtractor()
		val marksList = marksExtractor.readXSSFExcelFile(fin)
		marksList.size should be (10)
	}

}