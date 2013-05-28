package uk.ac.warwick.tabula.groups.commands.admin

import org.hibernate.validator.constraints._
import uk.ac.warwick.tabula.data.model.Module
import uk.ac.warwick.tabula.commands.SelfValidating
import uk.ac.warwick.tabula.commands.Command
import uk.ac.warwick.tabula.data.model.groups.SmallGroupSet
import uk.ac.warwick.tabula.data.model.groups.SmallGroupFormat
import uk.ac.warwick.tabula.AcademicYear
import org.joda.time.DateTime
import uk.ac.warwick.tabula.data.model.UserGroup
import uk.ac.warwick.tabula.data.model.groups.SmallGroup
import org.springframework.validation.Errors
import uk.ac.warwick.tabula.helpers.Promise
import uk.ac.warwick.tabula.data.model.groups.SmallGroupEvent
import uk.ac.warwick.tabula.commands.PromisingCommand
import scala.collection.mutable
import org.joda.time.LocalTime
import uk.ac.warwick.tabula.data.model.groups.WeekRange
import uk.ac.warwick.tabula.data.model.groups.DayOfWeek

/**
 * Common superclass for creation and modification. Note that any defaults on the vars here are defaults
 * for creation; the Edit command should call .copyFrom(SmallGroupEvent) to copy any existing properties.
 */
abstract class ModifySmallGroupEventCommand extends PromisingCommand[SmallGroupEvent] with SelfValidating {
	
	@NotEmpty
	var weekRanges: Seq[WeekRange] = mutable.Seq()
	
	@NotEmpty
	var day: DayOfWeek = _
	
	@NotEmpty
	var startTime: LocalTime = _
	
	@NotEmpty
	var endTime: LocalTime = _
	
	var location: String = _
	
	var title: String = _
	
	var tutors: JList[String] = JArrayList()
	
	def validate(errors: Errors) {
		// TODO
	}
	
	def copyFrom(event: SmallGroupEvent) {
		title = event.title
		location = event.location
		weekRanges = event.weekRanges
		day = event.day
		startTime = event.startTime
		endTime = event.endTime
		
		if (event.tutors != null) tutors.addAll(event.tutors.includeUsers)
	}
	
	def copyTo(event: SmallGroupEvent) {
		event.title = title
		event.location = location
		event.weekRanges = weekRanges
		event.day = day
		event.startTime = startTime
		event.endTime = endTime
		
		if (event.tutors == null) event.tutors = new UserGroup
		event.tutors.includeUsers = tutors
	}
}