package uk.ac.warwick.tabula.services

import org.springframework.stereotype.Service
import uk.ac.warwick.spring.Wire
import uk.ac.warwick.tabula.data.model.{CourseYearWeighting, ModuleRegistration, StudentCourseYearDetails}
import uk.ac.warwick.tabula.data.{AutowiringCourseDaoComponent, CourseDaoComponent}

import scala.math.BigDecimal.RoundingMode

sealed abstract class ProgressionResult(val description: String)

object ProgressionResult {
	case object Proceed extends ProgressionResult("Proceed")
	case object PossiblyProceed extends ProgressionResult("Proceed(?)")
	case object Resit extends ProgressionResult("Resit")
	case object Pass extends ProgressionResult("Pass")
	case class Unknown(details: String) extends ProgressionResult("?")
}

sealed abstract class FinalYearGrade(val description: String, val lowerBound: BigDecimal, val upperBound: BigDecimal) {
	def applies(mark: BigDecimal): Boolean = {
		mark <= upperBound && mark >= lowerBound
	}
}

object FinalYearGrade {
	case object First extends FinalYearGrade(
		"1",
		BigDecimal(70.0).setScale(1, RoundingMode.HALF_UP),
		BigDecimal(200.0).setScale(1, RoundingMode.HALF_UP)
	)
	case object UpperSecond extends FinalYearGrade(
		"2.1",
		BigDecimal(60.0).setScale(1, RoundingMode.HALF_UP),
		BigDecimal(69.9).setScale(1, RoundingMode.HALF_UP)
	)
	case object LowerSecond extends FinalYearGrade(
		"2.2",
		BigDecimal(50.0).setScale(1, RoundingMode.HALF_UP),
		BigDecimal(59.9).setScale(1, RoundingMode.HALF_UP)
	)
	case object Third extends FinalYearGrade(
		"3",
		BigDecimal(40.0).setScale(1, RoundingMode.HALF_UP),
		BigDecimal(49.9).setScale(1, RoundingMode.HALF_UP)
	)
	case object Pass extends FinalYearGrade(
		"Pass",
		BigDecimal(35.0).setScale(1, RoundingMode.HALF_UP),
		BigDecimal(39.9).setScale(1, RoundingMode.HALF_UP)
	)
	case object Fail extends FinalYearGrade(
		"Fail",
		BigDecimal(-100).setScale(1, RoundingMode.HALF_UP),
		BigDecimal(34.9).setScale(1, RoundingMode.HALF_UP)
	)
	case class Unknown(details: String) extends FinalYearGrade("?", null, null)
	case object Ignore extends FinalYearGrade("-", null, null)
	private val all = Seq(First, UpperSecond, LowerSecond, Third, Pass, Fail)
	def fromMark(mark: BigDecimal): FinalYearGrade = all.find(_.applies(mark)).getOrElse(Unknown(s"Could not find matching grade for mark ${mark.toString}"))
}

object ProgressionService {
	final val ModulePassMark = 40
	final val FirstYearPassMark = 40
	final val FirstYearRequiredCredits = 80
	final val IntermediateYearPassMark = 40
	final val IntermediateRequiredCredits = 60
	final val FinalTwoYearsRequiredCredits = 168
	final val FinalYearRequiredCredits = 80
}

trait ProgressionService {

	def suggestedResult(scyd: StudentCourseYearDetails, normalLoad: BigDecimal): ProgressionResult
	def suggestedFinalYearGrade(scyd: StudentCourseYearDetails, normalLoad: BigDecimal): FinalYearGrade
}

abstract class AbstractProgressionService extends ProgressionService {

	self: ModuleRegistrationServiceComponent with CourseDaoComponent =>

	def suggestedResult(scyd: StudentCourseYearDetails, normalLoad: BigDecimal): ProgressionResult = {
		if (scyd.moduleRegistrations.exists(_.firstDefinedMark.isEmpty)) {
			ProgressionResult.Unknown(s"No agreed mark or actual mark for modules: ${scyd.moduleRegistrations.filter(_.firstDefinedMark.isEmpty).map(_.module.code.toUpperCase).mkString(", ")}")
		} else if (scyd.moduleRegistrations.isEmpty) {
				ProgressionResult.Unknown(s"No module registrations found for ${scyd.studentCourseDetails.scjCode} ${scyd.academicYear.toString}")
		} else if (scyd.yearOfStudy.toInt == 1) {
			suggestedResultFirstYear(scyd, normalLoad)
		} else if (scyd.isFinalYear) {
			val sfyg = suggestedFinalYearGrade(scyd, normalLoad)
			if (sfyg == FinalYearGrade.Fail) {
				ProgressionResult.Resit
			} else {
				sfyg match {
					case unknown: FinalYearGrade.Unknown => ProgressionResult.Unknown(unknown.details)
					case _ => ProgressionResult.Pass
				}
			}
		} else {
			suggestedResultIntermediateYear(scyd, normalLoad)
		}

	}

	/**
		* Regulation defined at: http://www2.warwick.ac.uk/services/aro/dar/quality/categories/examinations/conventions/fyboe
		*/
	private def suggestedResultFirstYear(scyd: StudentCourseYearDetails, normalLoad: BigDecimal): ProgressionResult = {
		val coreRequiredModules = moduleRegistrationService.findCoreRequiredModules(scyd.studentCourseDetails.currentRoute, scyd.academicYear, scyd.yearOfStudy)
		val passedModuleRegistrations = scyd.moduleRegistrations.filter(mr => BigDecimal(mr.firstDefinedMark.get) >= ProgressionService.ModulePassMark)
		val passedCredits = passedModuleRegistrations.map(mr => BigDecimal(mr.cats)).sum > ProgressionService.FirstYearRequiredCredits
		val passedCoreRequired = coreRequiredModules.forall(cr => passedModuleRegistrations.exists(_.module == cr.module))
		val overallMarkSatisfied = getYearMark(scyd, normalLoad).map(mark => mark >= ProgressionService.FirstYearPassMark)

		if (overallMarkSatisfied.isEmpty) {
			ProgressionResult.Unknown("Over Catted Mark not yet chosen")
		} else {
			if (passedCredits && passedCoreRequired && overallMarkSatisfied.get) {
				ProgressionResult.Proceed
			} else if (passedCoreRequired && overallMarkSatisfied.get) {
				ProgressionResult.PossiblyProceed
			} else {
				ProgressionResult.Resit
			}
		}
	}

	/**
		* Regulation defined at: http://www2.warwick.ac.uk/services/aro/dar/quality/categories/examinations/conventions/ugprogression09/
		*/
	private def suggestedResultIntermediateYear(scyd: StudentCourseYearDetails, normalLoad: BigDecimal): ProgressionResult = {
		val passedModuleRegistrations = scyd.moduleRegistrations.filter(mr => BigDecimal(mr.firstDefinedMark.get) >= ProgressionService.ModulePassMark)
		val passedCredits = passedModuleRegistrations.map(mr => BigDecimal(mr.cats)).sum > ProgressionService.IntermediateRequiredCredits
		val overallMarkSatisfied = getYearMark(scyd, normalLoad).map(mark => mark >= ProgressionService.IntermediateYearPassMark)

		if (overallMarkSatisfied.isEmpty) {
			ProgressionResult.Unknown("Over Catted Mark not yet chosen")
		} else {
			if (passedCredits && overallMarkSatisfied.get) {
				ProgressionResult.Proceed
			} else {
				ProgressionResult.Resit
			}
		}
	}

	/**
		* Regulation defined at: http://www2.warwick.ac.uk/services/aro/dar/quality/categories/examinations/conventions/ug13
		*/
	def suggestedFinalYearGrade(scyd: StudentCourseYearDetails, normalLoad: BigDecimal): FinalYearGrade = {
		if (scyd.isFinalYear) {
			val finalYearOfStudy = scyd.yearOfStudy.toInt
			val scydsFromThisAndOlderCourses: Seq[StudentCourseYearDetails] = {
				val scds = scyd.studentCourseDetails.student.freshStudentCourseDetails.sorted.takeWhile(_.scjCode != scyd.studentCourseDetails.scjCode) ++ Seq(scyd.studentCourseDetails)
				scds.flatMap(_.freshStudentCourseYearDetails)
			}
			val scydPerYear: Seq[(Int, Option[StudentCourseYearDetails])] = (1 to finalYearOfStudy).map(year => {
				if (year != finalYearOfStudy) {
					val scydsForThisYear = scydsFromThisAndOlderCourses.filter(scyd => scyd.yearOfStudy.toInt == year)
					val latestSCYDForThisYear = scydsForThisYear.lastOption // SCDs and SCYDs are sorted collections
					(year, latestSCYDForThisYear)
				} else {
					(year, Option(scyd))
				}
			})
			lazy val markPerYear: Seq[(Int, Option[BigDecimal])] = getMarkPerYear(scyd, scydPerYear, finalYearOfStudy, normalLoad)
			lazy val yearWeightings: Seq[(Int, Option[CourseYearWeighting])] = markPerYear.map { case (year, _) =>
				(year, courseDao.getCourseYearWeighting(scyd.studentCourseDetails.course.code, scyd.academicYear, year))
			}
			if (markPerYear.exists(_._2.isEmpty)) {
				FinalYearGrade.Unknown(s"Could not find agreed mark for years: ${markPerYear.filter(_._2.isEmpty).map(_._1).mkString(", ")}")
			} else if (yearWeightings.exists(_._2.isEmpty)) {
				FinalYearGrade.Unknown("Could not find year weightings for: %s".format(
					yearWeightings.filter(_._2.isEmpty).map(_._1).map(year => s"${scyd.studentCourseDetails.course.code.toUpperCase} ${scyd.academicYear.toString} Year $year").mkString(", ")
				))
			} else {
				weightedFinalYearGrade(
					scyd,
					scydPerYear.map{case (year, option) => (year, option.get)},
					markPerYear.map{case (year, option) => (year, option.get)},
					yearWeightings.map{case (year, option) => (year, option.get)}
				)
			}
		} else {
			FinalYearGrade.Ignore
		}
	}

	private def getMarkPerYear(
		scyd: StudentCourseYearDetails,
		scydPerYear: Seq[(Int, Option[StudentCourseYearDetails])],
		finalYearOfStudy: Int,
		normalLoad: BigDecimal
	): Seq[(Int, Option[BigDecimal])] = {
		scydPerYear.map{ case (year, scydOption) =>
			(year, scydOption.flatMap(thisScyd => {
				if (year != finalYearOfStudy) {
					Option(thisScyd.agreedMark).map(mark => BigDecimal(mark))
				} else {
					getYearMark(scyd, normalLoad)
				}
			}))
		}
	}

	private def getYearMark(scyd: StudentCourseYearDetails, normalLoad: BigDecimal): Option[BigDecimal] = {
		val weightedMeanMark = moduleRegistrationService.weightedMeanYearMark(scyd.moduleRegistrations, Map())
		val cats = scyd.moduleRegistrations.map(mr => BigDecimal(mr.cats)).sum
		if (cats > normalLoad) {
			if (moduleRegistrationService.overcattedModuleSubsets(scyd.toGenerateExamGridEntity(None), Map(), normalLoad).size <= 1) {
				// If the student has overcatted, but there's only one valid subset, just choose the mean mark
				weightedMeanMark
			} else if (scyd.overcattingModules.isDefined) {
				// If the student has overcatted and a subset of modules has been chosen for the overcatted mark,
				// calculate the overcatted mark from that subset
				val overcatMark = moduleRegistrationService.weightedMeanYearMark(scyd.moduleRegistrations.filter(mr => scyd.overcattingModules.get.contains(mr.module)), Map())
				// Providing they're both defined, return the larger
				if (weightedMeanMark.isDefined && overcatMark.isDefined) {
					Option(Seq(weightedMeanMark.get, overcatMark.get).max)
				} else {
					None
				}
			} else {
				None
			}
		} else {
			weightedMeanMark
		}
	}

	private def weightedFinalYearGrade(
		scyd: StudentCourseYearDetails,
		scydPerYear: Seq[(Int, StudentCourseYearDetails)],
		markPerYear: Seq[(Int, BigDecimal)],
		yearWeightings: Seq[(Int, CourseYearWeighting)]
	): FinalYearGrade = {
		val finalTwoYearsModuleRegistrations = scydPerYear.reverse.take(2).flatMap(_._2.moduleRegistrations)

		if (finalTwoYearsModuleRegistrations.exists(_.firstDefinedMark.isEmpty)) {
			FinalYearGrade.Unknown(s"No agreed mark or actual mark for modules: ${
				finalTwoYearsModuleRegistrations.filter(_.firstDefinedMark.isEmpty).map(mr => "%s %s".format(mr.module.code.toUpperCase, mr.academicYear.toString)).mkString(", ")
			}")
		} else {

			val passedModuleRegistrationsInFinalTwoYears: Seq[ModuleRegistration] = finalTwoYearsModuleRegistrations
				.filter(mr => BigDecimal(mr.firstDefinedMark.get) >= ProgressionService.ModulePassMark)
			val passedCreditsInFinalTwoYears = passedModuleRegistrationsInFinalTwoYears.map(mr => BigDecimal(mr.cats)).sum > ProgressionService.FinalTwoYearsRequiredCredits

			val passedModuleRegistrationsFinalYear: Seq[ModuleRegistration] = scydPerYear.reverse.head._2
				.moduleRegistrations.filter(mr => BigDecimal(mr.firstDefinedMark.get) >= ProgressionService.ModulePassMark)
			val passedCreditsFinalYear = passedModuleRegistrationsFinalYear.map(mr => BigDecimal(mr.cats)).sum > ProgressionService.FinalYearRequiredCredits

			if (passedCreditsInFinalTwoYears && passedCreditsFinalYear) {
				val finalMark: BigDecimal = markPerYear.map(_._1).map(year =>
					markPerYear.toMap.apply(year) * yearWeightings.toMap.apply(year).weighting
				).sum.setScale(1, RoundingMode.HALF_UP)
				FinalYearGrade.fromMark(finalMark)
			} else {
				FinalYearGrade.Fail
			}

		}
	}

}

@Service("progressionService")
class ProgressionServiceImpl
	extends AbstractProgressionService
	with AutowiringModuleRegistrationServiceComponent
	with AutowiringCourseDaoComponent

trait ProgressionServiceComponent {
	def progressionService: ProgressionService
}

trait AutowiringProgressionServiceComponent extends ProgressionServiceComponent {
	var progressionService = Wire[ProgressionService]
}
