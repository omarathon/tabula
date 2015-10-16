package uk.ac.warwick.tabula.api.web.helpers

import uk.ac.warwick.tabula.DateFormats
import uk.ac.warwick.tabula.data.model.MapLocation
import uk.ac.warwick.tabula.timetables.EventOccurrence

trait EventOccurrenceToJsonConverter {

	def jsonEventOccurrenceObject(event: EventOccurrence) = Map(
		"uid" -> event.uid,
		"name" -> event.name,
		"title" -> event.title,
		"description" -> event.description,
		"eventType" -> event.eventType.displayName,
		"start" -> DateFormats.IsoDateTime.print(event.start),
		"end" -> DateFormats.IsoDateTime.print(event.end),
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
		"staffUniversityIds" -> event.staffUniversityIds
	)

}
