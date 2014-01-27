package uk.ac.warwick.tabula.scheduling.helpers

import uk.ac.warwick.tabula.data.{MemberDao, StudentCourseDetailsDao, StudentCourseYearDetailsDao}
import uk.ac.warwick.tabula.data.model.{StudentCourseDetails, StudentCourseYearDetails, StudentCourseYearKey, StudentMember}
import scala.collection.mutable.HashSet
import uk.ac.warwick.spring.Wire

class ImportRowTracker {
	var universityIdsSeen = new HashSet[String]
	var scjCodesSeen = new HashSet[String]
	var studentCourseYearDetailsSeen = new HashSet[StudentCourseYearKey]

	var memberDao = Wire.auto[MemberDao]
	var studentCourseDetailsDao = Wire.auto[StudentCourseDetailsDao]
	var studentCourseYearDetailsDao = Wire.auto[StudentCourseYearDetailsDao]

	def newStaleUniversityIds: Seq[String] = {
		val allFreshUniIds = memberDao.getFreshUniversityIds.toSet
		(allFreshUniIds -- universityIdsSeen).toSeq
	}

	def newStaleScjCodes: Seq[String] = {
		val allFreshScjCodes = studentCourseDetailsDao.getFreshScjCodes.toSet
		(allFreshScjCodes -- scjCodesSeen).toSeq
	}

	def newStaleScydIds: Seq[String] = {
		//logger.warn("Converting enrolment keys to ids ...")
		val scydIdsSeen = studentCourseYearDetailsDao.convertKeysToIds(studentCourseYearDetailsSeen)
		val allFreshIds = studentCourseYearDetailsDao.getFreshIds.toSet
		(allFreshIds -- scydIdsSeen).toSeq
	}

}
