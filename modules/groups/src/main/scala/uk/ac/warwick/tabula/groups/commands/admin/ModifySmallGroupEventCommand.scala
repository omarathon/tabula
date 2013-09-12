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
import uk.ac.warwick.tabula.system.BindListener
import org.springframework.validation.BindingResult
import scala.collection.JavaConverters._
import uk.ac.warwick.tabula.helpers.StringUtils._

/**
 * Common superclass for creation and modification. Note that any defaults on the vars here are defaults
 * for creation; the Edit command should call .copyFrom(SmallGroupEvent) to copy any existing properties.
 */
abstract class ModifySmallGroupEventCommand extends PromisingCommand[SmallGroupEvent] with SelfValidating with BindListener {
	
	var weeks: JSet[JInteger] = JSet()
	
	var day: DayOfWeek = _
	
	var startTime: LocalTime = CreateSmallGroupEventCommand.DefaultStartTime
	
	var endTime: LocalTime = CreateSmallGroupEventCommand.DefaultEndTime
	
	var location: String = _
	
	var title: String = _
	
	var tutors: JList[String] = JArrayList()
	
	// Used by parent command
	var delete: Boolean = false
	
	def weekRanges = Option(weeks) map { weeks => WeekRange.combine(weeks.asScala.toSeq.map { _.intValue }) } getOrElse(Seq())
	def weekRanges_=(ranges: Seq[WeekRange]) {
		weeks = 
			JHashSet(ranges
				.flatMap { range => range.minWeek to range.maxWeek }
				.map(i => JInteger(Some(i)))
				.toSet)
	}
	
	def validate(errors: Errors) {
		// Skip validation when this event is being deleted
		if (!delete) {
			if (weeks == null || weeks.isEmpty) errors.rejectValue("weeks", "smallGroupEvent.weeks.NotEmpty")
			
			if (day == null) errors.rejectValue("day", "smallGroupEvent.day.NotEmpty")

			if (startTime == null) errors.rejectValue("startTime", "smallGroupEvent.startTime.NotEmpty")
			
			if (endTime == null) errors.rejectValue("endTime", "smallGroupEvent.endTime.NotEmpty")
			
			if (endTime.isBefore(startTime)) errors.rejectValue("endTime", "smallGroupEvent.endTime.beforeStartTime")
		}
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
		
		if (event.tutors == null) event.tutors = UserGroup.ofUsercodes
		event.tutors.includeUsers = tutors
	}
	
	override def onBind(result: BindingResult) {
		// Find all empty textboxes for tutors and remove them - otherwise we end up with a never ending list of empties
		val indexesToRemove = tutors.asScala.zipWithIndex.flatMap { case (tutor, index) =>
			if (!tutor.hasText) Some(index)
			else None
		}
		
		// We reverse because removing from the back is better
		indexesToRemove.reverse.foreach { tutors.remove(_) }
	}
}