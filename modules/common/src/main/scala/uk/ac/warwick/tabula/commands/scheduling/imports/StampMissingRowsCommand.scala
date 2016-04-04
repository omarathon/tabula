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

import scala.collection.JavaConverters._

object StampMissingRowsCommand {
	def apply() =
		new StampMissingRowsCommandInternal
			with AutowiringMemberDaoComponent
			with AutowiringStudentCourseYearDetailsDaoComponent
			with AutowiringStudentCourseDetailsDaoComponent
			with AutowiringFeaturesComponent
			with ComposableCommand[Unit]
			with StampMissingRowsDescription
			with StampMissingRowsPermissions
}


class StampMissingRowsCommandInternal extends CommandInternal[Unit] with SitsAcademicYearAware with Logging {

	self: MemberDaoComponent with StudentCourseYearDetailsDaoComponent
		with StudentCourseDetailsDaoComponent with FeaturesComponent =>

	var profileImporter = Wire[ProfileImporter]

	override def applyInternal() = {
		val sitsCurrentAcademicYear = getCurrentSitsAcademicYearString
		val allUniversityIDs = memberDao.getFreshUniversityIds()

		logger.info(s"${allUniversityIDs.size} students to fetch from SITS")

		val parsedSitsRows = allUniversityIDs.grouped(Daoisms.MaxInClauseCount).zipWithIndex.map { case (universityIds, groupCount) =>
			val sitsRows = profileImporter.multipleStudentInformationQuery.executeByNamedParam(
				if (features.includePastYears)
					Map("universityIds" -> universityIds.asJava).asJava
				else
					Map("year" -> sitsCurrentAcademicYear, "universityIds" -> universityIds).asJava
			).asScala

			logger.info(s"${(groupCount + 1) * Daoisms.MaxInClauseCount} students fetched from SITS")

			(
				sitsRows.map(_.universityId.getOrElse("")).distinct,
				sitsRows.map(_.scjCode).distinct,
				sitsRows.map(row => new StudentCourseYearKey(row.scjCode, row.sceSequenceNumber)).distinct
			)
		}.toSeq

		val universityIdsSeen = parsedSitsRows.flatMap(_._1).distinct
		val scjCodesSeen = parsedSitsRows.flatMap(_._2).distinct
		val studentCourseYearKeysSeen = parsedSitsRows.flatMap(_._3)

		val newStaleUniversityIds: Seq[String] = {
			(allUniversityIDs.toSet -- universityIdsSeen).toSeq
		}

		val newStaleScjCodes: Seq[String] = {
			val allFreshScjCodes = studentCourseDetailsDao.getFreshScjCodes.toSet
			(allFreshScjCodes -- scjCodesSeen).toSeq
		}

		val newStaleScydIds: Seq[String] = {
			val scydIdsSeen = studentCourseYearDetailsDao.convertKeysToIds(studentCourseYearKeysSeen)
			val allFreshIds = studentCourseYearDetailsDao.getFreshIds.toSet
			(allFreshIds -- scydIdsSeen).toSeq
		}

		logger.warn("Timestamping missing students")
		memberDao.stampMissingFromImport(newStaleUniversityIds, DateTime.now)

		logger.warn("Timestamping missing studentCourseDetails")
		studentCourseDetailsDao.stampMissingFromImport(newStaleScjCodes, DateTime.now)

		logger.warn("Timestamping missing studentCourseYearDetails")
		studentCourseYearDetailsDao.stampMissingFromImport(newStaleScydIds, DateTime.now)
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
