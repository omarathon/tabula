package uk.ac.warwick.tabula.coursework.web.controllers.admin

import uk.ac.warwick.tabula.coursework.commands.assignments._
import uk.ac.warwick.tabula.data.model._
import uk.ac.warwick.util.csv.CSVLineWriter
import scala.collection.immutable.ListMap
import scala.collection.JavaConverters._
import uk.ac.warwick.tabula.DateFormats
import org.joda.time.ReadableInstant
import uk.ac.warwick.tabula.helpers.StringUtils._
import scala.xml._
import org.apache.poi.xssf.usermodel.XSSFWorkbook
import org.apache.poi.xssf.usermodel.XSSFSheet
import org.apache.poi.ss.util.WorkbookUtil
import org.apache.commons.lang3.text.WordUtils
import org.apache.poi.hssf.usermodel.HSSFDataFormat
import uk.ac.warwick.tabula.coursework.web.Routes
import uk.ac.warwick.spring.Wire
import uk.ac.warwick.tabula.data.model.forms.SavedFormValue

object AdminHomeExports {
	class XMLBuilder(val department: Department, val info: DepartmentHomeInformation) extends AdminHomeExport {
	
		var topLevelUrl: String = Wire.property("${toplevel.url}")
	
		// Pimp XML elements to allow a map mutator for attributes
		implicit class PimpedElement(elem: Elem) {
			def %(attrs:Map[String,Any]) = {
				val seq = for( (n,v) <- attrs ) yield new UnprefixedAttribute(n, toXMLString(Some(v)), Null)
				(elem /: seq) ( _ % _ )
			}
		}
		
		def toXMLString(value: Option[Any]): String = value match {
			case Some(o: Option[Any]) => toXMLString(o)
			case Some(i: ReadableInstant) => isoFormat(i)
			case Some(b: Boolean) => b.toString.toLowerCase
			case Some(i: Int) => i.toString
			case Some(s: String) => s
			case Some(other) => other.toString
			case None => ""
		}
		
		def toXML = {
			val assignments = info.modules.flatMap { _.assignments.asScala }.filter { a => !a.deleted && !a.archived }
			
			<assignments>
				{ assignments map assignmentElement }
			</assignments>
		}
		
		def assignmentElement(assignment: Assignment) = {
			<assignment>

			</assignment> % assignmentData(assignment)
		}
	}
	
	trait AdminHomeExport {
		val department: Department
		val info: DepartmentHomeInformation
		def topLevelUrl: String
		
		val isoFormatter = DateFormats.IsoDateTime
		def isoFormat(i: ReadableInstant) = isoFormatter print i
		
		protected def assignmentData(assignment: Assignment): Map[String, Any] = Map(
			"module-code" -> assignment.module.code,
			"id" -> assignment.id,
			"open-date" -> assignment.openDate,
			"open-ended" -> assignment.openEnded,
			"close-date" -> (if (assignment.openEnded) "" else assignment.closeDate),
			"last-submission-date" -> assignment.submissions.asScala.map { _.submittedDate }.sortBy { _.getMillis }.reverse.headOption,
			"submissions-zip-url" -> (topLevelUrl + "/coursework" + Routes.admin.assignment.submissionsZip(assignment))
		)
	}
}
