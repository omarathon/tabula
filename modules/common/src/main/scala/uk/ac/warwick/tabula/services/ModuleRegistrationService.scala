package uk.ac.warwick.tabula.services

import org.springframework.stereotype.Service
import uk.ac.warwick.spring.Wire
import uk.ac.warwick.tabula.AcademicYear
import uk.ac.warwick.tabula.commands.exams.grids.GenerateExamGridEntity
import uk.ac.warwick.tabula.data.model._
import uk.ac.warwick.tabula.data.{AutowiringModuleRegistrationDaoComponent, ModuleRegistrationDaoComponent}
import uk.ac.warwick.tabula.JavaImports.JBigDecimal
import scala.math.BigDecimal.RoundingMode

object ModuleRegistrationService {
	final val DefaultNormalLoad = 120
}

trait ModuleRegistrationService {

	def saveOrUpdate(moduleRegistration: ModuleRegistration): Unit
	def saveOrUpdate(coreRequiredModule: CoreRequiredModule): Unit
	def delete(coreRequiredModule: CoreRequiredModule): Unit

	def getByNotionalKey(
		studentCourseDetails: StudentCourseDetails,
		module: Module,
		cats: JBigDecimal,
		academicYear: AcademicYear,
		occurrence: String
	): Option[ModuleRegistration]

	def getByUsercodesAndYear(usercodes: Seq[String], academicYear: AcademicYear) : Seq[ModuleRegistration]

	/**
		* Gets the weighted mean mark for the given module registrations.
		* Each agreed mark is multiplied by the CAT weighing of the module then added together, and the result is divided by the total CATS.
		* This is then rounded to 1 decimal place.
		* @param moduleRegistrations The module registrations to use
		* @return The weighted mean mark, if all the provided registration has an agreed mark
		*/
	def weightedMeanYearMark(moduleRegistrations: Seq[ModuleRegistration], markOverrides: Map[Module, BigDecimal]): Option[BigDecimal]

	def overcattedModuleSubsets(entity: GenerateExamGridEntity, markOverrides: Map[Module, BigDecimal], normalLoad: Int): Seq[(BigDecimal, Seq[ModuleRegistration])]

	def findCoreRequiredModules(route: Route, academicYear: AcademicYear, yearOfStudy: Int): Seq[CoreRequiredModule]

}

abstract class AbstractModuleRegistrationService extends ModuleRegistrationService {

	self: ModuleRegistrationDaoComponent =>

	def saveOrUpdate(moduleRegistration: ModuleRegistration) = moduleRegistrationDao.saveOrUpdate(moduleRegistration)

	def saveOrUpdate(coreRequiredModule: CoreRequiredModule) = moduleRegistrationDao.saveOrUpdate(coreRequiredModule)

	def delete(coreRequiredModule: CoreRequiredModule): Unit = moduleRegistrationDao.delete(coreRequiredModule)

	def getByNotionalKey(
		studentCourseDetails: StudentCourseDetails,
		module: Module,
		cats: JBigDecimal,
		academicYear: AcademicYear,
		occurrence: String
	): Option[ModuleRegistration] =
		moduleRegistrationDao.getByNotionalKey(studentCourseDetails, module, cats, academicYear, occurrence)

	def getByUsercodesAndYear(usercodes: Seq[String], academicYear: AcademicYear): Seq[ModuleRegistration] =
		moduleRegistrationDao.getByUsercodesAndYear(usercodes, academicYear)

	def weightedMeanYearMark(moduleRegistrations: Seq[ModuleRegistration], markOverrides: Map[Module, BigDecimal]): Option[BigDecimal] = {
		val nonNullReplacedMarksAndCats: Seq[(BigDecimal, BigDecimal)] = moduleRegistrations.map(mr => {
			val mark: BigDecimal = markOverrides.getOrElse(mr.module, mr.firstDefinedMark.map(mark => BigDecimal(mark)).orNull)
			val cats: BigDecimal = Option(mr.cats).map(c => BigDecimal(c)).orNull
			(mark, cats)
		}).filter{case(mark, cats) => mark != null & cats != null}
		if (nonNullReplacedMarksAndCats.nonEmpty && nonNullReplacedMarksAndCats.size == moduleRegistrations.size) {
			Some(
				(nonNullReplacedMarksAndCats.map{case(mark, cats) => mark * cats}.sum / nonNullReplacedMarksAndCats.map{case(_, cats) => cats}.sum)
					.setScale(1, RoundingMode.HALF_UP)
			)
		} else {
			None
		}
	}

	def overcattedModuleSubsets(entity: GenerateExamGridEntity, markOverrides: Map[Module, BigDecimal], normalLoad: Int): Seq[(BigDecimal, Seq[ModuleRegistration])] = {
		if (entity.moduleRegistrations.exists(_.firstDefinedMark.isEmpty)) {
			Seq()
		} else {
			val coreAndOptionalCoreModules = entity.moduleRegistrations.filter(mr =>
				mr.selectionStatus == ModuleSelectionStatus.Core || mr.selectionStatus == ModuleSelectionStatus.OptionalCore
			)
			val subsets = entity.moduleRegistrations.toSet.subsets.toSeq
			val validSubsets = subsets.filter(_.nonEmpty).filter(modRegs =>
				// CATS total of at least the normal load
				modRegs.toSeq.map(mr => BigDecimal(mr.cats)).sum >= normalLoad &&
					// Contains all the core and optional core modules
					coreAndOptionalCoreModules.forall(modRegs.contains) &&
					// All the registrations have agreed or actual marks
					modRegs.forall(mr => mr.firstDefinedMark.isDefined || markOverrides.get(mr.module).isDefined && markOverrides(mr.module) != null)
			)
			validSubsets.map(modRegs => (weightedMeanYearMark(modRegs.toSeq, markOverrides).get, modRegs.toSeq.sortBy(_.module.code))).sortBy(_._1).reverse
		}
	}

	def findCoreRequiredModules(route: Route, academicYear: AcademicYear, yearOfStudy: Int): Seq[CoreRequiredModule] =
		moduleRegistrationDao.findCoreRequiredModules(route, academicYear, yearOfStudy)

}

@Service("moduleRegistrationService")
class ModuleRegistrationServiceImpl
	extends AbstractModuleRegistrationService
	with AutowiringModuleRegistrationDaoComponent

trait ModuleRegistrationServiceComponent {
	def moduleRegistrationService: ModuleRegistrationService
}

trait AutowiringModuleRegistrationServiceComponent extends ModuleRegistrationServiceComponent {
	var moduleRegistrationService = Wire[ModuleRegistrationService]
}
