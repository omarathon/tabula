package uk.ac.warwick.tabula.scheduling.commands.imports

import org.joda.time.DateTime
import org.springframework.beans.BeanWrapperImpl
import uk.ac.warwick.spring.Wire
import uk.ac.warwick.tabula.commands.{Description, Command, Unaudited}
import uk.ac.warwick.tabula.data.{AccreditedPriorLearningDao, StudentCourseDetailsDao}
import uk.ac.warwick.tabula.data.Transactions.transactional
import uk.ac.warwick.tabula.data.model.{Award, AccreditedPriorLearning, StudentCourseDetails, Level}
import uk.ac.warwick.tabula.helpers.Logging
import uk.ac.warwick.tabula.permissions.Permissions
import uk.ac.warwick.tabula.scheduling.helpers.PropertyCopying
import uk.ac.warwick.tabula.scheduling.services.{AutowiringLevelImporterComponent, AutowiringAwardImporterComponent, AccreditedPriorLearningRow}
import org.springframework.beans.BeanWrapper
import uk.ac.warwick.tabula.AcademicYear
import uk.ac.warwick.tabula.services.{AutowiringLevelServiceComponent, AutowiringAwardServiceComponent, LevelServiceComponent, AccreditedPriorLearningServiceComponent, AwardServiceComponent}

class ImportAccreditedPriorLearningCommand(accreditedPriorLearningRow: AccreditedPriorLearningRow)
	extends Command[Option[AccreditedPriorLearning]]
	with Logging with Unaudited with PropertyCopying
	with AutowiringAwardImporterComponent
	with AutowiringLevelImporterComponent {

	PermissionCheck(Permissions.ImportSystemData)

	var accreditedPriorLearningDao = Wire[AccreditedPriorLearningDao]
	var studentCourseDetailsDao = Wire[StudentCourseDetailsDao]

	val scjCode = accreditedPriorLearningRow.scjCode
	val awardCode = accreditedPriorLearningRow.awardCode
	val sequenceNumber = accreditedPriorLearningRow.sequenceNumber
	val academicYear = AcademicYear.parse(accreditedPriorLearningRow.academicYear)
	val cats: java.math.BigDecimal = accreditedPriorLearningRow.cats
	val levelCode = accreditedPriorLearningRow.levelCode
	val reason = accreditedPriorLearningRow.reason

	override def applyInternal(): Option[AccreditedPriorLearning] = transactional() ({
		logger.debug("Importing accredited prior learning for student " + scjCode + ", award " + awardCode)

		studentCourseDetailsDao.getByScjCode(scjCode) match {
			case None =>
				logger.warn("Can't record accredited prior learning - could not find a StudentCourseDetails for " + scjCode)
				None
			case Some(scd: StudentCourseDetails) => {
				awardImporter.getAwardByCodeCached(awardCode) match {
					case None =>
						logger.warn("Can't record accredited prior learning - could not find award for award code " + awardCode)
						None
					case Some(award: Award) => {
						val accreditedPriorLearningExisting: Option[AccreditedPriorLearning] = accreditedPriorLearningDao.getByNotionalKey(scd, award, sequenceNumber)
						val isTransient = !accreditedPriorLearningExisting.isDefined

						levelImporter.getLevelByCodeCached(levelCode) match {
							case None =>
								logger.warn ("Can't record accredited prior learning - couldn't find level for level code " + levelCode)
								None
							case Some(level: Level) => {

								val accreditedPriorLearning = accreditedPriorLearningExisting match {
									case Some(accreditedPriorLearning: AccreditedPriorLearning) => accreditedPriorLearning
									case _ => new AccreditedPriorLearning(scd, award, sequenceNumber, academicYear, cats, level, reason)
								}

								val commandBean = new BeanWrapperImpl(this)
								val accreditedPriorLearningBean = new BeanWrapperImpl(accreditedPriorLearning)

								val hasChanged = copyBasicProperties(properties, commandBean, accreditedPriorLearningBean)

								if (isTransient || hasChanged) {
									logger.debug("Saving changes for " + accreditedPriorLearning)

									accreditedPriorLearning.lastUpdatedDate = DateTime.now
									accreditedPriorLearningDao.saveOrUpdate(accreditedPriorLearning)
								}

								Some(accreditedPriorLearning)
							}
						}
					}
				}
			}
		}
	})

	private val properties = Set(
		"academicYear", "cats", "level", "reason"
	)

	override def describe(d: Description) = d.properties("scjCode" -> scjCode)

}
