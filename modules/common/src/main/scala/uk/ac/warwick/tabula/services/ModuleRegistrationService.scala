package uk.ac.warwick.tabula.services

import org.springframework.stereotype.Service
import uk.ac.warwick.spring.Wire
import uk.ac.warwick.tabula.AcademicYear
import uk.ac.warwick.tabula.commands.exams.grids.GenerateExamGridEntity
import uk.ac.warwick.tabula.data.model._
import uk.ac.warwick.tabula.data.{AutowiringModuleRegistrationDaoComponent, ModuleRegistrationDaoComponent}

import scala.math.BigDecimal.RoundingMode

trait ModuleRegistrationService {

	def saveOrUpdate(moduleRegistration: ModuleRegistration): Unit

	def getByNotionalKey(
		studentCourseDetails: StudentCourseDetails,
		module: Module,
		cats: java.math.BigDecimal,
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
	def weightedMeanYearMark(moduleRegistrations: Seq[ModuleRegistration]): Option[BigDecimal]

	def overcattedModuleSubsets(entity: GenerateExamGridEntity): Seq[(BigDecimal, Seq[ModuleRegistration])]

}

abstract class AbstractModuleRegistrationService extends ModuleRegistrationService {

	self: ModuleRegistrationDaoComponent =>

	def saveOrUpdate(moduleRegistration: ModuleRegistration) = moduleRegistrationDao.saveOrUpdate(moduleRegistration)

	def getByNotionalKey(
		studentCourseDetails: StudentCourseDetails,
		module: Module,
		cats: java.math.BigDecimal,
		academicYear: AcademicYear,
		occurrence: String
	): Option[ModuleRegistration] =
		moduleRegistrationDao.getByNotionalKey(studentCourseDetails, module, cats, academicYear, occurrence)

	def getByUsercodesAndYear(usercodes: Seq[String], academicYear: AcademicYear): Seq[ModuleRegistration] =
		moduleRegistrationDao.getByUsercodesAndYear(usercodes, academicYear)

	def weightedMeanYearMark(moduleRegistrations: Seq[ModuleRegistration]): Option[BigDecimal] = {
		if (moduleRegistrations.nonEmpty && moduleRegistrations.forall(_.agreedMark != null)) {
			Some(
				(moduleRegistrations.map(mr => BigDecimal(mr.agreedMark) * BigDecimal(mr.cats)).sum / moduleRegistrations.map(mr => BigDecimal(mr.cats)).sum)
					.setScale(1, RoundingMode.HALF_UP)
			)
		} else {
			None
		}
	}

	def overcattedModuleSubsets(entity: GenerateExamGridEntity): Seq[(BigDecimal, Seq[ModuleRegistration])] = {
		val coreAndCoreReqModules = entity.moduleRegistrations.filter(mr =>
			mr.selectionStatus == ModuleSelectionStatus.Core || mr.selectionStatus == ModuleSelectionStatus.CoreRequired
		)
		val subsets = entity.moduleRegistrations.toSet.subsets.toSeq
		val validSubsets = subsets.filter(_.nonEmpty).filter(modRegs =>
			// CATS total of at least the normal load
			modRegs.toSeq.map(mr => BigDecimal(mr.cats)).sum >= entity.normalCATLoad &&
			// Contains all the core and core required modules
			coreAndCoreReqModules.forall(modRegs.contains) &&
			// All the registrations have agreed marks
			modRegs.forall(_.agreedMark != null)
		)
		validSubsets.map(modRegs => (weightedMeanYearMark(modRegs.toSeq).get, modRegs.toSeq.sortBy(_.module.code))).sortBy(_._1).reverse
	}

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
