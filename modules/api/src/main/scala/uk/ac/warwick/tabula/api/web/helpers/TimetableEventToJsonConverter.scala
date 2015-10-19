package uk.ac.warwick.tabula.api.web.helpers

import uk.ac.warwick.tabula.data.model.MapLocation
import uk.ac.warwick.tabula.services.ProfileServiceComponent
import uk.ac.warwick.tabula.timetables.TimetableEvent

trait TimetableEventToJsonConverter {
	self: ProfileServiceComponent =>

	def jsonTimetableEventObject(event: TimetableEvent) = Map(
		"uid" -> event.uid,
		"name" -> event.name,
		"title" -> event.title,
		"description" -> event.description,
		"eventType" -> event.eventType.displayName,
		"weekRanges" -> event.weekRanges.map { range => Map("minWeek" -> range.minWeek, "maxWeek" -> range.maxWeek) },
		"day" -> event.day.name,
		"startTime" -> event.startTime.toString("HH:mm"),
		"endTime" -> event.endTime.toString("HH:mm"),
		"location" -> (event.location match {
			case Some(l: MapLocation) => Map(
				"name" -> l.name,
				"locationId" -> l.locationId
			)
			case Some(l) => Map("name" -> l.name)
			case _ => null
		}),
		"context" -> event.parent.shortName,
		"parent" -> event.parent,
		"comments" -> event.comments.orNull,
		"staffUniversityIds" -> event.staffUniversityIds,
		"staff" -> event.staffUniversityIds.map { universityId =>
			val member = profileService.getMemberByUniversityIdStaleOrFresh(universityId)

			Map(
				"universityId" -> universityId
			) ++ member.map { m =>
				Map(
					"firstName" -> m.firstName,
					"lastName" -> m.lastName,
					"email" -> m.email,
					"title" -> m.title,
					"fullFirstName" -> m.fullFirstName,
					"userType" -> m.userType.description,
					"jobTitle" -> m.jobTitle
				)
			}.getOrElse(Map())
		},
		"year" -> event.year.toString
	)

}
