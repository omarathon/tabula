package uk.ac.warwick.tabula.commands.scheduling.imports

import org.joda.time.DateTime
import uk.ac.warwick.spring.Wire
import uk.ac.warwick.tabula.commands._
import uk.ac.warwick.tabula.data._
import uk.ac.warwick.tabula.data.model.StudentCourseYearKey
import uk.ac.warwick.tabula.helpers.Logging
import uk.ac.warwick.tabula.permissions.Permissions
import uk.ac.warwick.tabula.services.scheduling.{ProfileImporter, SitsAcademicYearAware}
import uk.ac.warwick.tabula.system.permissions.{PermissionsChecking, PermissionsCheckingMethods, RequiresPermissionsChecking}
import uk.ac.warwick.tabula.{AutowiringFeaturesComponent, FeaturesComponent}
import Transactions._
import scala.collection.JavaConverters._

object StampMissingRowsCommand {
	def apply() =
		new StampMissingRowsCommandInternal
			with AutowiringMemberDaoComponent
			with AutowiringStudentCourseYearDetailsDaoComponent
			with AutowiringStudentCourseDetailsDaoComponent
			with AutowiringFeaturesComponent
			with ComposableCommandWithoutTransaction[Unit]
			with StampMissingRowsDescription
			with StampMissingRowsPermissions
}


class StampMissingRowsCommandInternal extends CommandInternal[Unit] with SitsAcademicYearAware with Logging with Daoisms {

	self: MemberDaoComponent with StudentCourseYearDetailsDaoComponent
		with StudentCourseDetailsDaoComponent with FeaturesComponent =>

	var profileImporter: ProfileImporter = Wire[ProfileImporter]

	override def applyInternal(): Unit = {
		val sitsCurrentAcademicYear = getCurrentSitsAcademicYearString
		val allUniversityIDs = transactional() { memberDao.getFreshUniversityIds() }

		logger.info(s"${allUniversityIDs.size} students to fetch from SITS for $sitsCurrentAcademicYear")

		val parsedSitsRows = allUniversityIDs.grouped(Daoisms.MaxInClauseCount).zipWithIndex.map { case (universityIds, groupCount) =>
			val sitsRows = profileImporter.multipleStudentInformationQuery.executeByNamedParam(
				if (features.includePastYears)
					Map("universityIds" -> universityIds.asJava).asJava
				else
					Map("year" -> sitsCurrentAcademicYear, "universityIds" -> universityIds).asJava
			).asScala

			logger.info(s"${(groupCount + 1) * Daoisms.MaxInClauseCount} students requested from SITS; ${sitsRows.size} rows found")

			(
				sitsRows.map(_.universityId.getOrElse("")).distinct,
				sitsRows.map(_.scjCode).distinct,
				sitsRows.map(row => new StudentCourseYearKey(row.scjCode, row.sceSequenceNumber)).distinct
				)
		}.toSeq

		val universityIdsSeen = parsedSitsRows.flatMap(_._1).distinct
		val scjCodesSeen = parsedSitsRows.flatMap(_._2).distinct
		val studentCourseYearKeysSeen = parsedSitsRows.flatMap(_._3)

		if (universityIdsSeen.isEmpty) {
			throw new UnsupportedOperationException("Could not find any students, so not marking all as missing")
		}

		if (scjCodesSeen.isEmpty) {
			throw new UnsupportedOperationException("Could not find any SCJ codes, so not marking all as missing")
		}

		if (studentCourseYearKeysSeen.isEmpty) {
			throw new UnsupportedOperationException("Could not find any year enrolments, so not marking all as missing")
		}

		val newStaleUniversityIds: Seq[String] = {
			(allUniversityIDs.toSet -- universityIdsSeen).toSeq
		}

		val newStaleScjCodes: Seq[String] = {
			val allFreshScjCodes = transactional() { studentCourseDetailsDao.getFreshScjCodes.toSet }
			(allFreshScjCodes -- scjCodesSeen).toSeq
		}

		val newStaleScydIds: Seq[String] = {
			val scydIdsSeen = transactional() { studentCourseYearDetailsDao.convertKeysToIds(studentCourseYearKeysSeen) }
			val allFreshIds = transactional() { studentCourseYearDetailsDao.getFreshIds.toSet }
			transactional() { (allFreshIds -- scydIdsSeen).toSeq }
		}

		logger.warn(s"Timestamping ${newStaleUniversityIds.size} missing students")
		transactional() { memberDao.stampMissingFromImport(newStaleUniversityIds, DateTime.now) }

		logger.warn(s"Timestamping ${newStaleScjCodes.size} missing studentCourseDetails")
		transactional() { studentCourseDetailsDao.stampMissingFromImport(newStaleScjCodes, DateTime.now) }

		logger.warn(s"Timestamping ${newStaleScydIds.size} missing studentCourseYearDetails")
		transactional() { studentCourseYearDetailsDao.stampMissingFromImport(newStaleScydIds, DateTime.now) }

		transactional() { session.flush() }
		transactional() { session.clear() }

		val newFreshUniIds = transactional() { memberDao.getFreshUniversityIds().toSet }
		val uniIDsStillNotMarked = newStaleUniversityIds.toSet.intersect(newFreshUniIds)
		if (uniIDsStillNotMarked.nonEmpty) {
			logger.error(s"There are still stale IDs that weren't marked as missing (${uniIDsStillNotMarked.size} total); here are a few: ${uniIDsStillNotMarked.take(5).mkString(", ")}")
		} else {
			logger.info(s"All ${newStaleUniversityIds.size} Uni IDs marked correctly")
		}
	}

}

trait StampMissingRowsPermissions extends RequiresPermissionsChecking with PermissionsCheckingMethods {

	override def permissionsCheck(p: PermissionsChecking) {
		p.PermissionCheck(Permissions.ImportSystemData)
	}

}

trait StampMissingRowsDescription extends Describable[Unit] {

	override lazy val eventName = "StampMissingRows"

	override def describe(d: Description) {

	}
}
