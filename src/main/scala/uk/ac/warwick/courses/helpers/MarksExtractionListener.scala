package uk.ac.warwick.courses.helpers

import org.apache.poi.hssf.eventusermodel.HSSFListener
import org.apache.poi.hssf.record.Record
import org.apache.poi.hssf.record.BOFRecord
import org.apache.poi.hssf.record.RowRecord
import org.apache.poi.hssf.record.SSTRecord
import org.apache.poi.hssf.record.BoundSheetRecord
import org.apache.poi.hssf.record.NumberRecord
import org.apache.poi.hssf.record.LabelSSTRecord
import uk.ac.warwick.courses.JavaImports.JList
import org.apache.poi.hssf.record.EOFRecord
import org.apache.poi.hssf.record.CellRecord
import oracle.net.aso.d
import org.apache.poi.util.LittleEndianOutput
import org.apache.poi.util.LittleEndianOutputStream
import java.io.ByteArrayOutputStream
import org.apache.poi.hssf.eventusermodel.FormatTrackingHSSFListener

class MarkItem(var universityId:String, var actualMark:String, var actualGrade:String) {
  def this() = this(null,null,null)
}

class MarksExtractionListener extends HSSFListener with Logging {
  
	val markItems:JList[MarkItem] = ArrayList();
    var sstrec:SSTRecord = null
    val formatListener = new FormatTrackingHSSFListener(this)
    
	@Override
	def processRecord(record:Record) = {
	  record  match {
	    case record:SSTRecord => {
	      logger.info("encountered new String Table Record - Saving" )
	      sstrec = record
	    }
	    case record:RowRecord => {
	      logger.info("encountered new row creating new mark record" )
	      markItems.add(new MarkItem)
	    }
	    case record:CellRecord => {
	      processCell(record, sstrec)
	      logger.info("Cell found at row "+record.getRow())
	    }
	    case _ =>
	  }
	}
    
    def processCell(record:CellRecord, currentSST:SSTRecord){
	  val rowNumber = record.getRow
	  val currentMarkItem = markItems.get(rowNumber)
	  val cellValue = record match {
	    case record:LabelSSTRecord => currentSST.getString(record.getSSTIndex()).toString()
	    case record:NumberRecord => record.getValue.toInt toString
	  }
	  
      record.getColumn match{
        case 0 => currentMarkItem.universityId = cellValue
	    case 1 => currentMarkItem.actualMark = cellValue
	    case 2 => currentMarkItem.actualGrade = cellValue
	    case _ =>
	  }
    }
}