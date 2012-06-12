package uk.ac.warwick.courses.web.controllers.admin

import collection.JavaConversions._
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.beans.factory.annotation.Configurable
import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.RequestMethod
import org.springframework.beans.factory.annotation.Autowired
import uk.ac.warwick.courses.web.controllers.BaseController
import uk.ac.warwick.courses.web.Mav
import uk.ac.warwick.courses.actions.Participate
import uk.ac.warwick.courses.services.fileserver.FileServer
import uk.ac.warwick.courses.commands.assignments.ListSubmissionsCommand
import uk.ac.warwick.courses.commands.assignments.DownloadAllSubmissionsCommand
import uk.ac.warwick.courses.commands.assignments.DownloadSubmissionsCommand
import org.joda.time.format.DateTimeFormat
import org.joda.time.format.ISODateTimeFormat
import org.joda.time.ReadableInstant
import javax.servlet.http.HttpServletResponse
import uk.ac.warwick.courses.data.model.SavedSubmissionValue
import uk.ac.warwick.courses.commands.assignments.SubmissionListItem
import uk.ac.warwick.courses.data.model.Assignment
import uk.ac.warwick.courses.helpers.DateTimeOrdering._
import uk.ac.warwick.courses.data.model.FileAttachment

/** Download submissions as XML.
  */
@Configurable @Controller
class SubmissionsInfoController extends BaseController {
	
	val formatter = ISODateTimeFormat.basicDateTimeNoMillis
	
	def format(i: ReadableInstant) = formatter print i
			
	@RequestMapping(value=Array("/admin/module/{module}/assignments/{assignment}/submissions.xml"), method=Array(GET, HEAD))
	def xml(command:ListSubmissionsCommand) = {
		mustBeLinked(mandatory(command.assignment), mandatory(command.module))
		mustBeAbleTo(Participate(command.module))
		
		val items = command.apply.sortBy { _.submission.submittedDate }.reverse
		val assignment = command.assignment
		
		<submissions>
			{assignmentElement(assignment)}
			{items map submissionElement}
		</submissions>
	}
	
	def assignmentElement(assignment: Assignment) = 
		<assignment id={assignment.id}
				open-date={format(assignment.openDate)}
				close-date={format(assignment.closeDate)} />
	
	def submissionElement(item: SubmissionListItem) =
		<submission id={item.submission.id} 
				submission-time={format(item.submission.submittedDate)} 
				university-id={item.submission.universityId}
				downloaded={item.downloaded.toString}>
			{item.submission.values map fieldElement(item)}
		</submission>
	
	def fieldElement(item: SubmissionListItem)(value: SavedSubmissionValue) = 
		if (value.hasAttachments)
			<field name={value.name}>
				{value.attachments map { file =>
					<file name={file.name} zip-path={item.submission.zipFileName(file)} />
				}}
			</field>
		else if (value.value != null)
			<field name={value.name} value={value.value} />
		else
			Nil //empty Node seq, no element
			
}