package uk.ac.warwick.tabula.commands

import uk.ac.warwick.tabula.data.model.{StudentMember, Member}

trait MemberCollectionHelper {

	def allMembersRoutesSorted(members: Iterable[Member]) = {
		val routes = for {
			student <- members.collect { case student: StudentMember => student };
			course <- student.mostSignificantCourseDetails
			if Option(course.route).isDefined
		} yield course.route
		routes.toSeq.sortBy(_.code).distinct
	}

	def allMembersYears(members: Iterable[Member]) = {
		val years = for (
			student <- members.collect { case student: StudentMember => student };
			course <- student.mostSignificantCourseDetails
		) yield course.latestStudentCourseYearDetails.yearOfStudy
		years.toSeq.distinct.sorted
	}
}
