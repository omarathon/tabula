package uk.ac.warwick.tabula.coursework.web.controllers.admin

import collection.JavaConversions._
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.beans.factory.annotation.Configurable
import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.RequestMethod
import org.springframework.beans.factory.annotation.Autowired
import uk.ac.warwick.tabula.coursework.web.controllers.CourseworkController
import uk.ac.warwick.tabula.web.Mav
import uk.ac.warwick.tabula.actions.Participate
import uk.ac.warwick.tabula.services.fileserver.FileServer
import uk.ac.warwick.tabula.coursework.commands.assignments.ListSubmissionsCommand
import uk.ac.warwick.tabula.coursework.commands.assignments.DownloadAllSubmissionsCommand
import uk.ac.warwick.tabula.coursework.commands.assignments.DownloadSubmissionsCommand
import org.joda.time.format.DateTimeFormat
import org.joda.time.format.ISODateTimeFormat
import org.joda.time.ReadableInstant
import javax.servlet.http.HttpServletResponse
import uk.ac.warwick.tabula.data.model.SavedSubmissionValue
import uk.ac.warwick.tabula.coursework.commands.assignments.SubmissionListItem
import uk.ac.warwick.tabula.data.model.Assignment
import uk.ac.warwick.tabula.helpers.DateTimeOrdering._
import uk.ac.warwick.tabula.data.model.FileAttachment
import uk.ac.warwick.tabula.DateFormats
import java.io.StringWriter
import uk.ac.warwick.util.csv.GoodCsvDocument
import uk.ac.warwick.util.csv.CSVLineWriter
import uk.ac.warwick.tabula.web.views.CSVView
import scala.collection.immutable.ListMap
import scala.collection.immutable.SortedSet
import scala.collection.mutable.LinkedHashSet

/**
 * Download submissions metadata.
 */
@Controller
class SubmissionsInfoController extends CourseworkController {

	val isoFormatter = DateFormats.IsoDateTime
	val csvFormatter = DateFormats.CSVDateTime

	def isoFormat(i: ReadableInstant) = isoFormatter print i
	def csvFormat(i: ReadableInstant) = csvFormatter print i

	var checkIndex = true

	@RequestMapping(value = Array("/admin/module/{module}/assignments/{assignment}/submissions.xml"), method = Array(GET, HEAD))
	def xml(command: ListSubmissionsCommand) = {
		mustBeLinked(mandatory(command.assignment), mandatory(command.module))
		mustBeAbleTo(Participate(command.module))
		command.checkIndex = checkIndex

		val items = command.apply.sortBy { _.submission.submittedDate }.reverse
		val assignment = command.assignment

		<submissions>
			{ assignmentElement(assignment) }
			{ items map submissionElement }
		</submissions>
	}

	def assignmentElement(assignment: Assignment) =
		<assignment id={ assignment.id } open-date={ isoFormat(assignment.openDate) } close-date={ isoFormat(assignment.closeDate) }/>

	def submissionElement(item: SubmissionListItem) =
		<submission id={ item.submission.id } submission-time={ isoFormat(item.submission.submittedDate) } university-id={ item.submission.universityId } downloaded={ item.downloaded.toString }>
			{ item.submission.values map fieldElement(item) }
		</submission>

	def fieldElement(item: SubmissionListItem)(value: SavedSubmissionValue) =
		if (value.hasAttachments)
			<field name={ value.name }>
				{
					value.attachments map { file =>
						<file name={ file.name } zip-path={ item.submission.zipFileName(file) }/>
					}
				}
			</field>
		else if (value.value != null)
			<field name={ value.name } value={ value.value }/>
		else
			Nil //empty Node seq, no element

			
	@RequestMapping(value = Array("/admin/module/{module}/assignments/{assignment}/submissions.csv"), method = Array(GET, HEAD))
	def csv(command: ListSubmissionsCommand) = {
		mustBeLinked(mandatory(command.assignment), mandatory(command.module))
		mustBeAbleTo(Participate(command.module))
		command.checkIndex = checkIndex

		val items = command.apply.sortBy { _.submission.submittedDate }.reverse
		val assignment = command.assignment
		val writer = new StringWriter
		val csvBuilder = new SubmissionsCSVBuilder(items)
		val doc = new GoodCsvDocument(csvBuilder, null)

		doc.setHeaderLine(true)
		csvBuilder.headers foreach (header => doc.addHeaderField(header))
		items foreach (item => doc.addLine(item))
		doc.write(writer)

		new CSVView("submissions.csv", writer.toString)
	}

	class SubmissionsCSVBuilder(items:Seq[SubmissionListItem]) extends CSVLineWriter[SubmissionListItem] {
		val headers = {
			var keys = Set[String]()
			
			// have to iterate all items to ensure complete field coverage. bleh :(
			items foreach ( item => keys = keys ++ fieldData(item).keySet )
			
			// return core headers in insertion order (make it easier for parsers), followed by alpha-sorted field headers
			(coreData(items.head).keys.toList ++ keys.toList.sorted)
		}
		
		def getNoOfColumns(item:SubmissionListItem) = headers.size
		
		def getColumn(item:SubmissionListItem, i:Int) = {
			itemData(item).get(headers.get(i)) getOrElse ""
		}
	}
	
	def itemData(item: SubmissionListItem) = coreData(item) ++ fieldData(item)

	def coreData(item: SubmissionListItem) = ListMap(
		"submission-id" -> item.submission.id,
		"submission-time" -> csvFormat(item.submission.submittedDate),
		"university-id" -> item.submission.universityId,
		"assignment-id" -> item.submission.assignment.id,
		"downloaded" -> item.downloaded.toString.toLowerCase
	)

	def fieldData(item: SubmissionListItem) = {
		var fieldDataMap = ListMap[String, String]()
		
		item.submission.values foreach ( value =>
			if (value.hasAttachments)
				value.attachments foreach {file => {
					fieldDataMap += (value.name + "-name") -> file.name
					fieldDataMap += (value.name + "-zip-path") -> item.submission.zipFileName(file)
				}}
			else if (value.value != null)
				fieldDataMap += value.name -> value.value
		)
		
		fieldDataMap
	}
}