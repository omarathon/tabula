package uk.ac.warwick.tabula.services

import org.springframework.stereotype.Service
import uk.ac.warwick.spring.Wire
import uk.ac.warwick.tabula.commands.exams.grids.ExamGridEntityYear
import uk.ac.warwick.tabula.data.model.DegreeType.{Postgraduate, Undergraduate}
import uk.ac.warwick.tabula.data.model._

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
	def this(description: String, min: Double, max: Double) {
		this(description, FinalYearGrade.toBigDecimal(min), FinalYearGrade.toBigDecimal(max))
	}
	def fail: Boolean = description == "Fail"

	def applies(mark: BigDecimal): Boolean = {
		mark <= upperBound && mark >= lowerBound
	}
	def withMark(mark: BigDecimal): FinalYearMark = FinalYearMark(mark, description, lowerBound, upperBound)

	override def equals(other: Any): Boolean = other match {
		case other: FinalYearGrade => description == other.description
		case _ => false
	}
}

case class FinalYearMark(mark: BigDecimal, override val description: String, override val lowerBound: BigDecimal, override val upperBound: BigDecimal)
	extends FinalYearGrade(description, lowerBound, upperBound)

object FinalYearGrade {
	def toBigDecimal(d: Double): BigDecimal = BigDecimal(d).setScale(1, RoundingMode.HALF_UP)

	object Undergraduate {
		val all = Seq(
			First, FirstBorderline, UpperSecond, UpperSecondBorderline, LowerSecond, LowerSecondBorderline,
			Third, ThirdBorderline, Pass, PassBorderline, Fail
		)

		case object First extends FinalYearGrade("1", min = 71, max = 200)
		case object FirstBorderline extends FinalYearGrade("1 (b)", min = 70, max = 70.9)
		case object UpperSecond extends FinalYearGrade("2.1", min = 60, max = 67.9)
		case object UpperSecondBorderline extends FinalYearGrade("2.1 (b)", min = 68, max = 69.9)
		case object LowerSecond extends FinalYearGrade("2.2", min = 50, max = 57.9)
		case object LowerSecondBorderline extends FinalYearGrade("2.2 (b)", min = 58, max = 59.9)
		case object Third extends FinalYearGrade("3", min = 40, max = 47.9)
		case object ThirdBorderline extends FinalYearGrade("3 (b)", min = 48, max = 49.9)
		case object Pass extends FinalYearGrade("Pass", min = 35, max = 37.9)
		case object PassBorderline extends FinalYearGrade("Pass (b)", min = 38, max = 39.9)
		case object Fail extends FinalYearGrade("Fail", min = -100, max = 34.9)
	}

	object Postgraduate {
		val all = Seq(HighDistinction, Distinction, Merit, Pass, Fail)

		case object HighDistinction extends FinalYearGrade("High Distinction", min = 80, max = 200)
		case object Distinction extends FinalYearGrade("Distinction", min = 70, max = 79.9)
		case object Merit extends FinalYearGrade("Merit", min = 60, max = 69.9)
		case object Pass extends FinalYearGrade("Pass", min = 50, max = 59.9)
		case object Fail extends FinalYearGrade("Fail", min = -100, max = 49.9)
	}

	case class Unknown(details: String) extends FinalYearGrade("?", null, null)
	case object Ignore extends FinalYearGrade("-", null, null)

	def fail(mark: BigDecimal, courseType: CourseType): FinalYearGrade = {
		if (courseType == CourseType.UG)
			Undergraduate.Fail.withMark(mark)
		else
			Postgraduate.Fail.withMark(mark)
	}

	def fromMark(mark: BigDecimal, courseType: CourseType): FinalYearGrade = {
		val grades = if (courseType == CourseType.UG) Undergraduate.all else Postgraduate.all

		grades.find(_.applies(mark)).map(_.withMark(mark))
			.getOrElse(Unknown(s"Could not find matching grade for mark ${mark.toString}"))
	}
}

object ProgressionService {

	def modulePassMark(degreeType: DegreeType): Int = degreeType match {
		case Undergraduate => UndergradPassMark
		case Postgraduate => PostgraduatePassMark
		case _ => DefaultPassMark
	}

	def allowEmptyYearMarks(yearWeightings: Seq[CourseYearWeighting], entityYear: ExamGridEntityYear): Boolean = {
		// codes are same that cognos used -TAB-6397
		val yearAbroadMoaCode =  List("YO","SW","YOE","SWE","YM","YME","YV")
		lazy val yearAbroad = entityYear.studentCourseYearDetails match {
			case Some(scyd) => yearAbroadMoaCode.contains(scyd.modeOfAttendance.code) && (scyd.blockOccurrence ==  null || scyd.blockOccurrence !=  "I") // doesn't apply to intercalated years
			case _ => false
		}
		yearWeightings.exists( w => w.yearOfStudy == entityYear.yearOfStudy && w.weighting == 0) || yearAbroad
	}

	final val DefaultPassMark = 40
	final val UndergradPassMark = 40
	final val PostgraduatePassMark = 50

	final val FirstYearPassMark = 40
	final val FirstYearRequiredCredits = 80
	final val IntermediateYearPassMark = 40
	final val IntermediateRequiredCredits = 60
	final val FinalTwoYearsRequiredCredits = 168
	final val FinalYearRequiredCredits = 80
}

trait ProgressionService {
	def getYearMark(entityYear: ExamGridEntityYear, normalLoad: BigDecimal, routeRules: Seq[UpstreamRouteRule], yearWeightings: Seq[CourseYearWeighting]): Either[String, BigDecimal]
	def suggestedResult(entityYear: ExamGridEntityYear, normalLoad: BigDecimal, routeRulesPerYear: Map[Int, Seq[UpstreamRouteRule]], calculateYearMarks: Boolean, groupByLevel: Boolean, yearWeightings: Seq[CourseYearWeighting]): ProgressionResult
	def suggestedFinalYearGrade(entityYear: ExamGridEntityYear, normalLoad: BigDecimal, routeRulesPerYear: Map[Int, Seq[UpstreamRouteRule]], calculateYearMarks: Boolean, groupByLevel: Boolean, yearWeightings: Seq[CourseYearWeighting]): FinalYearGrade
}

abstract class AbstractProgressionService extends ProgressionService {

	self: ModuleRegistrationServiceComponent with CourseAndRouteServiceComponent =>

	def getYearMark(entityYear: ExamGridEntityYear, normalLoad: BigDecimal, routeRules: Seq[UpstreamRouteRule], yearWeightings: Seq[CourseYearWeighting]): Either[String, BigDecimal] = {
		/**TODO (TAB-6397)- We need to check MOA categories similar to what cognos does currently which will resolve issue for different years abroad for the same course. If those specific categories, then allowEmpty should be set as true.
			* Will need same checking at other places too. Currently, for those  courses  year weightings are set as non zero for one of them (2nd or 3rd year) by modern language making it unable to calculate final year overall marks
			* even though they are abroad. A further validation  will be required to ensure weighted %age is 100 when we calculate final overall marks.
			*/
		val possibleWeightedMeanMark = moduleRegistrationService.weightedMeanYearMark(entityYear.moduleRegistrations, Map(), allowEmpty = ProgressionService.allowEmptyYearMarks(yearWeightings, entityYear))
		  	.left.map(msg => s"$msg for year ${entityYear.yearOfStudy}")

		val overcatSubsets = moduleRegistrationService.overcattedModuleSubsets(entityYear, Map(), normalLoad, routeRules)
		if (overcatSubsets.size <= 1) {
			// If the there's only one valid subset, just choose the mean mark
			possibleWeightedMeanMark
		} else if (entityYear.studentCourseYearDetails.flatMap(_.overcattingModules).isDefined) {
			// If the student has overcatted and a subset of modules has been chosen for the overcatted mark,
			// find the subset that matches those modules, and show that mark if found
			overcatSubsets.find { case (_, subset) => subset.size == entityYear.overcattingModules.get.size && subset.map(_.module).forall(entityYear.overcattingModules.get.contains) }
				.map { case (overcatMark, _) => possibleWeightedMeanMark match {
					case Right(mark) => Right(Seq(mark, overcatMark).max)
					case Left(message) => Left(message)
				}
				}.getOrElse(Left("Could not find valid module registration subset matching chosen subset"))
		} else {
			Left("The overcat adjusted mark subset has not been chosen")
		}
	}

	def suggestedResult(entityYear: ExamGridEntityYear, normalLoad: BigDecimal, routeRulesPerYear: Map[Int, Seq[UpstreamRouteRule]], calculateYearMarks: Boolean, groupByLevel: Boolean, yearWeightings: Seq[CourseYearWeighting]): ProgressionResult = {
		entityYear.studentCourseYearDetails.map(scyd => {
			val emptyExpectingMarks = entityYear.moduleRegistrations.filter(mr => !mr.passFail && mr.firstDefinedMark.isEmpty)
			val emptyExpectingGrades = entityYear.moduleRegistrations.filter(mr => mr.passFail && mr.firstDefinedGrade.isEmpty)

			if (emptyExpectingMarks.nonEmpty) {
				ProgressionResult.Unknown(s"No agreed mark or actual mark for modules: ${emptyExpectingMarks.map(_.module.code.toUpperCase).mkString(", ")}")
			} else if (emptyExpectingGrades.nonEmpty) {
				ProgressionResult.Unknown(s"No agreed grade or actual grade for modules: ${emptyExpectingGrades.map(_.module.code.toUpperCase).mkString(", ")}")
			} else if (entityYear.moduleRegistrations.isEmpty) {
				ProgressionResult.Unknown(s"No module registrations found for ${scyd.studentCourseDetails.scjCode} ${scyd.academicYear.toString}")
			} else if (entityYear.yearOfStudy == 1) {
				suggestedResultFirstYear(entityYear, normalLoad, routeRulesPerYear.getOrElse(scyd.yearOfStudy, Seq()), yearWeightings)
			} else if (scyd.isFinalYear) {
				val sfyg = suggestedFinalYearGrade(entityYear, normalLoad, routeRulesPerYear, calculateYearMarks, groupByLevel, yearWeightings)

				sfyg match {
					case g if g.fail => ProgressionResult.Resit
					case unknown: FinalYearGrade.Unknown => ProgressionResult.Unknown(unknown.details)
					case _ => ProgressionResult.Pass
				}
			} else {
				suggestedResultIntermediateYear(entityYear, normalLoad, routeRulesPerYear.getOrElse(scyd.yearOfStudy, Seq()), yearWeightings)
			}
		}).getOrElse(ProgressionResult.Unknown(s"Missing year details for ${entityYear.level.map(_.code).getOrElse("an unknown level")}"))
	}

	// a definition of a passed module that handles pass-fail modules
	private def isPassed(mr: ModuleRegistration) = {
		if (mr.passFail) mr.firstDefinedGrade.contains("P")
		else BigDecimal(mr.firstDefinedMark.get) >= ProgressionService.modulePassMark(mr.module.degreeType)
	}

	/**
		* Regulation defined at: http://www2.warwick.ac.uk/services/aro/dar/quality/categories/examinations/conventions/fyboe
		*/
	private def suggestedResultFirstYear(entityYear: ExamGridEntityYear, normalLoad: BigDecimal, routeRules: Seq[UpstreamRouteRule], yearWeightings: Seq[CourseYearWeighting]): ProgressionResult = {
		entityYear.studentCourseYearDetails.map(scyd => {
			val coreRequiredModules = moduleRegistrationService.findCoreRequiredModules(
				scyd.studentCourseDetails.currentRoute,
				scyd.academicYear,
				scyd.yearOfStudy
			)

			val passedModuleRegistrations = entityYear.moduleRegistrations.filter(isPassed)
			val passedCredits = passedModuleRegistrations.map(mr => BigDecimal(mr.cats)).sum > ProgressionService.FirstYearRequiredCredits
			val passedCoreRequired = coreRequiredModules.forall(cr => passedModuleRegistrations.exists(_.module == cr.module))
			val overallMark = getYearMark(entityYear, normalLoad, routeRules, yearWeightings)

			if (overallMark.isLeft) {
				ProgressionResult.Unknown(overallMark.left.get)
			} else {
				val overallMarkSatisfied = overallMark.right.get >= ProgressionService.FirstYearPassMark
				if (passedCredits && passedCoreRequired && overallMarkSatisfied) {
					ProgressionResult.Proceed
				} else if (passedCoreRequired && overallMarkSatisfied) {
					ProgressionResult.PossiblyProceed
				} else {
					ProgressionResult.Resit
				}
			}
		}).getOrElse(ProgressionResult.Unknown(s"Missing year details for ${entityYear.level.map(_.code).getOrElse("an unknown level")}"))
	}

	/**
		* Regulation defined at: http://www2.warwick.ac.uk/services/aro/dar/quality/categories/examinations/conventions/ugprogression09/
		*/
	private def suggestedResultIntermediateYear(entityYear: ExamGridEntityYear, normalLoad: BigDecimal, routeRules: Seq[UpstreamRouteRule], yearWeightings: Seq[CourseYearWeighting]): ProgressionResult = {
		entityYear.studentCourseYearDetails.map(scyd => {
			val passedModuleRegistrations = entityYear.moduleRegistrations.filter(isPassed)
			val passedCredits = passedModuleRegistrations.map(mr => BigDecimal(mr.cats)).sum > ProgressionService.IntermediateRequiredCredits
			val overallMark = getYearMark(entityYear, normalLoad, routeRules, yearWeightings)

			if (overallMark.isLeft) {
				ProgressionResult.Unknown("Over Catted Mark not yet chosen")
			} else {
				val overallMarkSatisfied = overallMark.right.get >= ProgressionService.IntermediateYearPassMark
				if (passedCredits && overallMarkSatisfied) {
					ProgressionResult.Proceed
				} else {
					ProgressionResult.Resit
				}
			}
		}).getOrElse(ProgressionResult.Unknown(s"Missing year details for ${entityYear.level.map(_.code).getOrElse("an unknown level")}"))
	}

	/**
		* Regulation defined at: http://www2.warwick.ac.uk/services/aro/dar/quality/categories/examinations/conventions/ug13
		*/
	def suggestedFinalYearGrade(entityYear: ExamGridEntityYear, normalLoad: BigDecimal, routeRulesPerYear: Map[Int, Seq[UpstreamRouteRule]], calculateYearMarks: Boolean, groupByLevel: Boolean, weightings: Seq[CourseYearWeighting]): FinalYearGrade = {
		entityYear.studentCourseYearDetails.map(scyd => {
			val finalYearOfStudy = scyd.studentCourseDetails.courseYearLength.toInt
			if (entityYear.yearOfStudy >= finalYearOfStudy) {
				val entityPerYear: Map[Int, ExamGridEntityYear] = {
					val scds = scyd.studentCourseDetails.student.freshStudentCourseDetails.sorted.takeWhile(_.scjCode != scyd.studentCourseDetails.scjCode) ++ Seq(scyd.studentCourseDetails)
					val allScyds = scds.flatMap(_.freshStudentCourseYearDetails)

					if (groupByLevel) {
						allScyds.groupBy(_.level.orNull)
							.map{ case (level, scyds) => level.toYearOfStudy -> StudentCourseYearDetails.toExamGridEntityYearGrouped(level.toYearOfStudy, scyds:_ *)}
					} else {
						(1 to finalYearOfStudy).map(block => {
							val latestSCYDForThisYear = allScyds.filter(_.yearOfStudy.toInt == block).lastOption
							block -> latestSCYDForThisYear.map(_.toExamGridEntityYear).orNull
						}).toMap
					}
				}

				lazy val markPerYear: Map[Int, Either[String, BigDecimal]] = getMarkPerYear(entityPerYear, finalYearOfStudy, normalLoad, routeRulesPerYear, calculateYearMarks, weightings)
				lazy val yearWeightings: Map[Int, Option[CourseYearWeighting]] = markPerYear.map { case (year, _) =>
					val yearWeighting =  weightings.filter(_.yearOfStudy == year)
					year -> yearWeighting.headOption
				}
				if (markPerYear.exists { case (_, possibleMark) => possibleMark.isLeft }) {
					FinalYearGrade.Unknown(
						"The final overall mark cannot be calculated because there is no mark for " +
						markPerYear.filter { case (_, possibleMark) => possibleMark.isLeft }
							.map { case (year, _) => year }
							.toSeq
							.sorted
							.map { year => s"year $year" }
							.mkString(", ")
					)
				} else if (yearWeightings.exists { case (_, possibleYearWeighting) => possibleYearWeighting.isEmpty } ) {
					FinalYearGrade.Unknown("Could not find year weightings for: %s".format(
						yearWeightings.filter { case (_, possibleWeighting) => possibleWeighting.isEmpty }.map { case (year, _) =>
							s"${scyd.studentCourseDetails.course.code.toUpperCase} ${scyd.studentCourseDetails.sprStartAcademicYear.toString} Year $year"
						}.mkString(", ")
					))
				} else {
					weightedFinalYearGrade(
						scyd,
						entityPerYear,
						markPerYear.map { case (year, possibleMark) => (year, possibleMark.right.get)},
						yearWeightings.map { case (year, option) => year -> option.get }
					)
				}
			} else {
				FinalYearGrade.Ignore
			}
		}).getOrElse(FinalYearGrade.Ignore)
	}

	private def getMarkPerYear(
		entityPerYear: Map[Int, ExamGridEntityYear],
		finalYearOfStudy: Int,
		normalLoad: BigDecimal,
		routeRulesPerYear: Map[Int, Seq[UpstreamRouteRule]],
		calculatePreviousYearMarks: Boolean,
		yearWeightings: Seq[CourseYearWeighting]
	): Map[Int, Either[String, BigDecimal]] = {
		entityPerYear.filter { case (_, entityYear) => entityYear != null }.map { case (year, entityYear) =>
			year -> entityYear.studentCourseYearDetails.map(thisScyd => {
				if (!calculatePreviousYearMarks && year != finalYearOfStudy) {
					Option(thisScyd.agreedMark) match {
						case Some(mark) => Right(BigDecimal(mark))
						case _ => Left(s"Could not find agreed mark for year $year")
					}
				} else {
					getYearMark(entityYear, normalLoad, routeRulesPerYear.getOrElse(year, Seq()), yearWeightings)
				}
			}).getOrElse(Left(s"Could not find course details for year $year"))
		}
	}

	private def invalidTotalYearWeightings(markPerYear: Map[Int, BigDecimal], yearWeightings:  Map[Int, CourseYearWeighting]): Boolean = {
		// you can set up 0/50/50/50 initially. One of those could be abroad(2nd or 3rd year year). Excluding any abroad year we still should have total as 100
		//0 marks  generated for the year  are valid allowed marks with 0 weightings based on year abroad. Check the remaining ones total are  still 100%
		markPerYear.filter(_._2 > 0).map { case(year, _) =>  yearWeightings(year).weighting }.toSeq.sum != 1
	}

	private def weightedFinalYearGrade(
		scyd: StudentCourseYearDetails,
		entityPerYear: Map[Int, ExamGridEntityYear],
		markPerYear: Map[Int, BigDecimal],
		yearWeightings:  Map[Int, CourseYearWeighting]
	): FinalYearGrade = {
		// This only considers years where the weighting counts  when they are not not abroad - so for a course with an
		// intercalated year weighted 0,50,0,50, this would consider years 2 and 4. For weightings set like 0/50/50/50 (2nd or 3rd year abroad for same course), it will consider last 2 years non- abroad ones
		val finalTwoYearsModuleRegistrations =
			entityPerYear.toSeq.reverse
				.filter { case (_, gridEntityYear) => gridEntityYear != null && !ProgressionService.allowEmptyYearMarks(yearWeightings.values.toSeq, gridEntityYear)}
				.take(2)
				.flatMap { case (_, yearDetails) => yearDetails.moduleRegistrations }

		if (finalTwoYearsModuleRegistrations.filterNot(_.passFail).exists(_.firstDefinedMark.isEmpty)) {
			FinalYearGrade.Unknown(s"No agreed mark or actual mark for modules: ${
				finalTwoYearsModuleRegistrations.filter(_.firstDefinedMark.isEmpty).map(mr => "%s %s".format(mr.module.code.toUpperCase, mr.academicYear.toString)).mkString(", ")
			}")
		} else if (invalidTotalYearWeightings(markPerYear, yearWeightings)) {
			FinalYearGrade.Unknown("Total year weightings for all course years excluding abroad are not 100%")
		} else	{
			val finalMark: BigDecimal = markPerYear.map { case (year, _) =>
				markPerYear(year) * yearWeightings(year).weighting
			}.sum.setScale(1, RoundingMode.HALF_UP)

			val passedModuleRegistrationsInFinalTwoYears: Seq[ModuleRegistration] = finalTwoYearsModuleRegistrations.filter(isPassed)
			val passedCreditsInFinalTwoYears = passedModuleRegistrationsInFinalTwoYears.map(mr => BigDecimal(mr.cats)).sum >= ProgressionService.FinalTwoYearsRequiredCredits

			val passedModuleRegistrationsFinalYear: Seq[ModuleRegistration] = entityPerYear.toSeq.reverse.head._2.moduleRegistrations.filter(isPassed)
			val passedCreditsFinalYear = passedModuleRegistrationsFinalYear.map(mr => BigDecimal(mr.cats)).sum >= ProgressionService.FinalYearRequiredCredits

			val onlyAttendedOneYear = entityPerYear.size == 1

			if ((passedCreditsInFinalTwoYears || onlyAttendedOneYear) && passedCreditsFinalYear) {
				FinalYearGrade.fromMark(finalMark, courseType = scyd.studentCourseDetails.courseType.get)
			} else {
				FinalYearGrade.fail(finalMark, courseType = scyd.studentCourseDetails.courseType.get)
			}
		}
	}

}

@Service("progressionService")
class ProgressionServiceImpl
	extends AbstractProgressionService
	with AutowiringModuleRegistrationServiceComponent
	with AutowiringCourseAndRouteServiceComponent

trait ProgressionServiceComponent {
	def progressionService: ProgressionService
}

trait AutowiringProgressionServiceComponent extends ProgressionServiceComponent {
	var progressionService: ProgressionService = Wire[ProgressionService]
}
