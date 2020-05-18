package uk.ac.warwick.tabula.services

import uk.ac.warwick.tabula.data.model.TabulaAssessmentSubtype.Exam
import uk.ac.warwick.tabula.data.model.{AssessmentComponent, AssessmentType, ModuleRegistration, ModuleSelectionStatus, StudentCourseDetails, UpstreamAssessmentGroup}
import uk.ac.warwick.tabula.data.{ModuleRegistrationDao, ModuleRegistrationDaoComponent}
import uk.ac.warwick.tabula.{AcademicYear, Fixtures, Mockito, TestBase}
import uk.ac.warwick.util.termdates.AcademicYearPeriod.PeriodType

import scala.collection.SortedMap
import scala.util.Random

class ModuleRegistrationServiceTest extends TestBase with Mockito {

  val mockModuleRegistrationDao: ModuleRegistrationDao = smartMock[ModuleRegistrationDao]

  trait Fixture {
    val service: AbstractModuleRegistrationService with ModuleRegistrationDaoComponent = new AbstractModuleRegistrationService with ModuleRegistrationDaoComponent {
      val moduleRegistrationDao: ModuleRegistrationDao = mockModuleRegistrationDao
    }
  }

  trait GraduationBenchmarkFixture extends Fixture {
    val scd: StudentCourseDetails = Fixtures.student().mostSignificantCourse
    val academicYear: AcademicYear = AcademicYear(2019)
    scd.latestStudentCourseYearDetails.academicYear = academicYear

    val mockMembershipService: AssessmentMembershipService = smartMock[AssessmentMembershipService]

    val modules = Map(
      "in301" -> Fixtures.module("in301"),
      "in302" -> Fixtures.module("in302"),
      "in303" -> Fixtures.module("in303"),
      "in304" -> Fixtures.module("in304"),
      "in305" -> Fixtures.module("in305"),
      "in306" -> Fixtures.module("in306"),
    )

    val moduleRegistrations = Seq(
      Fixtures.moduleRegistration(scd, modules("in301"), BigDecimal(30).underlying, academicYear, "", BigDecimal(55), ModuleSelectionStatus.Core),
      Fixtures.moduleRegistration(scd, modules("in302"), BigDecimal(30).underlying, academicYear, "", BigDecimal(60), ModuleSelectionStatus.Core),
      Fixtures.moduleRegistration(scd, modules("in303"), BigDecimal(30).underlying, academicYear, "", BigDecimal(67), ModuleSelectionStatus.Core),
      Fixtures.moduleRegistration(scd, modules("in304"), BigDecimal(15).underlying, academicYear, "", BigDecimal(56), ModuleSelectionStatus.Core),
      Fixtures.moduleRegistration(scd, modules("in305"), BigDecimal(7.5).underlying, academicYear, "", BigDecimal(77), ModuleSelectionStatus.Core),
      Fixtures.moduleRegistration(scd, modules("in306"), BigDecimal(7.5).underlying, academicYear, "", BigDecimal(88), ModuleSelectionStatus.Core),
    )

    val components: SortedMap[String, Seq[AssessmentComponent]] = SortedMap(
      "in301" -> Seq(Fixtures.assessmentComponent(modules("in301"), 1, AssessmentType.Essay)),

      "in302" -> Seq(
        Fixtures.assessmentComponent(modules("in302"), 1, AssessmentType.Laboratory, 30),
        Fixtures.assessmentComponent(modules("in302"), 2, AssessmentType.Laboratory, 30),
        Fixtures.assessmentComponent(modules("in302"), 3, AssessmentType.SummerExam, 40),
      ),

      "in303" -> Seq(
        Fixtures.assessmentComponent(modules("in303"), 1, AssessmentType.CriticalReview, 40),
        Fixtures.assessmentComponent(modules("in303"), 2, AssessmentType.SummerExam, 60),
      ),

      "in304" -> Seq(
        Fixtures.assessmentComponent(modules("in304"), 1, AssessmentType.InClassTest, 5),
        Fixtures.assessmentComponent(modules("in304"), 2, AssessmentType.InClassTest, 5),
        Fixtures.assessmentComponent(modules("in304"), 3, AssessmentType.SummerExam, 90),
      ),

      "in305" ->  Seq(
        Fixtures.assessmentComponent(modules("in305"), 1, AssessmentType.Essay, 20),
        Fixtures.assessmentComponent(modules("in305"), 2, AssessmentType.SummerExam, 80),
      ),

      "in306" -> Seq( Fixtures.assessmentComponent(modules("in306"), 1, AssessmentType.SummerExam) )
    )

    val componentMarks = Seq(
      BigDecimal(55),
      BigDecimal(67),
      BigDecimal(65),
      BigDecimal(43),
      BigDecimal(54),
      BigDecimal(65),
      BigDecimal(65),
      BigDecimal(66),
      BigDecimal(50),
      BigDecimal(65),
      BigDecimal(63)
    )

    val upstreamAssessmentGroups: Map[String, Seq[UpstreamAssessmentGroup]] = components.zipWithIndex.map { case ((mc, components), i) =>
      mc -> components.map { ac =>
        val deadline =
          if(ac.assessmentType.subtype == Exam) AcademicYear(2019).termOrVacation(PeriodType.summerTerm).firstDay
          else AcademicYear(2019).termOrVacation(PeriodType.springTerm).lastDay
        Fixtures.assessmentGroupAndMember(ac, componentMarks(i), academicYear, deadline)
      }
    }.toMap

    moduleRegistrations.foreach { mr =>
      scd.addModuleRegistration(mr)
      mr.membershipService = mockMembershipService
      mockMembershipService.getUpstreamAssessmentGroups(mr, eagerLoad = true) returns { upstreamAssessmentGroups(mr.module.code) }
    }
  }

  @Test
  def weightedMeanYearMark(): Unit = {
    val module1 = Fixtures.module("xx101")
    val module2 = Fixtures.module("xx102")
    val module3 = Fixtures.module("xx103")
    val module4 = Fixtures.module("xx104")
    val module5 = Fixtures.module("xx105")
    val module6 = Fixtures.module("xx106")
    new Fixture {
      val moduleRegistrations = Seq(
        Fixtures.moduleRegistration(null, module1, BigDecimal(30).underlying, null, agreedMark = BigDecimal(100)),
        Fixtures.moduleRegistration(null, module2, BigDecimal(45).underlying, null, agreedMark = BigDecimal(58)),
        Fixtures.moduleRegistration(null, module3, BigDecimal(15).underlying, null, agreedMark = BigDecimal(30)),
        Fixtures.moduleRegistration(null, module4, BigDecimal(7.5).underlying, null, agreedMark = BigDecimal(0)),
        Fixtures.moduleRegistration(null, module5, BigDecimal(7.5).underlying, null, agreedMark = BigDecimal(97)),
        Fixtures.moduleRegistration(null, module6, BigDecimal(15).underlying, null, agreedMark = BigDecimal(64))
      )
      val result: Either[String, BigDecimal] = service.weightedMeanYearMark(moduleRegistrations, Map(), allowEmpty = false)
      result.isRight should be (true)
      result.toOption.get.scale should be(1)
      result.toOption.get.doubleValue should be(64.6)

      val moduleRegistrationsWithMissingAgreedMark = Seq(
        Fixtures.moduleRegistration(null, module1, BigDecimal(30).underlying, null, agreedMark = BigDecimal(100)),
        Fixtures.moduleRegistration(null, module2, BigDecimal(45).underlying, null, agreedMark = BigDecimal(58)),
        Fixtures.moduleRegistration(null, module3, BigDecimal(15).underlying, null, agreedMark = BigDecimal(30)),
        Fixtures.moduleRegistration(null, module4, BigDecimal(7.5).underlying, null, agreedMark = null),
        Fixtures.moduleRegistration(null, module5, BigDecimal(7.5).underlying, null, agreedMark = BigDecimal(97)),
        Fixtures.moduleRegistration(null, module6, BigDecimal(15).underlying, null, agreedMark = BigDecimal(64))
      )
      val noResult: Either[String, BigDecimal] = service.weightedMeanYearMark(moduleRegistrationsWithMissingAgreedMark, Map(), allowEmpty = false)
      noResult.isRight should be (false)

      val moduleRegistrationsWithOverriddenMark = Seq(
        Fixtures.moduleRegistration(null, module1, BigDecimal(30).underlying, null, agreedMark = BigDecimal(100)),
        Fixtures.moduleRegistration(null, module2, BigDecimal(45).underlying, null, agreedMark = BigDecimal(58)),
        Fixtures.moduleRegistration(null, module3, BigDecimal(15).underlying, null, agreedMark = BigDecimal(30)),
        Fixtures.moduleRegistration(null, module4, BigDecimal(7.5).underlying, null, agreedMark = null),
        Fixtures.moduleRegistration(null, module5, BigDecimal(7.5).underlying, null, agreedMark = BigDecimal(97)),
        Fixtures.moduleRegistration(null, module6, BigDecimal(15).underlying, null, agreedMark = BigDecimal(64))
      )
      val overriddenResult: Either[String, BigDecimal] = service.weightedMeanYearMark(moduleRegistrationsWithOverriddenMark, Map(module4 -> 0), allowEmpty = false)
      overriddenResult.isRight should be (true)
      overriddenResult.toOption.get.scale should be(1)
      overriddenResult.toOption.get.doubleValue should be(64.6)
    }
  }

  @Test
  def weightedMeanYearMarkNoModules(): Unit = {
    new Fixture {
      val errorResult: Either[String, BigDecimal] = service.weightedMeanYearMark(Nil, Map(), allowEmpty = false)
      errorResult.isRight should be(false)

      val noErrorResult: Either[String, BigDecimal] = service.weightedMeanYearMark(Nil, Map(), allowEmpty = true)
      noErrorResult.isRight should be(true)
      noErrorResult.toOption.get should be(BigDecimal(0))
    }
  }

  @Test
  def overcattedModuleSubsets(): Unit = {
    new Fixture {
      val scd: StudentCourseDetails = Fixtures.student().mostSignificantCourse
      val academicYear: AcademicYear = AcademicYear(2014)
      scd.latestStudentCourseYearDetails.academicYear = academicYear
      val moduleRegistrations = Seq(
        Fixtures.moduleRegistration(scd, Fixtures.module("ch3c5"), BigDecimal(6).underlying, academicYear, "", BigDecimal(78), ModuleSelectionStatus.Core),
        Fixtures.moduleRegistration(scd, Fixtures.module("ch3f2"), BigDecimal(15).underlying, academicYear, "", BigDecimal(79), ModuleSelectionStatus.Core),
        Fixtures.moduleRegistration(scd, Fixtures.module("ch3f3"), BigDecimal(30).underlying, academicYear, "", BigDecimal(86), ModuleSelectionStatus.Core),
        Fixtures.moduleRegistration(scd, Fixtures.module("ch3c3"), BigDecimal(30).underlying, academicYear, "", BigDecimal(70), ModuleSelectionStatus.Core),
        Fixtures.moduleRegistration(scd, Fixtures.module("ch3f4"), BigDecimal(15).underlying, academicYear, "", BigDecimal(79), ModuleSelectionStatus.Option),

        Fixtures.moduleRegistration(scd, Fixtures.module("ch3f6"), BigDecimal(15).underlying, academicYear, "", BigDecimal(65), ModuleSelectionStatus.Option),
        Fixtures.moduleRegistration(scd, Fixtures.module("ch3f7"), BigDecimal(15).underlying, academicYear, "", BigDecimal(69), ModuleSelectionStatus.Option),
        Fixtures.moduleRegistration(scd, Fixtures.module("ch3f8"), BigDecimal(15).underlying, academicYear, "", BigDecimal(68), ModuleSelectionStatus.Option)
      )
      moduleRegistrations.foreach(scd.addModuleRegistration)
      val result: Seq[(BigDecimal, Seq[ModuleRegistration])] = service.overcattedModuleSubsets(scd.latestStudentCourseYearDetails.moduleRegistrations, Map(), 120, Seq()) // TODO check route rules
      // There are 81 CATS of core modules, leaving 39 to reach the normal load of 120
      // All the options are 15 CATS, so there are 5 combinations of modules that are valid (4 with 3 in each and 1 with 4)
      result.size should be(5)
    }
  }


  @Test
  def benchmarkComponentsAndMarks(): Unit = {
    new GraduationBenchmarkFixture {
      val componentAndMarks: Seq[ComponentAndMarks] = service.benchmarkComponentsAndMarks(moduleRegistrations.head)
      componentAndMarks.size should be (1)
      componentAndMarks.head.cats should be (BigDecimal(30))
      componentAndMarks.head.member.firstDefinedMark should be (Some(BigDecimal(55)))
    }
  }

  @Test
  def benchmarkWeightedAssessmentMark(): Unit = {
    new GraduationBenchmarkFixture {
      service.benchmarkWeightedAssessmentMark(moduleRegistrations) should be (BigDecimal(60.0))
    }
  }

  @Test
  def percentageOfAssessmentTaken(): Unit = {
    new GraduationBenchmarkFixture {
      service.percentageOfAssessmentTaken(moduleRegistrations) should be (BigDecimal(52.5))
    }
  }
}
