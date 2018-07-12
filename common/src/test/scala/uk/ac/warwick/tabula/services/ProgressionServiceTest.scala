package uk.ac.warwick.tabula.services

import org.mockito.Matchers
import org.scalatest.Assertions
import uk.ac.warwick.tabula.JavaImports.JBigDecimal
import uk.ac.warwick.tabula.commands.exams.grids.ExamGridEntityYear
import uk.ac.warwick.tabula.data.model._
import uk.ac.warwick.tabula.{AcademicYear, Fixtures, Mockito, TestBase}

class ProgressionServiceTest extends TestBase with Mockito {

	val module1: Module = Fixtures.module("its01")
	val module2: Module = Fixtures.module("its02")
	val academicYear = AcademicYear(2014)
	val course: Course = Fixtures.course("its1")

	trait Fixture {
		val student: StudentMember = Fixtures.student("1234")
		student.mostSignificantCourse.course = course
		student.mostSignificantCourse.sprStartAcademicYear = academicYear - 2
		val scyd3: StudentCourseYearDetails = student.mostSignificantCourse.latestStudentCourseYearDetails
		scyd3.academicYear = academicYear
		student.mostSignificantCourse.courseYearLength = "3"
		val service = new AbstractProgressionService with ModuleRegistrationServiceComponent with CourseAndRouteServiceComponent {
			override val moduleRegistrationService: ModuleRegistrationService = smartMock[ModuleRegistrationService]
			override val courseAndRouteService: CourseAndRouteService = smartMock[CourseAndRouteService]
		}
		def entityYear3: ExamGridEntityYear = scyd3.toExamGridEntityYear

		val yearWeighting1: CourseYearWeighting = Fixtures.yearWeighting(course, new JBigDecimal(20), student.mostSignificantCourse.sprStartAcademicYear, 1)
		val yearWeighting2: CourseYearWeighting = Fixtures.yearWeighting(course, new JBigDecimal(40), student.mostSignificantCourse.sprStartAcademicYear, 2)
		val yearWeighting3: CourseYearWeighting = Fixtures.yearWeighting(course, new JBigDecimal(40), student.mostSignificantCourse.sprStartAcademicYear, 3)

		def yearWeightings: Seq[CourseYearWeighting] = Seq(yearWeighting1, yearWeighting2, yearWeighting3)

	}

	@Test
	def suggestedResultNullModuleMark(): Unit = {
		new Fixture {
			student.mostSignificantCourse.addModuleRegistration(Fixtures.moduleRegistration(
				student.mostSignificantCourse,
				module1,
				BigDecimal(30).underlying,
				academicYear,
				agreedMark = null
			))
			val result: ProgressionResult = service.suggestedResult(entityYear3, 120, Map(), calculateYearMarks=false, groupByLevel=false, yearWeightings)
			result.description should be (ProgressionResult.Unknown("").description)
			result.asInstanceOf[ProgressionResult.Unknown].details.contains(module1.code.toUpperCase) should be {true}
		}
	}

	@Test
	def suggestedResultNoModuleRegistrations(): Unit = {
		new Fixture {
			val result: ProgressionResult = service.suggestedResult(entityYear3, 120, Map(), calculateYearMarks=false, groupByLevel=false, yearWeightings)
			result.description should be (ProgressionResult.Unknown("").description)
			result.asInstanceOf[ProgressionResult.Unknown].details.contains("No module registrations found") should be {true}
		}
	}

	@Test
	def suggestedResultFirstYear(): Unit = {
		// Not satisfied overall mark
		new Fixture {
			scyd3.yearOfStudy = 1
			service.courseAndRouteService.getCourseYearWeighting(course.code, student.mostSignificantCourse.sprStartAcademicYear, 1) returns None
			service.moduleRegistrationService.findCoreRequiredModules(null, academicYear, 1) returns Seq()
			service.moduleRegistrationService.weightedMeanYearMark(any[Seq[ModuleRegistration]], any[Map[Module, BigDecimal]], any[Boolean]) returns Right(BigDecimal(30))
			service.moduleRegistrationService.overcattedModuleSubsets(any[ExamGridEntityYear], any[Map[Module, BigDecimal]], any[BigDecimal], any[Seq[UpstreamRouteRule]]) answers(args =>
				Seq((BigDecimal(30), args.asInstanceOf[Array[_]](0).asInstanceOf[ExamGridEntityYear].moduleRegistrations))
			)

			student.mostSignificantCourse.addModuleRegistration(Fixtures.moduleRegistration(
				student.mostSignificantCourse,
				module1,
				BigDecimal(30).underlying,
				academicYear,
				agreedMark = BigDecimal(100)
			))

			val result: ProgressionResult = service.suggestedResult(entityYear3, 120, Map(), calculateYearMarks=false, groupByLevel=false, yearWeightings)
			result.description should be (ProgressionResult.Resit.description)
		}

		// Not passed core required
		new Fixture {
			scyd3.yearOfStudy = 1
			service.courseAndRouteService.getCourseYearWeighting(course.code, student.mostSignificantCourse.sprStartAcademicYear, 1) returns None
			service.moduleRegistrationService.findCoreRequiredModules(null, academicYear, 1) returns Seq(new CoreRequiredModule(null, null, 1, module1))
			service.moduleRegistrationService.weightedMeanYearMark(any[Seq[ModuleRegistration]], any[Map[Module, BigDecimal]], any[Boolean]) returns Right(BigDecimal(90))
			service.moduleRegistrationService.overcattedModuleSubsets(any[ExamGridEntityYear], any[Map[Module, BigDecimal]], any[BigDecimal], any[Seq[UpstreamRouteRule]]) answers(args =>
				Seq((BigDecimal(90), args.asInstanceOf[Array[_]](0).asInstanceOf[ExamGridEntityYear].moduleRegistrations))
				)

			student.mostSignificantCourse.addModuleRegistration(Fixtures.moduleRegistration(
				student.mostSignificantCourse,
				module1,
				BigDecimal(30).underlying,
				academicYear,
				agreedMark = BigDecimal(30)
			))

			val result: ProgressionResult = service.suggestedResult(entityYear3, 120, Map(), calculateYearMarks=false, groupByLevel=false, yearWeightings)
			result.description should be (ProgressionResult.Resit.description)
		}

		// Not passed enough credits
		new Fixture {
			scyd3.yearOfStudy = 1
			service.courseAndRouteService.getCourseYearWeighting(course.code, student.mostSignificantCourse.sprStartAcademicYear, 1) returns None
			service.moduleRegistrationService.findCoreRequiredModules(null, academicYear, 1) returns Seq(new CoreRequiredModule(null, null, 1, module1))
			service.moduleRegistrationService.weightedMeanYearMark(any[Seq[ModuleRegistration]], any[Map[Module, BigDecimal]], any[Boolean]) returns Right(BigDecimal(90))
			service.moduleRegistrationService.overcattedModuleSubsets(any[ExamGridEntityYear], any[Map[Module, BigDecimal]], any[BigDecimal], any[Seq[UpstreamRouteRule]]) answers(args =>
				Seq((BigDecimal(90), args.asInstanceOf[Array[_]](0).asInstanceOf[ExamGridEntityYear].moduleRegistrations))
				)

			student.mostSignificantCourse.addModuleRegistration(Fixtures.moduleRegistration(
				student.mostSignificantCourse,
				module1,
				BigDecimal(30).underlying,
				academicYear,
				agreedMark = BigDecimal(90)
			))

			val result: ProgressionResult = service.suggestedResult(entityYear3, 120, Map(), calculateYearMarks=false, groupByLevel=false, yearWeightings)
			result.description should be (ProgressionResult.PossiblyProceed.description)
		}

		// All good
		new Fixture {
			scyd3.yearOfStudy = 1
			service.courseAndRouteService.getCourseYearWeighting(course.code, student.mostSignificantCourse.sprStartAcademicYear, 1) returns None
			service.moduleRegistrationService.findCoreRequiredModules(null, academicYear, 1) returns Seq(new CoreRequiredModule(null, null, 1, module1))
			service.moduleRegistrationService.weightedMeanYearMark(any[Seq[ModuleRegistration]], any[Map[Module, BigDecimal]], any[Boolean]) returns Right(BigDecimal(90))
			service.moduleRegistrationService.overcattedModuleSubsets(any[ExamGridEntityYear], any[Map[Module, BigDecimal]], any[BigDecimal], any[Seq[UpstreamRouteRule]]) answers(args =>
				Seq((BigDecimal(90), args.asInstanceOf[Array[_]](0).asInstanceOf[ExamGridEntityYear].moduleRegistrations))
			)

			student.mostSignificantCourse.addModuleRegistration(Fixtures.moduleRegistration(
				student.mostSignificantCourse,
				module1,
				BigDecimal(60).underlying,
				academicYear,
				agreedMark = BigDecimal(90)
			))
			student.mostSignificantCourse.addModuleRegistration(Fixtures.moduleRegistration(
				student.mostSignificantCourse,
				module1,
				BigDecimal(30).underlying,
				academicYear,
				agreedMark = BigDecimal(40)
			))

			val result: ProgressionResult = service.suggestedResult(entityYear3, 120, Map(), calculateYearMarks=false, groupByLevel=false, yearWeightings)
			result.description should be (ProgressionResult.Proceed.description)
		}
	}

	@Test
	def suggestedResultIntermediateYear(): Unit = {
		// Not satisfied overall mark
		new Fixture {
			scyd3.yearOfStudy = 2
			service.courseAndRouteService.getCourseYearWeighting(course.code, student.mostSignificantCourse.sprStartAcademicYear, 2) returns None
			service.moduleRegistrationService.findCoreRequiredModules(null, academicYear, 2) returns Seq()
			service.moduleRegistrationService.weightedMeanYearMark(any[Seq[ModuleRegistration]], any[Map[Module, BigDecimal]], any[Boolean]) returns Right(BigDecimal(30))
			service.moduleRegistrationService.overcattedModuleSubsets(any[ExamGridEntityYear], any[Map[Module, BigDecimal]], Matchers.eq(BigDecimal(120)), Matchers.eq(Seq())) returns Seq(
				(BigDecimal(30.0), Seq())
			)

			student.mostSignificantCourse.addModuleRegistration(Fixtures.moduleRegistration(
				student.mostSignificantCourse,
				module1,
				BigDecimal(30).underlying,
				academicYear,
				agreedMark = BigDecimal(100)
			))

			val result: ProgressionResult = service.suggestedResult(entityYear3, 120, Map(), calculateYearMarks=false, groupByLevel=false, yearWeightings)
			result.description should be (ProgressionResult.Resit.description)
		}

		// Not passed enough credits
		new Fixture {
			scyd3.yearOfStudy = 2
			service.courseAndRouteService.getCourseYearWeighting(course.code, student.mostSignificantCourse.sprStartAcademicYear, 2) returns None
			service.moduleRegistrationService.findCoreRequiredModules(null, academicYear, 2) returns Seq(new CoreRequiredModule(null, null, 2, module1))
			service.moduleRegistrationService.weightedMeanYearMark(any[Seq[ModuleRegistration]], any[Map[Module, BigDecimal]], any[Boolean]) returns Right(BigDecimal(90))
			service.moduleRegistrationService.overcattedModuleSubsets(any[ExamGridEntityYear], any[Map[Module, BigDecimal]], Matchers.eq(BigDecimal(120)), Matchers.eq(Seq())) returns Seq(
				(BigDecimal(90.0), Seq())
			)

			student.mostSignificantCourse.addModuleRegistration(Fixtures.moduleRegistration(
				student.mostSignificantCourse,
				module1,
				BigDecimal(30).underlying,
				academicYear,
				agreedMark = BigDecimal(90)
			))

			val result: ProgressionResult = service.suggestedResult(entityYear3, 120, Map(), calculateYearMarks=false, groupByLevel=false, yearWeightings)
			result.description should be (ProgressionResult.Resit.description)
		}

		// All good
		new Fixture {
			scyd3.yearOfStudy = 2
			service.courseAndRouteService.getCourseYearWeighting(course.code, student.mostSignificantCourse.sprStartAcademicYear, 2) returns None
			service.moduleRegistrationService.findCoreRequiredModules(null, academicYear, 2) returns Seq(new CoreRequiredModule(null, null, 2, module1))
			service.moduleRegistrationService.weightedMeanYearMark(any[Seq[ModuleRegistration]], any[Map[Module, BigDecimal]], any[Boolean]) returns Right(BigDecimal(90))
			service.moduleRegistrationService.overcattedModuleSubsets(any[ExamGridEntityYear], any[Map[Module, BigDecimal]], Matchers.eq(BigDecimal(120)), Matchers.eq(Seq())) returns Seq(
				(BigDecimal(90.0), Seq())
			)

			student.mostSignificantCourse.addModuleRegistration(Fixtures.moduleRegistration(
				student.mostSignificantCourse,
				module1,
				BigDecimal(60).underlying,
				academicYear,
				agreedMark = BigDecimal(90)
			))
			student.mostSignificantCourse.addModuleRegistration(Fixtures.moduleRegistration(
				student.mostSignificantCourse,
				module1,
				BigDecimal(30).underlying,
				academicYear,
				agreedMark = BigDecimal(40)
			))

			val result: ProgressionResult = service.suggestedResult(entityYear3, 120, Map(), calculateYearMarks=false, groupByLevel=false, yearWeightings)
			result.description should be (ProgressionResult.Proceed.description)
		}
	}

	@Test
	def suggestedFinalYearGradeNotFinalYear(): Unit = {
		new Fixture {
			scyd3.yearOfStudy = 2
			val result: FinalYearGrade = service.suggestedFinalYearGrade(entityYear3, 120, Map(), calculateYearMarks=false, groupByLevel=false, yearWeightings)
			result.description should be (FinalYearGrade.Ignore.description)
		}
	}

	trait ThreeYearStudentFixture extends Fixture {
		scyd3.yearOfStudy = 3
		val scyd1: StudentCourseYearDetails = Fixtures.studentCourseYearDetails()
		scyd1.studentCourseDetails = student.mostSignificantCourse
		scyd1.yearOfStudy = 1
		scyd1.academicYear = academicYear - 2
		val entityYear1: ExamGridEntityYear = scyd1.toExamGridEntityYear
		student.mostSignificantCourse.addStudentCourseYearDetails(scyd1)
		val scyd2: StudentCourseYearDetails = Fixtures.studentCourseYearDetails()
		scyd2.studentCourseDetails = student.mostSignificantCourse
		scyd2.yearOfStudy = 2
		scyd2.academicYear = academicYear - 1
		val entityYear2: ExamGridEntityYear = scyd2.toExamGridEntityYear
		student.mostSignificantCourse.addStudentCourseYearDetails(scyd2)
	}

	@Test
	def suggestedFinalYearGradeMissingYearMark(): Unit = {
		// No marks for any year
		new ThreeYearStudentFixture {
			service.courseAndRouteService.getCourseYearWeighting(course.code, student.mostSignificantCourse.sprStartAcademicYear, 3) returns None
			service.moduleRegistrationService.weightedMeanYearMark(any[Seq[ModuleRegistration]], any[Map[Module, BigDecimal]], any[Boolean]) returns Left("No mark for you")
			service.moduleRegistrationService.overcattedModuleSubsets(any[ExamGridEntityYear], any[Map[Module, BigDecimal]], any[BigDecimal], any[Seq[UpstreamRouteRule]]) answers(args => Seq())
			val result: FinalYearGrade = service.suggestedFinalYearGrade(entityYear3, 120, Map(), calculateYearMarks=false, groupByLevel=false, yearWeightings)
			result.description should be (FinalYearGrade.Unknown("").description)
			result.asInstanceOf[FinalYearGrade.Unknown].details should be ("The final overall mark cannot be calculated because there is no mark for year 1, year 2, year 3")
		}

		// No mark for 2nd year
		new ThreeYearStudentFixture {
			service.courseAndRouteService.getCourseYearWeighting(course.code, student.mostSignificantCourse.sprStartAcademicYear, 3) returns None
			service.moduleRegistrationService.weightedMeanYearMark(any[Seq[ModuleRegistration]], any[Map[Module, BigDecimal]], any[Boolean]) returns Right(BigDecimal(90.0))
			service.moduleRegistrationService.overcattedModuleSubsets(any[ExamGridEntityYear], any[Map[Module, BigDecimal]], any[BigDecimal], any[Seq[UpstreamRouteRule]]) answers(args =>
				Seq((BigDecimal(90.0), args.asInstanceOf[Array[_]](0).asInstanceOf[ExamGridEntityYear].moduleRegistrations))
			)
			student.mostSignificantCourse.freshStudentCourseYearDetails.head.agreedMark = BigDecimal(90.0).underlying
			val result: FinalYearGrade = service.suggestedFinalYearGrade(entityYear3, 120, Map(), calculateYearMarks=false, groupByLevel=false, yearWeightings)
			result.description should be (FinalYearGrade.Unknown("").description)
			result.asInstanceOf[FinalYearGrade.Unknown].details should be ("The final overall mark cannot be calculated because there is no mark for year 2")
		}

		// No mark for this year
		new ThreeYearStudentFixture {
			service.courseAndRouteService.getCourseYearWeighting(course.code, student.mostSignificantCourse.sprStartAcademicYear, 3) returns None
			service.moduleRegistrationService.weightedMeanYearMark(any[Seq[ModuleRegistration]], any[Map[Module, BigDecimal]], any[Boolean]) returns Left("No mark for you")
			service.moduleRegistrationService.overcattedModuleSubsets(any[ExamGridEntityYear], any[Map[Module, BigDecimal]], any[BigDecimal], any[Seq[UpstreamRouteRule]]) answers(args => Seq())
			student.mostSignificantCourse.freshStudentCourseYearDetails.head.agreedMark = BigDecimal(90.0).underlying
			student.mostSignificantCourse.freshStudentCourseYearDetails.tail.head.agreedMark = BigDecimal(90.0).underlying
			val result: FinalYearGrade = service.suggestedFinalYearGrade(entityYear3, 120, Map(), calculateYearMarks=false, groupByLevel=false, yearWeightings)
			result.description should be (FinalYearGrade.Unknown("").description)
			result.asInstanceOf[FinalYearGrade.Unknown].details should be ("The final overall mark cannot be calculated because there is no mark for year 3")
		}
	}

	@Test
	def suggestedFinalYearGradeOvercat(): Unit = {
		trait TestFixture extends ThreeYearStudentFixture {
			service.moduleRegistrationService.weightedMeanYearMark(any[Seq[ModuleRegistration]], any[Map[Module, BigDecimal]], any[Boolean]) returns Right(BigDecimal(90.0))
			student.mostSignificantCourse.freshStudentCourseYearDetails.head.agreedMark = BigDecimal(90.0).underlying
			student.mostSignificantCourse.freshStudentCourseYearDetails.tail.head.agreedMark = BigDecimal(90.0).underlying
			service.courseAndRouteService.getCourseYearWeighting(Matchers.eq(course.code), Matchers.eq(student.mostSignificantCourse.sprStartAcademicYear), any[Int]) returns None

			val mr1: ModuleRegistration = Fixtures.moduleRegistration(
				student.mostSignificantCourse,
				module1,
				BigDecimal(60).underlying,
				academicYear,
				agreedMark = BigDecimal(90)
			)
			val mr2: ModuleRegistration = Fixtures.moduleRegistration(
				student.mostSignificantCourse,
				module2,
				BigDecimal(75).underlying,
				academicYear,
				agreedMark = BigDecimal(40)
			)
			student.mostSignificantCourse.addModuleRegistration(mr1)
			student.mostSignificantCourse.addModuleRegistration(mr2)
			scyd3.moduleAndDepartmentService = smartMock[ModuleAndDepartmentService]
			scyd3.moduleAndDepartmentService.getModuleByCode(module1.code) returns Option(module1)
			scyd3.moduleAndDepartmentService.getModuleByCode(module2.code) returns Option(module2)
		}

		// Not overcat
		new TestFixture {
			service.moduleRegistrationService.overcattedModuleSubsets(any[ExamGridEntityYear], any[Map[Module, BigDecimal]], Matchers.eq(BigDecimal(180)), Matchers.eq(Seq())) returns Seq(
				(BigDecimal(90.0), Seq(mr1, mr2))
			)
			val result: FinalYearGrade = service.suggestedFinalYearGrade(entityYear3, 180, Map(), calculateYearMarks=false, groupByLevel=false, Seq())
			result.description should be (FinalYearGrade.Unknown("").description)
			result.asInstanceOf[FinalYearGrade.Unknown].details.contains("Could not find year weightings") should be {true}
		}

		// Overcat, single subset
		new TestFixture {
			service.moduleRegistrationService.overcattedModuleSubsets(any[ExamGridEntityYear], any[Map[Module, BigDecimal]], Matchers.eq(BigDecimal(180)), Matchers.eq(Seq())) returns Seq(
				(BigDecimal(90.0), Seq(mr1, mr2))
			) // One subset

			val result: FinalYearGrade = service.suggestedFinalYearGrade(entityYear3, 180, Map(), calculateYearMarks=false, groupByLevel=false, Seq())
			// Should re-use the initally calculated mark as only 1 subset
			verify(service.moduleRegistrationService, times(1)).weightedMeanYearMark(any[Seq[ModuleRegistration]], any[Map[Module, BigDecimal]], any[Boolean])
			result.description should be (FinalYearGrade.Unknown("").description)
			result.asInstanceOf[FinalYearGrade.Unknown].details.contains("Could not find year weightings") should be {true}
		}

		// Overcat, 2 subsets, none chosen
		new TestFixture {
			val mr3: ModuleRegistration = Fixtures.moduleRegistration(
				student.mostSignificantCourse,
				module2,
				BigDecimal(60).underlying,
				academicYear,
				agreedMark = BigDecimal(50)
			)
			student.mostSignificantCourse.addModuleRegistration(mr3)
			service.moduleRegistrationService.overcattedModuleSubsets(any[ExamGridEntityYear], any[Map[Module, BigDecimal]], Matchers.eq(BigDecimal(180)), Matchers.eq(Seq())) returns Seq(
				(BigDecimal(90.0), Seq(mr1, mr2)),
				(BigDecimal(90.0), Seq(mr1, mr3))
			) // Two subsets

			val result: FinalYearGrade = service.suggestedFinalYearGrade(entityYear3, 180, Map(), calculateYearMarks=false, groupByLevel=false, Seq())
			result.description should be (FinalYearGrade.Unknown("").description)
			result.asInstanceOf[FinalYearGrade.Unknown].details should be ("The final overall mark cannot be calculated because there is no mark for year 3")
		}

		// Overcat, 2 subsets, 1 chosen
		new TestFixture {
			val mr3: ModuleRegistration = Fixtures.moduleRegistration(
				student.mostSignificantCourse,
				module2,
				BigDecimal(60).underlying,
				academicYear,
				agreedMark = BigDecimal(50)
			)
			student.mostSignificantCourse.addModuleRegistration(mr3)
			service.moduleRegistrationService.overcattedModuleSubsets(any[ExamGridEntityYear], any[Map[Module, BigDecimal]], Matchers.eq(BigDecimal(180)), Matchers.eq(Seq())) returns Seq(
				(BigDecimal(90.0), Seq(mr1, mr2)),
				(BigDecimal(90.0), Seq(mr1, mr3))
			) // Two subsets
			scyd3.overcattingModules = Seq(module1, module2) // Subset chosen

			val result: FinalYearGrade = service.suggestedFinalYearGrade(entityYear3, 180, Map(), calculateYearMarks=false, groupByLevel=false, Seq())
			result.description should be (FinalYearGrade.Unknown("").description)
			result.asInstanceOf[FinalYearGrade.Unknown].details.contains("Could not find year weightings") should be {true}
		}
	}

	@Test
	def suggestedFinalYearGradeMissingYearWeightings(): Unit = {
		// No weightings for any year
		new ThreeYearStudentFixture {
			service.moduleRegistrationService.weightedMeanYearMark(any[Seq[ModuleRegistration]], any[Map[Module, BigDecimal]], any[Boolean]) returns Right(BigDecimal(90.0))
			student.mostSignificantCourse.freshStudentCourseYearDetails.head.agreedMark = BigDecimal(90.0).underlying
			student.mostSignificantCourse.freshStudentCourseYearDetails.tail.head.agreedMark = BigDecimal(90.0).underlying
			service.moduleRegistrationService.overcattedModuleSubsets(any[ExamGridEntityYear], any[Map[Module, BigDecimal]], Matchers.eq(BigDecimal(180)), Matchers.eq(Seq())) returns Seq(
				(BigDecimal(90.0), Seq())
			)

			service.courseAndRouteService.getCourseYearWeighting(course.code, student.mostSignificantCourse.sprStartAcademicYear, 1) returns None
			service.courseAndRouteService.getCourseYearWeighting(course.code, student.mostSignificantCourse.sprStartAcademicYear, 2) returns None
			service.courseAndRouteService.getCourseYearWeighting(course.code, student.mostSignificantCourse.sprStartAcademicYear, 3) returns None
			val result: FinalYearGrade = service.suggestedFinalYearGrade(entityYear3, 180, Map(), calculateYearMarks=false, groupByLevel=false, Seq())
			result.description should be (FinalYearGrade.Unknown("").description)
			result.asInstanceOf[FinalYearGrade.Unknown].details.contains("Could not find year weightings") should be {true}
		}

		// No weighting for 2nd year
		new ThreeYearStudentFixture {
			service.moduleRegistrationService.weightedMeanYearMark(any[Seq[ModuleRegistration]], any[Map[Module, BigDecimal]], any[Boolean]) returns Right(BigDecimal(90.0))
			student.mostSignificantCourse.freshStudentCourseYearDetails.head.agreedMark = BigDecimal(90.0).underlying
			student.mostSignificantCourse.freshStudentCourseYearDetails.tail.head.agreedMark = BigDecimal(90.0).underlying
			service.moduleRegistrationService.overcattedModuleSubsets(any[ExamGridEntityYear], any[Map[Module, BigDecimal]], Matchers.eq(BigDecimal(180)), Matchers.eq(Seq())) returns Seq(
				(BigDecimal(90.0), Seq())
			)
			val year1Weighting: CourseYearWeighting = Fixtures.yearWeighting(course, new JBigDecimal(20), student.mostSignificantCourse.sprStartAcademicYear, 1)
			val year3Weighting: CourseYearWeighting = Fixtures.yearWeighting(course, new JBigDecimal(40), student.mostSignificantCourse.sprStartAcademicYear, 3)


			service.courseAndRouteService.getCourseYearWeighting(course.code, student.mostSignificantCourse.sprStartAcademicYear, 1) returns Some(year1Weighting)
			service.courseAndRouteService.getCourseYearWeighting(course.code, student.mostSignificantCourse.sprStartAcademicYear, 2) returns None
			service.courseAndRouteService.getCourseYearWeighting(course.code, student.mostSignificantCourse.sprStartAcademicYear, 3) returns Some(year3Weighting)
			val result: FinalYearGrade = service.suggestedFinalYearGrade(entityYear3, 180, Map(), calculateYearMarks=false, groupByLevel=false, Seq(year1Weighting,year3Weighting))
			result.description should be (FinalYearGrade.Unknown("").description)
			result.asInstanceOf[FinalYearGrade.Unknown].details.contains(s"Could not find year weightings for: ${course.code.toUpperCase} ${student.mostSignificantCourse.sprStartAcademicYear.toString} Year 2")
		}
	}

	trait ThreeYearStudentWithMarksAndYearWeightingsFixture extends ThreeYearStudentFixture {
		student.mostSignificantCourse.freshStudentCourseYearDetails.head.agreedMark = BigDecimal(65.0).underlying
		student.mostSignificantCourse.freshStudentCourseYearDetails.tail.head.agreedMark = BigDecimal(60.0).underlying
		service.moduleRegistrationService.weightedMeanYearMark(any[Seq[ModuleRegistration]], any[Map[Module, BigDecimal]], any[Boolean]) returns Right(BigDecimal(80.0))
		service.moduleRegistrationService.overcattedModuleSubsets(any[ExamGridEntityYear], any[Map[Module, BigDecimal]], any[BigDecimal], any[Seq[UpstreamRouteRule]]) answers(args =>
			Seq((BigDecimal(80.0), args.asInstanceOf[Array[_]](0).asInstanceOf[ExamGridEntityYear].moduleRegistrations))
		)

		service.courseAndRouteService.getCourseYearWeighting(course.code, student.mostSignificantCourse.sprStartAcademicYear, 1) returns Option(yearWeighting1)
		service.courseAndRouteService.getCourseYearWeighting(course.code, student.mostSignificantCourse.sprStartAcademicYear, 2) returns Option(yearWeighting2)
		service.courseAndRouteService.getCourseYearWeighting(course.code, student.mostSignificantCourse.sprStartAcademicYear, 3) returns Option(yearWeighting3)
	}

	@Test
	def suggestedFinalYearGrade(): Unit = {
		// Missing module mark in 2nd year
		new ThreeYearStudentWithMarksAndYearWeightingsFixture {
			val mr1: ModuleRegistration = Fixtures.moduleRegistration(
				student.mostSignificantCourse,
				module1,
				BigDecimal(60).underlying,
				scyd2.academicYear,
				agreedMark = null
			)
			val mr2: ModuleRegistration = Fixtures.moduleRegistration(
				student.mostSignificantCourse,
				module2,
				BigDecimal(60).underlying,
				scyd2.academicYear,
				agreedMark = BigDecimal(90)
			)
			student.mostSignificantCourse.addModuleRegistration(mr1)
			student.mostSignificantCourse.addModuleRegistration(mr2)
			val result: FinalYearGrade = service.suggestedFinalYearGrade(entityYear3, 180, Map(), calculateYearMarks=false, groupByLevel=false, yearWeightings)
			result.description should be (FinalYearGrade.Unknown("").description)
			result.asInstanceOf[FinalYearGrade.Unknown].details.contains(module1.code.toUpperCase) should be {true}
		}

		// Not enough credits passed in 2nd year
		new ThreeYearStudentWithMarksAndYearWeightingsFixture {
			val mr1: ModuleRegistration = Fixtures.moduleRegistration(
				student.mostSignificantCourse,
				module1,
				BigDecimal(40).underlying,
				scyd2.academicYear,
				agreedMark = BigDecimal(90)
			)
			val mr2: ModuleRegistration = Fixtures.moduleRegistration(
				student.mostSignificantCourse,
				module2,
				BigDecimal(40).underlying,
				scyd2.academicYear,
				agreedMark = BigDecimal(35) // Failed in 2nd year
			)
			val mr3: ModuleRegistration = Fixtures.moduleRegistration(
				student.mostSignificantCourse,
				module1,
				BigDecimal(40).underlying,
				scyd3.academicYear,
				agreedMark = BigDecimal(90)
			)
			val mr4: ModuleRegistration = Fixtures.moduleRegistration(
				student.mostSignificantCourse,
				module2,
				BigDecimal(60).underlying,
				scyd3.academicYear,
				agreedMark = BigDecimal(90)
			)
			student.mostSignificantCourse.addModuleRegistration(mr1)
			student.mostSignificantCourse.addModuleRegistration(mr2)
			student.mostSignificantCourse.addModuleRegistration(mr3)
			student.mostSignificantCourse.addModuleRegistration(mr4)
			val result: FinalYearGrade = service.suggestedFinalYearGrade(entityYear3, 180, Map(), calculateYearMarks=false, groupByLevel=false, yearWeightings)
			result match {
				case withMark: FinalYearMark => withMark.mark should be (BigDecimal(69))
				case _ => Assertions.fail("Incorrect type returned")
			}
			result.description should be (FinalYearGrade.Fail.description)
		}

		// Not enough credits passed in final year
		new ThreeYearStudentWithMarksAndYearWeightingsFixture {
			val mr1: ModuleRegistration = Fixtures.moduleRegistration(
				student.mostSignificantCourse,
				module1,
				BigDecimal(40).underlying,
				scyd2.academicYear,
				agreedMark = BigDecimal(90)
			)
			val mr2: ModuleRegistration = Fixtures.moduleRegistration(
				student.mostSignificantCourse,
				module2,
				BigDecimal(40).underlying,
				scyd2.academicYear,
				agreedMark = BigDecimal(90)
			)
			val mr3: ModuleRegistration = Fixtures.moduleRegistration(
				student.mostSignificantCourse,
				module1,
				BigDecimal(40).underlying,
				scyd3.academicYear,
				agreedMark = BigDecimal(90)
			)
			val mr4: ModuleRegistration = Fixtures.moduleRegistration(
				student.mostSignificantCourse,
				module2,
				BigDecimal(60).underlying,
				scyd3.academicYear,
				agreedMark = BigDecimal(35) // Failed in final year
			)
			student.mostSignificantCourse.addModuleRegistration(mr1)
			student.mostSignificantCourse.addModuleRegistration(mr2)
			student.mostSignificantCourse.addModuleRegistration(mr3)
			student.mostSignificantCourse.addModuleRegistration(mr4)
			val result: FinalYearGrade = service.suggestedFinalYearGrade(entityYear3, 180, Map(), calculateYearMarks=false, groupByLevel=false, yearWeightings)
			result match {
				case withMark: FinalYearMark => withMark.mark should be (BigDecimal(69))
				case _ => Assertions.fail("Incorrect type returned")
			}
			result.description should be (FinalYearGrade.Fail.description)
		}

		// All good
		new ThreeYearStudentWithMarksAndYearWeightingsFixture {
			val mr1: ModuleRegistration = Fixtures.moduleRegistration(
				student.mostSignificantCourse,
				module1,
				BigDecimal(40).underlying,
				scyd2.academicYear,
				agreedMark = BigDecimal(90)
			)
			val mr2: ModuleRegistration = Fixtures.moduleRegistration(
				student.mostSignificantCourse,
				module2,
				BigDecimal(40).underlying,
				scyd2.academicYear,
				agreedMark = BigDecimal(90)
			)
			val mr3: ModuleRegistration = Fixtures.moduleRegistration(
				student.mostSignificantCourse,
				module1,
				BigDecimal(40).underlying,
				scyd3.academicYear,
				agreedMark = BigDecimal(90)
			)
			val mr4: ModuleRegistration = Fixtures.moduleRegistration(
				student.mostSignificantCourse,
				module2,
				BigDecimal(60).underlying,
				scyd3.academicYear,
				agreedMark = BigDecimal(90)
			)
			student.mostSignificantCourse.addModuleRegistration(mr1)
			student.mostSignificantCourse.addModuleRegistration(mr2)
			student.mostSignificantCourse.addModuleRegistration(mr3)
			student.mostSignificantCourse.addModuleRegistration(mr4)
			val result: FinalYearGrade = service.suggestedFinalYearGrade(entityYear3, 180, Map(), calculateYearMarks=false, groupByLevel=false, yearWeightings)
			// 1st year: 65 @ 20%, 2nd year: 60 @ 40%, 3rd year 80 @ 40%
			// Final grade 69 = 2.1, but on the grade boundary so borderline
			result match {
				case withMark: FinalYearMark => withMark.mark should be (BigDecimal(69))
				case _ => Assertions.fail("Incorrect type returned")
			}
			result.description should be (FinalYearGrade.UpperSecondBorderline.description)
		}
	}

	@Test
	def suggestedFinalYearGradeWithZeroWeightedYear(): Unit = {
		// Zero-weighted second year not considered
		new ThreeYearStudentWithMarksAndYearWeightingsFixture {
			val year1Weighting: CourseYearWeighting = Fixtures.yearWeighting(course, new JBigDecimal(70), student.mostSignificantCourse.sprStartAcademicYear, 1)
			val year2Weighting: CourseYearWeighting = Fixtures.yearWeighting(course, new JBigDecimal(0), student.mostSignificantCourse.sprStartAcademicYear, 2)
			val year3Weighting: CourseYearWeighting = Fixtures.yearWeighting(course, new JBigDecimal(30), student.mostSignificantCourse.sprStartAcademicYear, 3)

			val mr1: ModuleRegistration = Fixtures.moduleRegistration(
				student.mostSignificantCourse,
				module1,
				BigDecimal(40).underlying,
				scyd1.academicYear,
				agreedMark = BigDecimal(90)
			)
			val mr2: ModuleRegistration = Fixtures.moduleRegistration(
				student.mostSignificantCourse,
				module2,
				BigDecimal(40).underlying,
				scyd1.academicYear,
				agreedMark = BigDecimal(90)
			)
			val mr3: ModuleRegistration = Fixtures.moduleRegistration(
				student.mostSignificantCourse,
				module1,
				BigDecimal(40).underlying,
				scyd3.academicYear,
				agreedMark = BigDecimal(90)
			)
			val mr4: ModuleRegistration = Fixtures.moduleRegistration(
				student.mostSignificantCourse,
				module2,
				BigDecimal(60).underlying,
				scyd3.academicYear,
				agreedMark = BigDecimal(90)
			)
			student.mostSignificantCourse.addModuleRegistration(mr1)
			student.mostSignificantCourse.addModuleRegistration(mr2)
			student.mostSignificantCourse.addModuleRegistration(mr3)
			student.mostSignificantCourse.addModuleRegistration(mr4)
			val result: FinalYearGrade = service.suggestedFinalYearGrade(entityYear3, 180, Map(), calculateYearMarks = false, groupByLevel = false, Seq(year1Weighting, year2Weighting, year3Weighting))
			// 1st year: 65 @ 70%, 2nd year: 60 @ 0%, 3rd year 80 @ 30%
			// Final grade 69.5 = 2.1, but on the grade boundary so borderline
			result match {
				case withMark: FinalYearMark => withMark.mark should be (BigDecimal(69.5))
				case _ => Assertions.fail("Incorrect type returned")
			}
			result.description should be (FinalYearGrade.UpperSecondBorderline.description)
		}
	}

	@Test
	def getYearMark(): Unit = {
		new ThreeYearStudentWithMarksAndYearWeightingsFixture {
			val year1Result: Either[String, BigDecimal] = service.getYearMark(entityYear1, 180, Nil, yearWeightings)
			year1Result should be (Right(BigDecimal(80.0)))

			val year2Result: Either[String, BigDecimal] = service.getYearMark(entityYear2, 180, Nil, yearWeightings)
			year2Result should be (Right(BigDecimal(80.0)))

			val year3Result: Either[String, BigDecimal] = service.getYearMark(entityYear3, 180, Nil, yearWeightings)
			year3Result should be (Right(BigDecimal(80.0)))
		}

		new Fixture {
			service.courseAndRouteService.getCourseYearWeighting(course.code, student.mostSignificantCourse.sprStartAcademicYear, 1) returns None
			service.moduleRegistrationService.weightedMeanYearMark(any[Seq[ModuleRegistration]], any[Map[Module, BigDecimal]], any[Boolean]) returns Right(BigDecimal(30))
			service.moduleRegistrationService.overcattedModuleSubsets(any[ExamGridEntityYear], any[Map[Module, BigDecimal]], any[BigDecimal], any[Seq[UpstreamRouteRule]]) answers(args => Seq())

			val result: Either[String, BigDecimal] = service.getYearMark(entityYear3, 180, Nil, yearWeightings)
			result should be (Right(BigDecimal(30)))
		}
	}


}
