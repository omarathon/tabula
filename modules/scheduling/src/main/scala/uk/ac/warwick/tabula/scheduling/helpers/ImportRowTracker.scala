package uk.ac.warwick.tabula.scheduling.helpers

import scala.collection.mutable.HashMap
import scala.collection.mutable.HashSet
import uk.ac.warwick.tabula.data.model.{StudentCourseDetails, StudentCourseYearDetails, StudentMember}
import uk.ac.warwick.tabula.data.model.StudentCourseYearKey

class ImportRowTracker {
	val universityIdsSeen = new HashSet[String]
	val scjCodesSeen = new HashSet[String]
	val studentCourseYearDetailsSeen = new HashSet[StudentCourseYearKey]
}
