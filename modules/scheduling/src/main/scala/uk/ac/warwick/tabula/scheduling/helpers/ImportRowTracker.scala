package uk.ac.warwick.tabula.scheduling.helpers

import org.apache.log4j.Logger
import uk.ac.warwick.membership.Member
import uk.ac.warwick.tabula.data.model.StudentCourseDetails
import uk.ac.warwick.tabula.data.model.StudentCourseYearDetails
import uk.ac.warwick.tabula.data.model.StudentCourseYearDetails
import scala.collection.mutable.HashSet
import uk.ac.warwick.tabula.data.model.StudentMember

class ImportRowTracker {
	val studentsSeen = new HashSet[StudentMember]
	val studentCourseDetailsSeen = new HashSet[StudentCourseDetails]
	val studentCourseYearDetailsSeen = new HashSet[StudentCourseYearDetails]
}
