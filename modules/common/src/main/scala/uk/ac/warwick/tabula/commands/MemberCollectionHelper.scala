package uk.ac.warwick.tabula.commands

import uk.ac.warwick.tabula.data.model.Member

trait MemberCollectionHelper {

	def allMembersRoutesSorted(members: Iterable[Member]) = {
		val routes = for {
			member <- members
			course <- member.mostSignificantCourseDetails
			if Option(course.route).isDefined
		} yield course.route
		routes.toSeq.sortBy(_.code).distinct
	}

	def allMembersYears(members: Iterable[Member]) = {
		val years = for (
			member <- members;
			course <- member.mostSignificantCourseDetails
		) yield course.latestStudentCourseYearDetails.yearOfStudy
		years.toSeq.distinct.sorted
	}
}
