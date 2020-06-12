package uk.ac.warwick.tabula.services

import org.mockito.ArgumentMatchers
import org.scalatest.Assertions
import uk.ac.warwick.tabula.JavaImports.JBigDecimal
import uk.ac.warwick.tabula.commands.exams.grids.ExamGridEntityYear
import uk.ac.warwick.tabula.data.model.CourseType.{PGT, UG}
import uk.ac.warwick.tabula.data.model._
import uk.ac.warwick.tabula.{AcademicYear, Fixtures, Mockito, TestBase}

import scala.util.Random

class ProgressionServiceTest extends TestBase with Mockito {

  val module1: Module = Fixtures.module("its01")
  val module2: Module = Fixtures.module("its02")
  val academicYear = AcademicYear(2014)
  val course: Course = Fixtures.course("UCSA-ITS1")

  trait Fixture {
    val student: StudentMember = Fixtures.student("1234")
    student.mostSignificantCourse.course = course
    student.mostSignificantCourse.sprStartAcademicYear = academicYear - 2
    val scyd3: StudentCourseYearDetails = student.mostSignificantCourse.latestStudentCourseYearDetails
    scyd3.academicYear = academicYear
    student.mostSignificantCourse.courseYearLength = 3
    scyd3.modeOfAttendance = Fixtures.modeOfAttendance("F", "FT", "Full time")
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
      student.mostSignificantCourse._moduleRegistrations.add(Fixtures.moduleRegistration(
        student.mostSignificantCourse,
        module1,
        BigDecimal(30).underlying,
        academicYear,
        agreedMark = None
      ))
      val result: ProgressionResult = service.suggestedResult(entityYear3, 120, Map(), calculateYearMarks = false, groupByLevel = false, applyBenchmark = false, yearWeightings)
      result.description should be(ProgressionResult.Unknown("").description)
      result.asInstanceOf[ProgressionResult.Unknown].details.contains(module1.code.toUpperCase) should be(true)
    }
  }

  @Test
  def suggestedResultNoModuleRegistrations(): Unit = {
    new Fixture {
      val result: ProgressionResult = service.suggestedResult(entityYear3, 120, Map(), calculateYearMarks = false, groupByLevel = false, applyBenchmark = false, yearWeightings)
      result.description should be(ProgressionResult.Unknown("").description)
      result.asInstanceOf[ProgressionResult.Unknown].details.contains("No module registrations found") should be(true)
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
      service.moduleRegistrationService.overcattedModuleSubsets(any[Seq[ModuleRegistration]], any[Map[Module, BigDecimal]], any[BigDecimal], any[Seq[UpstreamRouteRule]]) answers { args: Array[AnyRef] =>
        Seq((BigDecimal(30), args(0).asInstanceOf[Seq[ModuleRegistration]]))
      }

      student.mostSignificantCourse._moduleRegistrations.add(Fixtures.moduleRegistration(
        student.mostSignificantCourse,
        module1,
        BigDecimal(30).underlying,
        academicYear,
        agreedMark = Some(100)
      ))

      val result: ProgressionResult = service.suggestedResult(entityYear3, 120, Map(), calculateYearMarks = false, groupByLevel = false, applyBenchmark = false, yearWeightings)
      result.description should be(ProgressionResult.Resit.description)
    }

    // Not passed core required
    new Fixture {
      scyd3.yearOfStudy = 1
      service.courseAndRouteService.getCourseYearWeighting(course.code, student.mostSignificantCourse.sprStartAcademicYear, 1) returns None
      service.moduleRegistrationService.findCoreRequiredModules(null, academicYear, 1) returns Seq(new CoreRequiredModule(null, null, 1, module1))
      service.moduleRegistrationService.weightedMeanYearMark(any[Seq[ModuleRegistration]], any[Map[Module, BigDecimal]], any[Boolean]) returns Right(BigDecimal(90))
      service.moduleRegistrationService.overcattedModuleSubsets(any[Seq[ModuleRegistration]], any[Map[Module, BigDecimal]], any[BigDecimal], any[Seq[UpstreamRouteRule]]) answers { args: Array[AnyRef] =>
        Seq((BigDecimal(90), args(0).asInstanceOf[Seq[ModuleRegistration]]))
      }

      student.mostSignificantCourse._moduleRegistrations.add(Fixtures.moduleRegistration(
        student.mostSignificantCourse,
        module1,
        BigDecimal(30).underlying,
        academicYear,
        agreedMark = Some(30)
      ))

      val result: ProgressionResult = service.suggestedResult(entityYear3, 120, Map(), calculateYearMarks = false, groupByLevel = false, applyBenchmark = false, yearWeightings)
      result.description should be(ProgressionResult.Resit.description)
    }

    // Not passed enough credits
    new Fixture {
      scyd3.yearOfStudy = 1
      service.courseAndRouteService.getCourseYearWeighting(course.code, student.mostSignificantCourse.sprStartAcademicYear, 1) returns None
      service.moduleRegistrationService.findCoreRequiredModules(null, academicYear, 1) returns Seq(new CoreRequiredModule(null, null, 1, module1))
      service.moduleRegistrationService.weightedMeanYearMark(any[Seq[ModuleRegistration]], any[Map[Module, BigDecimal]], any[Boolean]) returns Right(BigDecimal(90))
      service.moduleRegistrationService.overcattedModuleSubsets(any[Seq[ModuleRegistration]], any[Map[Module, BigDecimal]], any[BigDecimal], any[Seq[UpstreamRouteRule]]) answers { args: Array[AnyRef] =>
        Seq((BigDecimal(90), args(0).asInstanceOf[Seq[ModuleRegistration]]))
      }

      student.mostSignificantCourse._moduleRegistrations.add(Fixtures.moduleRegistration(
        student.mostSignificantCourse,
        module1,
        BigDecimal(30).underlying,
        academicYear,
        agreedMark = Some(90)
      ))

      val result: ProgressionResult = service.suggestedResult(entityYear3, 120, Map(), calculateYearMarks = false, groupByLevel = false, applyBenchmark = false, yearWeightings)
      result.description should be(ProgressionResult.PossiblyProceed.description)
    }

    // All good
    new Fixture {
      scyd3.yearOfStudy = 1
      service.courseAndRouteService.getCourseYearWeighting(course.code, student.mostSignificantCourse.sprStartAcademicYear, 1) returns None
      service.moduleRegistrationService.findCoreRequiredModules(null, academicYear, 1) returns Seq(new CoreRequiredModule(null, null, 1, module1))
      service.moduleRegistrationService.weightedMeanYearMark(any[Seq[ModuleRegistration]], any[Map[Module, BigDecimal]], any[Boolean]) returns Right(BigDecimal(90))
      service.moduleRegistrationService.overcattedModuleSubsets(any[Seq[ModuleRegistration]], any[Map[Module, BigDecimal]], any[BigDecimal], any[Seq[UpstreamRouteRule]]) answers { args: Array[AnyRef] =>
        Seq((BigDecimal(90), args(0).asInstanceOf[Seq[ModuleRegistration]]))
      }

      student.mostSignificantCourse._moduleRegistrations.add(Fixtures.moduleRegistration(
        student.mostSignificantCourse,
        module1,
        BigDecimal(60).underlying,
        academicYear,
        agreedMark = Some(90)
      ))
      student.mostSignificantCourse._moduleRegistrations.add(Fixtures.moduleRegistration(
        student.mostSignificantCourse,
        module1,
        BigDecimal(30).underlying,
        academicYear,
        agreedMark = Some(40)
      ))

      val result: ProgressionResult = service.suggestedResult(entityYear3, 120, Map(), calculateYearMarks = false, groupByLevel = false, applyBenchmark = false, yearWeightings)
      result.description should be(ProgressionResult.Proceed.description)
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
      service.moduleRegistrationService.overcattedModuleSubsets(any[Seq[ModuleRegistration]], any[Map[Module, BigDecimal]], ArgumentMatchers.eq(BigDecimal(120)), ArgumentMatchers.eq(Seq())) returns Seq(
        (BigDecimal(30.0), Seq())
      )

      student.mostSignificantCourse._moduleRegistrations.add(Fixtures.moduleRegistration(
        student.mostSignificantCourse,
        module1,
        BigDecimal(30).underlying,
        academicYear,
        agreedMark = Some(100)
      ))

      val result: ProgressionResult = service.suggestedResult(entityYear3, 120, Map(), calculateYearMarks = false, groupByLevel = false, applyBenchmark = false, yearWeightings)
      result.description should be(ProgressionResult.Resit.description)
    }

    // Not passed enough credits
    new Fixture {
      scyd3.yearOfStudy = 2
      service.courseAndRouteService.getCourseYearWeighting(course.code, student.mostSignificantCourse.sprStartAcademicYear, 2) returns None
      service.moduleRegistrationService.findCoreRequiredModules(null, academicYear, 2) returns Seq(new CoreRequiredModule(null, null, 2, module1))
      service.moduleRegistrationService.weightedMeanYearMark(any[Seq[ModuleRegistration]], any[Map[Module, BigDecimal]], any[Boolean]) returns Right(BigDecimal(90))
      service.moduleRegistrationService.overcattedModuleSubsets(any[Seq[ModuleRegistration]], any[Map[Module, BigDecimal]], ArgumentMatchers.eq(BigDecimal(120)), ArgumentMatchers.eq(Seq())) returns Seq(
        (BigDecimal(90.0), Seq())
      )

      student.mostSignificantCourse._moduleRegistrations.add(Fixtures.moduleRegistration(
        student.mostSignificantCourse,
        module1,
        BigDecimal(30).underlying,
        academicYear,
        agreedMark = Some(90)
      ))

      val result: ProgressionResult = service.suggestedResult(entityYear3, 120, Map(), calculateYearMarks = false, groupByLevel = false, applyBenchmark = false, yearWeightings)
      result.description should be(ProgressionResult.Resit.description)
    }

    // All good
    new Fixture {
      scyd3.yearOfStudy = 2
      service.courseAndRouteService.getCourseYearWeighting(course.code, student.mostSignificantCourse.sprStartAcademicYear, 2) returns None
      service.moduleRegistrationService.findCoreRequiredModules(null, academicYear, 2) returns Seq(new CoreRequiredModule(null, null, 2, module1))
      service.moduleRegistrationService.weightedMeanYearMark(any[Seq[ModuleRegistration]], any[Map[Module, BigDecimal]], any[Boolean]) returns Right(BigDecimal(90))
      service.moduleRegistrationService.overcattedModuleSubsets(any[Seq[ModuleRegistration]], any[Map[Module, BigDecimal]], ArgumentMatchers.eq(BigDecimal(120)), ArgumentMatchers.eq(Seq())) returns Seq(
        (BigDecimal(90.0), Seq())
      )

      student.mostSignificantCourse._moduleRegistrations.add(Fixtures.moduleRegistration(
        student.mostSignificantCourse,
        module1,
        BigDecimal(60).underlying,
        academicYear,
        agreedMark = Some(90)
      ))
      student.mostSignificantCourse._moduleRegistrations.add(Fixtures.moduleRegistration(
        student.mostSignificantCourse,
        module1,
        BigDecimal(30).underlying,
        academicYear,
        agreedMark = Some(40)
      ))

      val result: ProgressionResult = service.suggestedResult(entityYear3, 120, Map(), calculateYearMarks = false, groupByLevel = false, applyBenchmark = false, yearWeightings)
      result.description should be(ProgressionResult.Proceed.description)
    }
  }

  @Test
  def suggestedFinalYearGradeNotFinalYear(): Unit = {
    new Fixture {
      scyd3.yearOfStudy = 2
      val result: FinalYearGrade = service.suggestedFinalYearGrade(entityYear3, 120, Map(), calculateYearMarks = false, groupByLevel = false, applyBenchmark = false, yearWeightings)
      result.description should be(FinalYearGrade.Ignore.description)
    }
  }

  trait ThreeYearStudentFixture extends Fixture {
    scyd3.yearOfStudy = 3
    val scyd1: StudentCourseYearDetails = Fixtures.studentCourseYearDetails()
    scyd1.modeOfAttendance = Fixtures.modeOfAttendance("F", "FT", "Full time")
    scyd1.studentCourseDetails = student.mostSignificantCourse
    scyd1.yearOfStudy = 1
    scyd1.academicYear = academicYear - 2
    val entityYear1: ExamGridEntityYear = scyd1.toExamGridEntityYear
    student.mostSignificantCourse.addStudentCourseYearDetails(scyd1)
    val scyd2: StudentCourseYearDetails = Fixtures.studentCourseYearDetails()
    scyd2.studentCourseDetails = student.mostSignificantCourse
    scyd2.yearOfStudy = 2
    scyd2.academicYear = academicYear - 1
    scyd2.modeOfAttendance = Fixtures.modeOfAttendance("F", "FT", "Full time")
    val entityYear2: ExamGridEntityYear = scyd2.toExamGridEntityYear
    student.mostSignificantCourse.addStudentCourseYearDetails(scyd2)
  }

  @Test
  def suggestedFinalYearGradeMissingYearMark(): Unit = {
    // No marks for any year
    new ThreeYearStudentFixture {
      service.courseAndRouteService.getCourseYearWeighting(course.code, student.mostSignificantCourse.sprStartAcademicYear, 3) returns None
      service.moduleRegistrationService.weightedMeanYearMark(any[Seq[ModuleRegistration]], any[Map[Module, BigDecimal]], any[Boolean]) returns Left("No mark for you")
      service.moduleRegistrationService.overcattedModuleSubsets(any[Seq[ModuleRegistration]], any[Map[Module, BigDecimal]], any[BigDecimal], any[Seq[UpstreamRouteRule]]) answers { args: Array[AnyRef] => Seq() }
      val result: FinalYearGrade = service.suggestedFinalYearGrade(entityYear3, 120, Map(), calculateYearMarks = false, groupByLevel = false, applyBenchmark = false, yearWeightings)
      result.description should be(FinalYearGrade.Unknown("").description)
      result.asInstanceOf[FinalYearGrade.Unknown].details should be("The final overall mark cannot be calculated because there is no mark for year 1, year 2, year 3")
    }

    // No mark for 2nd year
    new ThreeYearStudentFixture {
      service.courseAndRouteService.getCourseYearWeighting(course.code, student.mostSignificantCourse.sprStartAcademicYear, 3) returns None
      service.moduleRegistrationService.weightedMeanYearMark(any[Seq[ModuleRegistration]], any[Map[Module, BigDecimal]], any[Boolean]) returns Right(BigDecimal(90.0))
      service.moduleRegistrationService.overcattedModuleSubsets(any[Seq[ModuleRegistration]], any[Map[Module, BigDecimal]], any[BigDecimal], any[Seq[UpstreamRouteRule]]) answers { args: Array[AnyRef] =>
        Seq((BigDecimal(90.0), args(0).asInstanceOf[Seq[ModuleRegistration]]))
      }
      student.mostSignificantCourse.freshStudentCourseYearDetails.head.agreedMark = BigDecimal(90.0).underlying
      val result: FinalYearGrade = service.suggestedFinalYearGrade(entityYear3, 120, Map(), calculateYearMarks = false, groupByLevel = false, applyBenchmark = false, yearWeightings)
      result.description should be(FinalYearGrade.Unknown("").description)
      result.asInstanceOf[FinalYearGrade.Unknown].details should be("The final overall mark cannot be calculated because there is no mark for year 2")
    }

    // No mark for this year
    new ThreeYearStudentFixture {
      service.courseAndRouteService.getCourseYearWeighting(course.code, student.mostSignificantCourse.sprStartAcademicYear, 3) returns None
      service.moduleRegistrationService.weightedMeanYearMark(any[Seq[ModuleRegistration]], any[Map[Module, BigDecimal]], any[Boolean]) returns Left("No mark for you")
      service.moduleRegistrationService.overcattedModuleSubsets(any[Seq[ModuleRegistration]], any[Map[Module, BigDecimal]], any[BigDecimal], any[Seq[UpstreamRouteRule]]) answers { args: Array[AnyRef] => Seq() }
      student.mostSignificantCourse.freshStudentCourseYearDetails.head.agreedMark = BigDecimal(90.0).underlying
      student.mostSignificantCourse.freshStudentCourseYearDetails.tail.head.agreedMark = BigDecimal(90.0).underlying
      val result: FinalYearGrade = service.suggestedFinalYearGrade(entityYear3, 120, Map(), calculateYearMarks = false, groupByLevel = false, applyBenchmark = false, yearWeightings)
      result.description should be(FinalYearGrade.Unknown("").description)
      result.asInstanceOf[FinalYearGrade.Unknown].details should be("The final overall mark cannot be calculated because there is no mark for year 3")
    }
  }

  @Test
  def suggestedFinalYearGradeOvercat(): Unit = {
    trait TestFixture extends ThreeYearStudentFixture {
      service.moduleRegistrationService.weightedMeanYearMark(any[Seq[ModuleRegistration]], any[Map[Module, BigDecimal]], any[Boolean]) returns Right(BigDecimal(90.0))
      student.mostSignificantCourse.freshStudentCourseYearDetails.head.agreedMark = BigDecimal(90.0).underlying
      student.mostSignificantCourse.freshStudentCourseYearDetails.tail.head.agreedMark = BigDecimal(90.0).underlying
      service.courseAndRouteService.getCourseYearWeighting(ArgumentMatchers.eq(course.code), ArgumentMatchers.eq(student.mostSignificantCourse.sprStartAcademicYear), any[Int]) returns None

      val mr1: ModuleRegistration = Fixtures.moduleRegistration(
        student.mostSignificantCourse,
        module1,
        BigDecimal(60).underlying,
        academicYear,
        agreedMark = Some(90)
      )
      val mr2: ModuleRegistration = Fixtures.moduleRegistration(
        student.mostSignificantCourse,
        module2,
        BigDecimal(75).underlying,
        academicYear,
        agreedMark = Some(40)
      )
      student.mostSignificantCourse._moduleRegistrations.add(mr1)
      student.mostSignificantCourse._moduleRegistrations.add(mr2)
      scyd3.moduleAndDepartmentService = smartMock[ModuleAndDepartmentService]
      scyd3.moduleAndDepartmentService.getModuleByCode(module1.code) returns Option(module1)
      scyd3.moduleAndDepartmentService.getModuleByCode(module2.code) returns Option(module2)
      scyd3.moduleAndDepartmentService.getModulesByCodes(Seq(module1.code, module2.code)) returns Seq(module1, module2)
    }

    // Not overcat
    new TestFixture {
      service.moduleRegistrationService.overcattedModuleSubsets(any[Seq[ModuleRegistration]], any[Map[Module, BigDecimal]], ArgumentMatchers.eq(BigDecimal(180)), ArgumentMatchers.eq(Seq())) returns Seq(
        (BigDecimal(90.0), Seq(mr1, mr2))
      )
      val result: FinalYearGrade = service.suggestedFinalYearGrade(entityYear3, 180, Map(), calculateYearMarks = false, groupByLevel = false, applyBenchmark = false, Seq())
      result.description should be(FinalYearGrade.Unknown("").description)
      result.asInstanceOf[FinalYearGrade.Unknown].details.contains("Could not find year weightings") should be(true)
    }

    // Overcat, single subset
    new TestFixture {
      service.moduleRegistrationService.overcattedModuleSubsets(any[Seq[ModuleRegistration]], any[Map[Module, BigDecimal]], ArgumentMatchers.eq(BigDecimal(180)), ArgumentMatchers.eq(Seq())) returns Seq(
        (BigDecimal(90.0), Seq(mr1, mr2))
      ) // One subset

      val result: FinalYearGrade = service.suggestedFinalYearGrade(entityYear3, 180, Map(), calculateYearMarks = false, groupByLevel = false, applyBenchmark = false, Seq())
      // Should re-use the initally calculated mark as only 1 subset
      verify(service.moduleRegistrationService, times(1)).weightedMeanYearMark(any[Seq[ModuleRegistration]], any[Map[Module, BigDecimal]], any[Boolean])
      result.description should be(FinalYearGrade.Unknown("").description)
      result.asInstanceOf[FinalYearGrade.Unknown].details.contains("Could not find year weightings") should be(true)
    }

    // Overcat, 2 subsets, none chosen
    new TestFixture {
      val mr3: ModuleRegistration = Fixtures.moduleRegistration(
        student.mostSignificantCourse,
        module2,
        BigDecimal(60).underlying,
        academicYear,
        agreedMark = Some(50)
      )
      student.mostSignificantCourse._moduleRegistrations.add(mr3)
      service.moduleRegistrationService.overcattedModuleSubsets(any[Seq[ModuleRegistration]], any[Map[Module, BigDecimal]], ArgumentMatchers.eq(BigDecimal(180)), ArgumentMatchers.eq(Seq())) returns Seq(
        (BigDecimal(90.0), Seq(mr1, mr2)),
        (BigDecimal(90.0), Seq(mr1, mr3))
      ) // Two subsets

      val result: FinalYearGrade = service.suggestedFinalYearGrade(entityYear3, 180, Map(), calculateYearMarks = false, groupByLevel = false, applyBenchmark = false, Seq())
      result.description should be(FinalYearGrade.Unknown("").description)
      result.asInstanceOf[FinalYearGrade.Unknown].details should be("The final overall mark cannot be calculated because there is no mark for year 3")
    }

    // Overcat, 2 subsets, 1 chosen
    new TestFixture {
      val mr3: ModuleRegistration = Fixtures.moduleRegistration(
        student.mostSignificantCourse,
        module2,
        BigDecimal(60).underlying,
        academicYear,
        agreedMark = Some(50)
      )
      student.mostSignificantCourse._moduleRegistrations.add(mr3)
      service.moduleRegistrationService.overcattedModuleSubsets(any[Seq[ModuleRegistration]], any[Map[Module, BigDecimal]], ArgumentMatchers.eq(BigDecimal(180)), ArgumentMatchers.eq(Seq())) returns Seq(
        (BigDecimal(90.0), Seq(mr1, mr2)),
        (BigDecimal(90.0), Seq(mr1, mr3))
      ) // Two subsets
      scyd3.overcattingModules = Seq(module1, module2) // Subset chosen

      val result: FinalYearGrade = service.suggestedFinalYearGrade(entityYear3, 180, Map(), calculateYearMarks = false, groupByLevel = false, applyBenchmark = false, Seq())
      result.description should be(FinalYearGrade.Unknown("").description)
      result.asInstanceOf[FinalYearGrade.Unknown].details.contains("Could not find year weightings") should be(true)
    }
  }

  @Test
  def suggestedFinalYearGradeMissingYearWeightings(): Unit = {
    // No weightings for any year
    new ThreeYearStudentFixture {
      service.moduleRegistrationService.weightedMeanYearMark(any[Seq[ModuleRegistration]], any[Map[Module, BigDecimal]], any[Boolean]) returns Right(BigDecimal(90.0))
      student.mostSignificantCourse.freshStudentCourseYearDetails.head.agreedMark = BigDecimal(90.0).underlying
      student.mostSignificantCourse.freshStudentCourseYearDetails.tail.head.agreedMark = BigDecimal(90.0).underlying
      service.moduleRegistrationService.overcattedModuleSubsets(any[Seq[ModuleRegistration]], any[Map[Module, BigDecimal]], ArgumentMatchers.eq(BigDecimal(180)), ArgumentMatchers.eq(Seq())) returns Seq(
        (BigDecimal(90.0), Seq())
      )

      service.courseAndRouteService.getCourseYearWeighting(course.code, student.mostSignificantCourse.sprStartAcademicYear, 1) returns None
      service.courseAndRouteService.getCourseYearWeighting(course.code, student.mostSignificantCourse.sprStartAcademicYear, 2) returns None
      service.courseAndRouteService.getCourseYearWeighting(course.code, student.mostSignificantCourse.sprStartAcademicYear, 3) returns None
      val result: FinalYearGrade = service.suggestedFinalYearGrade(entityYear3, 180, Map(), calculateYearMarks = false, groupByLevel = false, applyBenchmark = false, Seq())
      result.description should be(FinalYearGrade.Unknown("").description)
      result.asInstanceOf[FinalYearGrade.Unknown].details.contains("Could not find year weightings") should be(true)
    }

    // No weighting for 2nd year
    new ThreeYearStudentFixture {
      service.moduleRegistrationService.weightedMeanYearMark(any[Seq[ModuleRegistration]], any[Map[Module, BigDecimal]], any[Boolean]) returns Right(BigDecimal(90.0))
      student.mostSignificantCourse.freshStudentCourseYearDetails.head.agreedMark = BigDecimal(90.0).underlying
      student.mostSignificantCourse.freshStudentCourseYearDetails.tail.head.agreedMark = BigDecimal(90.0).underlying
      service.moduleRegistrationService.overcattedModuleSubsets(any[Seq[ModuleRegistration]], any[Map[Module, BigDecimal]], ArgumentMatchers.eq(BigDecimal(180)), ArgumentMatchers.eq(Seq())) returns Seq(
        (BigDecimal(90.0), Seq())
      )
      val year1Weighting: CourseYearWeighting = Fixtures.yearWeighting(course, new JBigDecimal(20), student.mostSignificantCourse.sprStartAcademicYear, 1)
      val year3Weighting: CourseYearWeighting = Fixtures.yearWeighting(course, new JBigDecimal(40), student.mostSignificantCourse.sprStartAcademicYear, 3)


      service.courseAndRouteService.getCourseYearWeighting(course.code, student.mostSignificantCourse.sprStartAcademicYear, 1) returns Some(year1Weighting)
      service.courseAndRouteService.getCourseYearWeighting(course.code, student.mostSignificantCourse.sprStartAcademicYear, 2) returns None
      service.courseAndRouteService.getCourseYearWeighting(course.code, student.mostSignificantCourse.sprStartAcademicYear, 3) returns Some(year3Weighting)
      val result: FinalYearGrade = service.suggestedFinalYearGrade(entityYear3, 180, Map(), calculateYearMarks = false, groupByLevel = false, applyBenchmark = false, Seq(year1Weighting, year3Weighting))
      result.description should be(FinalYearGrade.Unknown("").description)
      result.asInstanceOf[FinalYearGrade.Unknown].details.contains(s"Could not find year weightings for: ${course.code.toUpperCase} ${student.mostSignificantCourse.sprStartAcademicYear.toString} Year 2")
    }
  }

  trait ThreeYearStudentWithMarksAndYearWeightingsFixture extends ThreeYearStudentFixture {
    student.mostSignificantCourse.freshStudentCourseYearDetails.head.agreedMark = BigDecimal(65.0).underlying
    student.mostSignificantCourse.freshStudentCourseYearDetails.tail.head.agreedMark = BigDecimal(60.0).underlying
    service.moduleRegistrationService.weightedMeanYearMark(any[Seq[ModuleRegistration]], any[Map[Module, BigDecimal]], any[Boolean]) returns Right(BigDecimal(80.0))
    service.moduleRegistrationService.overcattedModuleSubsets(any[Seq[ModuleRegistration]], any[Map[Module, BigDecimal]], any[BigDecimal], any[Seq[UpstreamRouteRule]]) answers { args: Array[AnyRef] =>
      Seq((BigDecimal(80.0), args(0).asInstanceOf[Seq[ModuleRegistration]]))
    }

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
        agreedMark = None
      )
      val mr2: ModuleRegistration = Fixtures.moduleRegistration(
        student.mostSignificantCourse,
        module2,
        BigDecimal(60).underlying,
        scyd2.academicYear,
        agreedMark = Some(90)
      )
      student.mostSignificantCourse._moduleRegistrations.add(mr1)
      student.mostSignificantCourse._moduleRegistrations.add(mr2)
      val result: FinalYearGrade = service.suggestedFinalYearGrade(entityYear3, 180, Map(), calculateYearMarks = false, groupByLevel = false, applyBenchmark = false, yearWeightings)
      result.description should be(FinalYearGrade.Unknown("").description)
      result.asInstanceOf[FinalYearGrade.Unknown].details.contains(module1.code.toUpperCase) should be(true)
    }

    // Not enough credits passed in 2nd year
    new ThreeYearStudentWithMarksAndYearWeightingsFixture {
      val mr1: ModuleRegistration = Fixtures.moduleRegistration(
        student.mostSignificantCourse,
        module1,
        BigDecimal(40).underlying,
        scyd2.academicYear,
        agreedMark = Some(90)
      )
      val mr2: ModuleRegistration = Fixtures.moduleRegistration(
        student.mostSignificantCourse,
        module2,
        BigDecimal(40).underlying,
        scyd2.academicYear,
        agreedMark = Some(35) // Failed in 2nd year
      )
      val mr3: ModuleRegistration = Fixtures.moduleRegistration(
        student.mostSignificantCourse,
        module1,
        BigDecimal(40).underlying,
        scyd3.academicYear,
        agreedMark = Some(90)
      )
      val mr4: ModuleRegistration = Fixtures.moduleRegistration(
        student.mostSignificantCourse,
        module2,
        BigDecimal(60).underlying,
        scyd3.academicYear,
        agreedMark = Some(90)
      )
      student.mostSignificantCourse._moduleRegistrations.add(mr1)
      student.mostSignificantCourse._moduleRegistrations.add(mr2)
      student.mostSignificantCourse._moduleRegistrations.add(mr3)
      student.mostSignificantCourse._moduleRegistrations.add(mr4)
      val result: FinalYearGrade = service.suggestedFinalYearGrade(entityYear3, 180, Map(), calculateYearMarks = false, groupByLevel = false, applyBenchmark = false, yearWeightings)
      result match {
        case withMark: FinalYearMark => withMark.mark should be(BigDecimal(69))
        case _ => Assertions.fail("Incorrect type returned")
      }
      result.description should be(FinalYearGrade.Undergraduate.Fail.description)
    }

    // Not enough credits passed in final year
    new ThreeYearStudentWithMarksAndYearWeightingsFixture {
      val mr1: ModuleRegistration = Fixtures.moduleRegistration(
        student.mostSignificantCourse,
        module1,
        BigDecimal(40).underlying,
        scyd2.academicYear,
        agreedMark = Some(90)
      )
      val mr2: ModuleRegistration = Fixtures.moduleRegistration(
        student.mostSignificantCourse,
        module2,
        BigDecimal(40).underlying,
        scyd2.academicYear,
        agreedMark = Some(90)
      )
      val mr3: ModuleRegistration = Fixtures.moduleRegistration(
        student.mostSignificantCourse,
        module1,
        BigDecimal(40).underlying,
        scyd3.academicYear,
        agreedMark = Some(90)
      )
      val mr4: ModuleRegistration = Fixtures.moduleRegistration(
        student.mostSignificantCourse,
        module2,
        BigDecimal(60).underlying,
        scyd3.academicYear,
        agreedMark = Some(35) // Failed in final year
      )
      student.mostSignificantCourse._moduleRegistrations.add(mr1)
      student.mostSignificantCourse._moduleRegistrations.add(mr2)
      student.mostSignificantCourse._moduleRegistrations.add(mr3)
      student.mostSignificantCourse._moduleRegistrations.add(mr4)
      val result: FinalYearGrade = service.suggestedFinalYearGrade(entityYear3, 180, Map(), calculateYearMarks = false, groupByLevel = false, applyBenchmark = false, yearWeightings)
      result match {
        case withMark: FinalYearMark => withMark.mark should be(BigDecimal(69))
        case _ => Assertions.fail("Incorrect type returned")
      }
      result.description should be(FinalYearGrade.Undergraduate.Fail.description)
    }

    // All good
    new ThreeYearStudentWithMarksAndYearWeightingsFixture {
      val mr1: ModuleRegistration = Fixtures.moduleRegistration(
        student.mostSignificantCourse,
        module1,
        BigDecimal(40).underlying,
        scyd2.academicYear,
        agreedMark = Some(90)
      )
      val mr2: ModuleRegistration = Fixtures.moduleRegistration(
        student.mostSignificantCourse,
        module2,
        BigDecimal(40).underlying,
        scyd2.academicYear,
        agreedMark = Some(90)
      )
      val mr3: ModuleRegistration = Fixtures.moduleRegistration(
        student.mostSignificantCourse,
        module1,
        BigDecimal(40).underlying,
        scyd3.academicYear,
        agreedMark = Some(90)
      )
      val mr4: ModuleRegistration = Fixtures.moduleRegistration(
        student.mostSignificantCourse,
        module2,
        BigDecimal(60).underlying,
        scyd3.academicYear,
        agreedMark = Some(90)
      )
      student.mostSignificantCourse._moduleRegistrations.add(mr1)
      student.mostSignificantCourse._moduleRegistrations.add(mr2)
      student.mostSignificantCourse._moduleRegistrations.add(mr3)
      student.mostSignificantCourse._moduleRegistrations.add(mr4)
      val result: FinalYearGrade = service.suggestedFinalYearGrade(entityYear3, 180, Map(), calculateYearMarks = false, groupByLevel = false, applyBenchmark = false, yearWeightings)
      // 1st year: 65 @ 20%, 2nd year: 60 @ 40%, 3rd year 80 @ 40%
      // Final grade 69 = 2.1, but on the grade boundary so borderline
      result match {
        case withMark: FinalYearMark => withMark.mark should be(BigDecimal(69))
        case _ => Assertions.fail("Incorrect type returned")
      }
      result.description should be(FinalYearGrade.Undergraduate.UpperSecondBorderline.description)
    }
  }

  @Test
  def suggestedFinalYearGradePostgraduate(): Unit = {
    val student: StudentMember = Fixtures.student("1234")
    student.mostSignificantCourse.course = Fixtures.course("TPOS-ITS1")
    student.mostSignificantCourse.sprStartAcademicYear = academicYear
    val scyd: StudentCourseYearDetails = student.mostSignificantCourse.latestStudentCourseYearDetails
    scyd.academicYear = academicYear
    student.mostSignificantCourse.courseYearLength = 1
    scyd.modeOfAttendance = Fixtures.modeOfAttendance("F", "FT", "Full time")

    val service = new AbstractProgressionService with ModuleRegistrationServiceComponent with CourseAndRouteServiceComponent {
      override val moduleRegistrationService: ModuleRegistrationService = smartMock[ModuleRegistrationService]
      override val courseAndRouteService: CourseAndRouteService = smartMock[CourseAndRouteService]
    }

    def entityYear: ExamGridEntityYear = scyd.toExamGridEntityYear

    val yearWeighting: CourseYearWeighting = Fixtures.yearWeighting(course, new JBigDecimal(100), student.mostSignificantCourse.sprStartAcademicYear, 1)
    service.moduleRegistrationService.overcattedModuleSubsets(any[Seq[ModuleRegistration]], any[Map[Module, BigDecimal]], any[BigDecimal], any[Seq[UpstreamRouteRule]]) answers { args: Array[AnyRef] => Seq() }

    student.mostSignificantCourse._moduleRegistrations.add(Fixtures.moduleRegistration(
      scd = student.mostSignificantCourse,
      mod = module1,
      cats = BigDecimal(40).underlying,
      year = scyd.academicYear,
      agreedMark = Some(70)
    ))
    student.mostSignificantCourse._moduleRegistrations.add(Fixtures.moduleRegistration(
      scd = student.mostSignificantCourse,
      mod = module2,
      cats = BigDecimal(40).underlying,
      year = scyd.academicYear,
      agreedMark = Some(70)
    ))
    service.moduleRegistrationService.weightedMeanYearMark(any[Seq[ModuleRegistration]], any[Map[Module, BigDecimal]], any[Boolean]) returns Right(BigDecimal(70.0))
    service.courseAndRouteService.getCourseYearWeighting(course.code, student.mostSignificantCourse.sprStartAcademicYear, 1) returns Option(yearWeighting)
    val result: FinalYearGrade = service.suggestedFinalYearGrade(entityYear, 70, Map(), calculateYearMarks = false, groupByLevel = false, applyBenchmark = false, weightings = Seq(yearWeighting))
    result match {
      case withMark: FinalYearMark => withMark.mark should be(BigDecimal(70))
      case _ => Assertions.fail("Incorrect type returned")
    }
    result should be(FinalYearGrade.Postgraduate.Distinction)
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
        agreedMark = Some(90)
      )
      val mr2: ModuleRegistration = Fixtures.moduleRegistration(
        student.mostSignificantCourse,
        module2,
        BigDecimal(40).underlying,
        scyd1.academicYear,
        agreedMark = Some(90)
      )
      val mr3: ModuleRegistration = Fixtures.moduleRegistration(
        student.mostSignificantCourse,
        module1,
        BigDecimal(40).underlying,
        scyd3.academicYear,
        agreedMark = Some(90)
      )
      val mr4: ModuleRegistration = Fixtures.moduleRegistration(
        student.mostSignificantCourse,
        module2,
        BigDecimal(60).underlying,
        scyd3.academicYear,
        agreedMark = Some(90)
      )
      student.mostSignificantCourse._moduleRegistrations.add(mr1)
      student.mostSignificantCourse._moduleRegistrations.add(mr2)
      student.mostSignificantCourse._moduleRegistrations.add(mr3)
      student.mostSignificantCourse._moduleRegistrations.add(mr4)
      val result: FinalYearGrade = service.suggestedFinalYearGrade(entityYear3, 180, Map(), calculateYearMarks = false, groupByLevel = false, applyBenchmark = false, Seq(year1Weighting, year2Weighting, year3Weighting))
      // 1st year: 65 @ 70%, 2nd year: 60 @ 0%, 3rd year 80 @ 30%
      // Final grade 69.5 = 2.1, but on the grade boundary so borderline
      result match {
        case withMark: FinalYearMark => withMark.mark should be(BigDecimal(69.5))
        case _ => Assertions.fail("Incorrect type returned")
      }
      result.description should be(FinalYearGrade.Undergraduate.UpperSecondBorderline.description)
    }
  }

  @Test
  def getYearMark(): Unit = {
    new ThreeYearStudentWithMarksAndYearWeightingsFixture {
      val year1Result: Either[String, BigDecimal] = service.getYearMark(entityYear1, 180, Nil, yearWeightings)
      year1Result should be(Right(BigDecimal(80.0)))

      val year2Result: Either[String, BigDecimal] = service.getYearMark(entityYear2, 180, Nil, yearWeightings)
      year2Result should be(Right(BigDecimal(80.0)))

      val year3Result: Either[String, BigDecimal] = service.getYearMark(entityYear3, 180, Nil, yearWeightings)
      year3Result should be(Right(BigDecimal(80.0)))
    }

    new Fixture {
      service.courseAndRouteService.getCourseYearWeighting(course.code, student.mostSignificantCourse.sprStartAcademicYear, 1) returns None
      service.moduleRegistrationService.weightedMeanYearMark(any[Seq[ModuleRegistration]], any[Map[Module, BigDecimal]], any[Boolean]) returns Right(BigDecimal(30))
      service.moduleRegistrationService.overcattedModuleSubsets(any[Seq[ModuleRegistration]], any[Map[Module, BigDecimal]], any[BigDecimal], any[Seq[UpstreamRouteRule]]) answers { args: Array[AnyRef] => Seq() }

      val result: Either[String, BigDecimal] = service.getYearMark(entityYear3, 180, Nil, yearWeightings)
      result should be(Right(BigDecimal(30)))
    }
  }

  @Test
  def allowEmptyYearMarks(): Unit = {
    // full time student with no year abroad
    new ThreeYearStudentFixture {
      var allowEmpty = ProgressionService.allowEmptyYearMarks(yearWeightings, entityYear3)
      allowEmpty should be(false)
    }

    // An year abroad - 2nd year  with null block occurrence
    new ThreeYearStudentFixture {
      scyd2.modeOfAttendance = Fixtures.modeOfAttendance("SWE", "SANDWICH (E)", "Sandwich (thick) Erasmus Scheme")
      var allowEmpty = ProgressionService.allowEmptyYearMarks(yearWeightings, entityYear2)
      allowEmpty should be(true)
    }

    // An year abroad - 2nd year
    new ThreeYearStudentFixture {
      scyd2.modeOfAttendance = Fixtures.modeOfAttendance("YO", "OPTIONAL YR", "Optional year out (study related)")
      scyd2.blockOccurrence = "FW" //  SITS block occurrence
      var allowEmpty = ProgressionService.allowEmptyYearMarks(yearWeightings, entityYear2)
      allowEmpty should be(true)
    }

    // Intercalated year (this should not consider year abroad even though we have one of the valid MOA codes that is acceptable for year abroad)
    new ThreeYearStudentFixture {
      scyd2.modeOfAttendance = Fixtures.modeOfAttendance("YM", "COMP YR", "Compulsory year out (study related)")
      scyd2.blockOccurrence = "I"
      var allowEmpty = ProgressionService.allowEmptyYearMarks(yearWeightings, entityYear2)
      allowEmpty should be(false)
    }

    // Intercalated year (this should not consider year abroad)
    new ThreeYearStudentFixture {
      scyd2.modeOfAttendance = Fixtures.modeOfAttendance("YX", "EXCH YR", "Exchange year out (study related)")
      scyd2.blockOccurrence = "I"
      var allowEmpty = ProgressionService.allowEmptyYearMarks(yearWeightings, entityYear2)
      allowEmpty should be(false)
    }

  }

  @Test
  def postgraduateBenchmark(): Unit = {
    new Fixture {
      val scd: StudentCourseDetails = Fixtures.student().mostSignificantCourse
      val academicYear: AcademicYear = AcademicYear(2019)
      scd.latestStudentCourseYearDetails.academicYear = academicYear
      val department: Department = Fixtures.department("in")

      // examples from https://warwick.ac.uk/insite/coronavirus/staff/teaching/policyguidance/pgt/examples/

      val pgtScd: StudentCourseDetails = Fixtures.studentCourseDetails(Fixtures.student(), department)
      pgtScd.award = new Award("MSC")
      val pgtScyd: StudentCourseYearDetails = Fixtures.studentCourseYearDetails(studentCourseDetails = pgtScd)

      val exampleA: Seq[ModuleRegistration] = Random.shuffle(Seq(
        Fixtures.moduleRegistration(scd, Fixtures.module("ax931"), BigDecimal(10).underlying, academicYear, "", Some(91), ModuleSelectionStatus.Core),
        Fixtures.moduleRegistration(scd, Fixtures.module("ax924"), BigDecimal(10).underlying, academicYear, "", Some(74), ModuleSelectionStatus.Core),
        Fixtures.moduleRegistration(scd, Fixtures.module("ax9ba"), BigDecimal(10).underlying, academicYear, "", Some(74), ModuleSelectionStatus.Core),
        Fixtures.moduleRegistration(scd, Fixtures.module("ax91v"), BigDecimal(10).underlying, academicYear, "", Some(73), ModuleSelectionStatus.Core),
        Fixtures.moduleRegistration(scd, Fixtures.module("ax93p"), BigDecimal(10).underlying, academicYear, "", Some(73), ModuleSelectionStatus.Core),
        Fixtures.moduleRegistration(scd, Fixtures.module("ax423"), BigDecimal(10).underlying, academicYear, "", Some(72), ModuleSelectionStatus.Core),
        Fixtures.moduleRegistration(scd, Fixtures.module("ax95k"), BigDecimal(10).underlying, academicYear, "", Some(72), ModuleSelectionStatus.Core),
        Fixtures.moduleRegistration(scd, Fixtures.module("ax92t"), BigDecimal(10).underlying, academicYear, "", Some(72), ModuleSelectionStatus.Core),
        Fixtures.moduleRegistration(scd, Fixtures.module("ax9t7"), BigDecimal(10).underlying, academicYear, "", Some(69), ModuleSelectionStatus.Core),
        Fixtures.moduleRegistration(scd, Fixtures.module("ax98p"), BigDecimal(50).underlying, academicYear, "", Some(68), ModuleSelectionStatus.Core),
        Fixtures.moduleRegistration(scd, Fixtures.module("ax956"), BigDecimal(10).underlying, academicYear, "", Some(68), ModuleSelectionStatus.Core),
        Fixtures.moduleRegistration(scd, Fixtures.module("ax9p2"), BigDecimal(10).underlying, academicYear, "", Some(68), ModuleSelectionStatus.Core),
        Fixtures.moduleRegistration(scd, Fixtures.module("ax9p7"), BigDecimal(10).underlying, academicYear, "", Some(66), ModuleSelectionStatus.Core),
        Fixtures.moduleRegistration(scd, Fixtures.module("ax95l"), BigDecimal(10).underlying, academicYear, "", Some(60), ModuleSelectionStatus.Core),
      ))
      service.postgraduateBenchmark(pgtScyd, exampleA) should be(BigDecimal(72.1))


      val exampleB: Seq[ModuleRegistration] = Random.shuffle(Seq(
        Fixtures.moduleRegistration(scd, Fixtures.module("ax9t8"), BigDecimal(10).underlying, academicYear, "", Some(71), ModuleSelectionStatus.Core),
        Fixtures.moduleRegistration(scd, Fixtures.module("ax98p"), BigDecimal(50).underlying, academicYear, "", Some(68), ModuleSelectionStatus.Core),
        Fixtures.moduleRegistration(scd, Fixtures.module("ax9p4"), BigDecimal(10).underlying, academicYear, "", Some(68), ModuleSelectionStatus.Core),
        Fixtures.moduleRegistration(scd, Fixtures.module("ax9f7"), BigDecimal(10).underlying, academicYear, "", Some(66), ModuleSelectionStatus.Core),
        Fixtures.moduleRegistration(scd, Fixtures.module("ax984"), BigDecimal(10).underlying, academicYear, "", Some(65), ModuleSelectionStatus.Core),
        Fixtures.moduleRegistration(scd, Fixtures.module("ax926"), BigDecimal(10).underlying, academicYear, "", Some(63), ModuleSelectionStatus.Core),
        Fixtures.moduleRegistration(scd, Fixtures.module("ax9l8"), BigDecimal(10).underlying, academicYear, "", Some(61), ModuleSelectionStatus.Core),
        Fixtures.moduleRegistration(scd, Fixtures.module("ax3m7"), BigDecimal(10).underlying, academicYear, "", Some(60), ModuleSelectionStatus.Core),
        Fixtures.moduleRegistration(scd, Fixtures.module("ax963"), BigDecimal(10).underlying, academicYear, "", Some(60), ModuleSelectionStatus.Core),
        Fixtures.moduleRegistration(scd, Fixtures.module("ax9w7"), BigDecimal(10).underlying, academicYear, "", Some(57), ModuleSelectionStatus.Core),
        Fixtures.moduleRegistration(scd, Fixtures.module("ax9e5"), BigDecimal(10).underlying, academicYear, "", Some(57), ModuleSelectionStatus.Core),
        Fixtures.moduleRegistration(scd, Fixtures.module("ax932"), BigDecimal(10).underlying, academicYear, "", Some(57), ModuleSelectionStatus.Core),
        Fixtures.moduleRegistration(scd, Fixtures.module("ax9ba"), BigDecimal(10).underlying, academicYear, "", Some(57), ModuleSelectionStatus.Core),
        Fixtures.moduleRegistration(scd, Fixtures.module("ax97g"), BigDecimal(10).underlying, academicYear, "", Some(46), ModuleSelectionStatus.Core),
      ))
      service.postgraduateBenchmark(pgtScyd, exampleB) should be(BigDecimal(66.2))

      val exampleC: Seq[ModuleRegistration] = Random.shuffle(Seq(
        Fixtures.moduleRegistration(scd, Fixtures.module("ax9p8"), BigDecimal(10).underlying, academicYear, "", Some(73), ModuleSelectionStatus.Core),
        Fixtures.moduleRegistration(scd, Fixtures.module("ax94r"), BigDecimal(15).underlying, academicYear, "", Some(71), ModuleSelectionStatus.Core),
        Fixtures.moduleRegistration(scd, Fixtures.module("ax933"), BigDecimal(15).underlying, academicYear, "", Some(70), ModuleSelectionStatus.Core),
        Fixtures.moduleRegistration(scd, Fixtures.module("ax994"), BigDecimal(15).underlying, academicYear, "", Some(68), ModuleSelectionStatus.Core),
        Fixtures.moduleRegistration(scd, Fixtures.module("ax982"), BigDecimal(10).underlying, academicYear, "", Some(68), ModuleSelectionStatus.Core),
        Fixtures.moduleRegistration(scd, Fixtures.module("ax97l"), BigDecimal(15).underlying, academicYear, "", Some(66), ModuleSelectionStatus.Core),
        Fixtures.moduleRegistration(scd, Fixtures.module("ax947"), BigDecimal(15).underlying, academicYear, "", Some(60), ModuleSelectionStatus.Core),
        Fixtures.moduleRegistration(scd, Fixtures.module("ax94z"), BigDecimal(60).underlying, academicYear, "", Some(56), ModuleSelectionStatus.Core),
        Fixtures.moduleRegistration(scd, Fixtures.module("ax9ep"), BigDecimal(15).underlying, academicYear, "", Some(56), ModuleSelectionStatus.Core),
        Fixtures.moduleRegistration(scd, Fixtures.module("ax9m3"), BigDecimal(10).underlying, academicYear, "", Some(56), ModuleSelectionStatus.Core),
      ))
      service.postgraduateBenchmark(pgtScyd, exampleC) should be(BigDecimal(63.2))

      val exampleD: Seq[ModuleRegistration] = Random.shuffle(Seq(
        Fixtures.moduleRegistration(scd, Fixtures.module("ax986"), BigDecimal(15).underlying, academicYear, "", Some(62), ModuleSelectionStatus.Core),
        Fixtures.moduleRegistration(scd, Fixtures.module("ax979"), BigDecimal(15).underlying, academicYear, "", Some(61), ModuleSelectionStatus.Core),
        Fixtures.moduleRegistration(scd, Fixtures.module("ax95j"), BigDecimal(15).underlying, academicYear, "", Some(57), ModuleSelectionStatus.Core),
        Fixtures.moduleRegistration(scd, Fixtures.module("ax9e9"), BigDecimal(60).underlying, academicYear, "", Some(50), ModuleSelectionStatus.Core),
        Fixtures.moduleRegistration(scd, Fixtures.module("ax97k"), BigDecimal(15).underlying, academicYear, "", Some(50), ModuleSelectionStatus.Core),
        Fixtures.moduleRegistration(scd, Fixtures.module("ax94r"), BigDecimal(15).underlying, academicYear, "", Some(50), ModuleSelectionStatus.Core),
        Fixtures.moduleRegistration(scd, Fixtures.module("ax98y"), BigDecimal(15).underlying, academicYear, "", Some(50), ModuleSelectionStatus.Core),
        Fixtures.moduleRegistration(scd, Fixtures.module("ax957"), BigDecimal(15).underlying, academicYear, "", Some(50), ModuleSelectionStatus.Core),
        Fixtures.moduleRegistration(scd, Fixtures.module("ax94n"), BigDecimal(15).underlying, academicYear, "", Some(50), ModuleSelectionStatus.Core),
      ))
      service.postgraduateBenchmark(pgtScyd, exampleD) should be(BigDecimal(53.8))

      val exampleE: Seq[ModuleRegistration] = Random.shuffle(Seq(
        Fixtures.moduleRegistration(scd, Fixtures.module("ax92b"), BigDecimal(15).underlying, academicYear, "", Some(70), ModuleSelectionStatus.Core),
        Fixtures.moduleRegistration(scd, Fixtures.module("ax974"), BigDecimal(15).underlying, academicYear, "", Some(65), ModuleSelectionStatus.Core),
        Fixtures.moduleRegistration(scd, Fixtures.module("ax95p"), BigDecimal(60).underlying, academicYear, "", Some(62), ModuleSelectionStatus.Core),
        Fixtures.moduleRegistration(scd, Fixtures.module("ax9w7"), BigDecimal(15).underlying, academicYear, "", Some(56), ModuleSelectionStatus.Core),
        Fixtures.moduleRegistration(scd, Fixtures.module("ax9s4"), BigDecimal(15).underlying, academicYear, "", Some(50), ModuleSelectionStatus.Core),
        Fixtures.moduleRegistration(scd, Fixtures.module("ax966"), BigDecimal(15).underlying, academicYear, "", Some(50), ModuleSelectionStatus.Core),
        Fixtures.moduleRegistration(scd, Fixtures.module("ax9dw"), BigDecimal(15).underlying, academicYear, "", Some(50), ModuleSelectionStatus.Core),
        Fixtures.moduleRegistration(scd, Fixtures.module("ax9r4"), BigDecimal(15).underlying, academicYear, "", Some(50), ModuleSelectionStatus.Core),
        Fixtures.moduleRegistration(scd, Fixtures.module("ax9vp"), BigDecimal(15).underlying, academicYear, "", Some(50), ModuleSelectionStatus.Core),
      ))
      service.postgraduateBenchmark(pgtScyd, exampleE) should be(BigDecimal(61.1))

      val exampleF: Seq[ModuleRegistration] = Random.shuffle(Seq(
        Fixtures.moduleRegistration(scd, Fixtures.module("ax92p"), BigDecimal(15).underlying, academicYear, "", Some(100), ModuleSelectionStatus.Core),
        Fixtures.moduleRegistration(scd, Fixtures.module("ax98y"), BigDecimal(15).underlying, academicYear, "", Some(76), ModuleSelectionStatus.Core),
        Fixtures.moduleRegistration(scd, Fixtures.module("ax92w"), BigDecimal(15).underlying, academicYear, "", Some(75), ModuleSelectionStatus.Core),
        Fixtures.moduleRegistration(scd, Fixtures.module("ax9t2"), BigDecimal(45).underlying, academicYear, "", Some(70), ModuleSelectionStatus.Core),
        Fixtures.moduleRegistration(scd, Fixtures.module("ax93n"), BigDecimal(15).underlying, academicYear, "", Some(62), ModuleSelectionStatus.Core),
        Fixtures.moduleRegistration(scd, Fixtures.module("ax9w4"), BigDecimal(15).underlying, academicYear, "", Some(62), ModuleSelectionStatus.Core),
        Fixtures.moduleRegistration(scd, Fixtures.module("ax9u0"), BigDecimal(15).underlying, academicYear, "", Some(60), ModuleSelectionStatus.Core),
        Fixtures.moduleRegistration(scd, Fixtures.module("ax93y"), BigDecimal(15).underlying, academicYear, "", Some(54), ModuleSelectionStatus.Core),
        Fixtures.moduleRegistration(scd, Fixtures.module("ax988"), BigDecimal(15).underlying, academicYear, "", Some(54), ModuleSelectionStatus.Core),
        Fixtures.moduleRegistration(scd, Fixtures.module("ax92m"), BigDecimal(15).underlying, academicYear, "", Some(53), ModuleSelectionStatus.Core),
      ))
      service.postgraduateBenchmark(pgtScyd, exampleF) should be(BigDecimal(73.1))


      val pgdipScd: StudentCourseDetails = Fixtures.studentCourseDetails(Fixtures.student(), department)
      pgdipScd.award = new Award("PGDIP") // MSC
      val pgdipScyd: StudentCourseYearDetails = Fixtures.studentCourseYearDetails(studentCourseDetails = pgdipScd)

      val exampleG: Seq[ModuleRegistration] = Random.shuffle(Seq(
        Fixtures.moduleRegistration(scd, Fixtures.module("ax92p"), BigDecimal(15).underlying, academicYear, "", Some(100), ModuleSelectionStatus.Core),
        Fixtures.moduleRegistration(scd, Fixtures.module("ax98y"), BigDecimal(15).underlying, academicYear, "", Some(76), ModuleSelectionStatus.Core),
        Fixtures.moduleRegistration(scd, Fixtures.module("ax92w"), BigDecimal(15).underlying, academicYear, "", Some(75), ModuleSelectionStatus.Core),
        Fixtures.moduleRegistration(scd, Fixtures.module("ax9t2"), BigDecimal(45).underlying, academicYear, "", Some(70), ModuleSelectionStatus.Core),
        Fixtures.moduleRegistration(scd, Fixtures.module("ax93n"), BigDecimal(15).underlying, academicYear, "", Some(62), ModuleSelectionStatus.Core),
        Fixtures.moduleRegistration(scd, Fixtures.module("ax9w4"), BigDecimal(15).underlying, academicYear, "", Some(62), ModuleSelectionStatus.Core),
      ))
      service.postgraduateBenchmark(pgdipScyd, exampleG) should be(BigDecimal(76.8))
    }
  }

  class FixtureWithYearsAndMarks(marks: Seq[BigDecimal], yearWeightings: Seq[Int]) {

    val student: StudentMember = Fixtures.student("1234")
    student.mostSignificantCourse.course = course
    student.mostSignificantCourse.sprStartAcademicYear = academicYear - (yearWeightings.size - 1)
    val currentScyd: StudentCourseYearDetails = student.mostSignificantCourse.latestStudentCourseYearDetails
    currentScyd.academicYear = academicYear
    currentScyd.yearOfStudy = yearWeightings.size
    student.mostSignificantCourse.courseYearLength = yearWeightings.size
    currentScyd.modeOfAttendance = Fixtures.modeOfAttendance("F", "FT", "Full time")
    val service = new AbstractProgressionService with ModuleRegistrationServiceComponent with CourseAndRouteServiceComponent {
      override val moduleRegistrationService: ModuleRegistrationService = smartMock[ModuleRegistrationService]
      override val courseAndRouteService: CourseAndRouteService = smartMock[CourseAndRouteService]
    }

    def entityYear: ExamGridEntityYear = currentScyd.toExamGridEntityYear
    var weightings: Seq[CourseYearWeighting] = Seq()

    val previousYears = yearWeightings.reverse.tail.reverse.zipWithIndex.map { case (w, i) =>
      val yearOfStudy = i + 1
      val scyd: StudentCourseYearDetails = Fixtures.studentCourseYearDetails()
      scyd.modeOfAttendance = Fixtures.modeOfAttendance("F", "FT", "Full time")
      scyd.studentCourseDetails = student.mostSignificantCourse
      scyd.yearOfStudy = yearOfStudy
      scyd.academicYear = academicYear - (yearWeightings.size -1 -i)
      scyd.agreedMark = marks(i).underlying()
      student.mostSignificantCourse.addStudentCourseYearDetails(scyd)

      val weighting = Fixtures.yearWeighting(course, new JBigDecimal(w), student.mostSignificantCourse.sprStartAcademicYear, yearOfStudy)
      weightings = weightings :+ weighting
      service.courseAndRouteService.getCourseYearWeighting(course.code, student.mostSignificantCourse.sprStartAcademicYear, yearOfStudy) returns Option(weighting)
      scyd
    }

    service.moduleRegistrationService.weightedMeanYearMark(any[Seq[ModuleRegistration]], any[Map[Module, BigDecimal]], any[Boolean]) returns Right(marks.last)
    service.moduleRegistrationService.overcattedModuleSubsets(any[Seq[ModuleRegistration]], any[Map[Module, BigDecimal]], any[BigDecimal], any[Seq[UpstreamRouteRule]]) answers { args: Array[AnyRef] =>
      Seq((marks.last, args(0).asInstanceOf[Seq[ModuleRegistration]]))
    }

    val lastYearWeighting = Fixtures.yearWeighting(course, new JBigDecimal(yearWeightings.last), student.mostSignificantCourse.sprStartAcademicYear, yearWeightings.size)
    weightings = weightings :+ lastYearWeighting
    service.courseAndRouteService.getCourseYearWeighting(course.code, student.mostSignificantCourse.sprStartAcademicYear, yearWeightings.size) returns Option(lastYearWeighting)
  }

  @Test
  def graduateBenchmark(): Unit = {
    // examples from https://warwick.ac.uk/insite/coronavirus/staff/teaching/policyguidance/undergraduateintermediatefinal/graduationbenchmark-finalists/
    new FixtureWithYearsAndMarks(
      Seq(BigDecimal(70), BigDecimal(58), BigDecimal(60)),
      Seq(0, 50, 50)
    ) {
      service.moduleRegistrationService.percentageOfAssessmentTaken(any[Seq[ModuleRegistration]]) returns (BigDecimal(50))
      service.moduleRegistrationService.benchmarkWeightedAssessmentMark(any[Seq[ModuleRegistration]]) returns (BigDecimal(65))

      service.graduationBenchmark(entityYear.studentCourseYearDetails, entityYear.yearOfStudy, BigDecimal(120), Map(), calculateYearMarks = false, groupByLevel = false, weightings) should be(Right(60.3))
    }

    new FixtureWithYearsAndMarks(
      Seq(BigDecimal(64), BigDecimal(68), BigDecimal(70)),
      Seq(10, 30, 60)
    ) {
      service.moduleRegistrationService.percentageOfAssessmentTaken(any[Seq[ModuleRegistration]]) returns (BigDecimal(100) / BigDecimal(3))
      service.moduleRegistrationService.benchmarkWeightedAssessmentMark(any[Seq[ModuleRegistration]]) returns (BigDecimal(76))
      service.graduationBenchmark(entityYear.studentCourseYearDetails, entityYear.yearOfStudy, BigDecimal(120), Map(), calculateYearMarks = false, groupByLevel = false, weightings) should be(Right(70))
    }

    new FixtureWithYearsAndMarks(
      Seq(BigDecimal(64), BigDecimal(68), BigDecimal(76), BigDecimal(72)),
      Seq(10, 20, 30, 40)
    ) {
      service.moduleRegistrationService.percentageOfAssessmentTaken(any[Seq[ModuleRegistration]]) returns (BigDecimal(100) / BigDecimal(3))
      service.moduleRegistrationService.benchmarkWeightedAssessmentMark(any[Seq[ModuleRegistration]]) returns (BigDecimal(72))

      service.graduationBenchmark(entityYear.studentCourseYearDetails, entityYear.yearOfStudy, BigDecimal(120), Map(), calculateYearMarks = false, groupByLevel = false, weightings) should be(Right(71.5))
    }
  }

}
