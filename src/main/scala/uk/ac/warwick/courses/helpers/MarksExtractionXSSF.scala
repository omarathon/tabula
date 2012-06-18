package uk.ac.warwick.courses.helpers

import org.xml.sax.helpers.DefaultHandler
import org.apache.poi.xssf.model.SharedStringsTable
import org.xml.sax.Attributes
import org.apache.poi.xssf.usermodel.XSSFRichTextString

class ExcelSheetHandler extends DefaultHandler  with Logging  {
   
  var sst:SharedStringsTable = _
  var lastContents:String = null
  var nextIsString:Boolean = false

  def this(sst:SharedStringsTable) = {
    this();
    this.sst = sst;
  }
   
  override def startElement(uri:String, localName:String, name:String, attributes:Attributes) = {
    // c  a cell
    if(name.equals("c")) {
      logger.info("encountered new cell" )
	  logger.info(attributes.getValue("r") + " - ");
				
	  val cellType = attributes.getValue("t");
	  nextIsString = (cellType != null && cellType.equals("s"))
	  // Clear contents cache
	  lastContents = "";
    }
  }
  
  override def endElement(uri:String, localName:String, name:String) = {
    // Process the last contents as required.		
	if(nextIsString) {
	  val idx = Integer.parseInt(lastContents);
	  lastContents = new XSSFRichTextString(sst.getEntryAt(idx)).toString();
      nextIsString = false;
	}
    // v is the contents of a cell	
	if(name.equals("v")) {
		System.out.println(lastContents);
	}
  }
  
  override def characters(ch:Array[Char],  start:Int, length:Int) ={
    lastContents += new String(ch, start, length);
  }
  
}