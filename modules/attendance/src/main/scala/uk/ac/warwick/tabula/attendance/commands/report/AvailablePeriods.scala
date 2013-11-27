package uk.ac.warwick.tabula.attendance.commands.report

import org.joda.time.DateTime
import uk.ac.warwick.tabula.services.{MonitoringPointServiceComponent, TermService}
import uk.ac.warwick.tabula.{AcademicYear, ItemNotFoundException}
import uk.ac.warwick.tabula.attendance.commands.GroupMonitoringPointsByTerm
import uk.ac.warwick.tabula.data.model.StudentMember
import scala.collection.JavaConverters._

trait AvailablePeriods extends MonitoringPointServiceComponent with GroupMonitoringPointsByTerm {

	def getAvailablePeriods(allStudents: Seq[StudentMember], academicYear: AcademicYear) = {
		// Get the students' point sets
		val pointSets = monitoringPointService.findPointSetsForStudents(allStudents, academicYear)
		// Get all the term names for those point sets
		val termsWithPoints = groupByTerm(pointSets.flatMap(_.points.asScala), academicYear).keys.toSeq
		// Get the index in the list of all terms of the current term
		val thisTerm = termService.getTermFromDateIncludingVacations(DateTime.now).getTermTypeAsString
		val thisTermIndex = TermService.orderedTermNames.zipWithIndex
			.find(_._1 == thisTerm).getOrElse(throw new ItemNotFoundException())._2
		// Get the terms that have happened so far in the current academic year, including the current term
		val termsSoFarThisYear = TermService.orderedTermNames.slice(0, thisTermIndex + 1)
		// The terms to show to the user are those that have happened this year (including the current term)
		// AND that have some points happening during them
		val termsToShow = termsWithPoints.intersect(termsSoFarThisYear)
		// Of the terms to show, the ones that the user can choose as those where at least some of the given students have not reported
		val nonReportedTerms = monitoringPointService.findNonReportedTerms(allStudents, academicYear)
		termsToShow.map(term => term -> nonReportedTerms.contains(term))
	}

}
