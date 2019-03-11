package uk.ac.warwick.tabula.commands.exams.grids

import java.io.{ByteArrayOutputStream, FileOutputStream}

import org.apache.poi.ss.usermodel.Workbook
import org.joda.time.DateTime
import uk.ac.warwick.tabula.commands.Command
import uk.ac.warwick.tabula.data.model.StudentCourseYearDetails.YearOfStudy
import uk.ac.warwick.tabula.data.model._
import uk.ac.warwick.tabula.exams.grids.columns._
import uk.ac.warwick.tabula.exams.grids.columns.cats._
import uk.ac.warwick.tabula.exams.grids.columns.marking.{CurrentYearMarkColumnOption, OvercattedYearMarkColumnOption}
import uk.ac.warwick.tabula.exams.grids.columns.modules.{CoreModulesColumnOption, ModuleExamGridColumn, ModuleReportsColumn, OptionalModulesColumnOption}
import uk.ac.warwick.tabula.exams.grids.columns.studentidentification.{NameColumnOption, RouteColumnOption, UniversityIDColumnOption}
import uk.ac.warwick.tabula.exams.grids.{NullStatusAdapter, StatusAdapter}
import uk.ac.warwick.tabula.sandbox.SandboxData
import uk.ac.warwick.tabula.services.exams.grids.{NormalCATSLoadService, NormalLoadLookup, UpstreamRouteRuleService}
import uk.ac.warwick.tabula.services.{AssessmentMembershipService, ModuleRegistrationService}
import uk.ac.warwick.tabula.{AcademicYear, Fixtures, Mockito, TestBase}
import uk.ac.warwick.util.core.StopWatch

import scala.util.Random

/**
  * This test is a performance test for generating full and short exam grids. We use a streaming workbook
  * so building the workbook itself is fast, the time taken actually happens when we try and write the workbook
  * because the thing that's slow is (almost always) auto-sizing columns. In TAB-6873 we don't auto-size *any*
  * columns on either full or short grids any more, and the performance for writing should be sub-1s. At some point
  * in the future POI may fix performance issues related to SheetUtil.getCellWidth and the CellReference constructor.
  *
  * If you run into performance problems the first thing to do is to run this test through the IDEA Profiler.
  *
  * Oh and don't look too hard at the ridiculous fixture setup because it's a mess.
  *
  * @see https://warwick.slack.com/archives/C025ZSENB/p1552059325058100
  */
class GenerateExamGridExcelPerformanceTest extends TestBase with Mockito {

  // This is taken from an actual slow grid for TAB-6873, so we can test-first performance improvements
  val department: Department = Fixtures.department("ma", "Maths")

  val level: Level = new Level("1", "Undergraduate Level 1")
  val route: Route = Fixtures.route("g103")

  val sprFullyEnrolledStatus: SitsStatus = Fixtures.sitsStatus("F", "Fully Enrolled", "Fully Enrolled for this Session")

  val academicYear: AcademicYear = AcademicYear.starting(2018)

  val course: Course = Fixtures.course("UMAA-G103", "Mathematics (MMath)")
  val courses: Seq[Course] = Seq(course)

  val routes: Seq[Route] = Nil

  val yearOfStudy: Int = 1

  val normalLoadLookup: NormalLoadLookup = NormalLoadLookup(academicYear, yearOfStudy, smartMock[NormalCATSLoadService])
  val upstreamRouteRuleLookup: UpstreamRouteRuleLookup = UpstreamRouteRuleLookup(academicYear, smartMock[UpstreamRouteRuleService])

  val yearWeightings: Seq[CourseYearWeighting] =
    Seq(
      new CourseYearWeighting(course, academicYear, 1, BigDecimal(0.100)),
      new CourseYearWeighting(course, academicYear, 2, BigDecimal(0.200)),
      new CourseYearWeighting(course, academicYear, 3, BigDecimal(0.300)),
      new CourseYearWeighting(course, academicYear, 4, BigDecimal(0.400)),
    )

  val modules: Map[String, Module] = Map(
    "ma106" -> Fixtures.module(code = "ma106", name = "Linear Algebra"),
    "ma124" -> Fixtures.module(code = "ma124", name = "Mathematics by Computer"),
    "ma131" -> Fixtures.module(code = "ma131", name = "Analysis"),
    "ma132" -> Fixtures.module(code = "ma132", name = "Foundations"),
    "ma133" -> Fixtures.module(code = "ma133", name = "Differential Equations"),
    "ma134" -> Fixtures.module(code = "ma134", name = "Geometry and Motion"),
    "ma136" -> Fixtures.module(code = "ma136", name = "Introduction to Abstract Algebra"),
    "st111" -> Fixtures.module(code = "st111", name = "Probability (Part A)"),
    "cs126" -> Fixtures.module(code = "cs126", name = "Design of Information Structures"),
    "cs137" -> Fixtures.module(code = "cs137", name = "Discrete Mathematics & its Applications 2"),
    "ec106" -> Fixtures.module(code = "ec106", name = "Introduction to Quantitative Economics"),
    "es193" -> Fixtures.module(code = "es193", name = "Engineering Mathematics"),
    "ib104" -> Fixtures.module(code = "ib104", name = "Mathematical Programming I"),
    "ll113" -> Fixtures.module(code = "ll113", name = "French 1"),
    "ll129" -> Fixtures.module(code = "ll129", name = "Japanese I"),
    "ll130" -> Fixtures.module(code = "ll130", name = "Chinese 1"),
    "ll209" -> Fixtures.module(code = "ll209", name = "French 5"),
    "ll212" -> Fixtures.module(code = "ll212", name = "German 5"),
    "ll215" -> Fixtures.module(code = "ll215", name = "Spanish 2"),
    "ll216" -> Fixtures.module(code = "ll216", name = "Russian 2"),
    "ll224" -> Fixtures.module(code = "ll224", name = "Spanish 4"),
    "ll228" -> Fixtures.module(code = "ll228", name = "Spanish 5"),
    "ll230" -> Fixtures.module(code = "ll230", name = "Chinese 3 (Mandarin)"),
    "ll234" -> Fixtures.module(code = "ll234", name = "French 2"),
    "ll235" -> Fixtures.module(code = "ll235", name = "German 2"),
    "ma112" -> Fixtures.module(code = "ma112", name = "Experimental Mathematics"),
    "ma117" -> Fixtures.module(code = "ma117", name = "Programming for Scientists"),
    "ma125" -> Fixtures.module(code = "ma125", name = "Introduction to Geometry"),
    "ph136" -> Fixtures.module(code = "ph136", name = "Logic 1: Introduction to Symbolic Logic"),
    "ph144" -> Fixtures.module(code = "ph144", name = "Mind and Reality"),
    "px101" -> Fixtures.module(code = "px101", name = "Quantum Phenomena"),
    "px120" -> Fixtures.module(code = "px120", name = "Electricity and Magnetism"),
    "px144" -> Fixtures.module(code = "px144", name = "Introduction to Astronomy"),
    "px147" -> Fixtures.module(code = "px147", name = "Introduction to Particle Physics"),
    "px148" -> Fixtures.module(code = "px148", name = "Classical Mechanics & Special Relativity"),
    "st104" -> Fixtures.module(code = "st104", name = "Statistical Laboratory I"),
    "st112" -> Fixtures.module(code = "st112", name = "Probability (Part B)")
  )

  def moduleRegistration(universityId: String, moduleCode: String, cats: BigDecimal): ModuleRegistration = {
    val reg = Fixtures.moduleRegistration(students(universityId).mostSignificantCourse, modules(moduleCode), cats.underlying(), academicYear)
    reg.membershipService = smartMock[AssessmentMembershipService]
    reg.membershipService.getUpstreamAssessmentGroups(reg, eagerLoad = true) returns Nil // TODO component marks here
    reg
  }

  def examGridEntityYear(universityId: String, moduleRegistrations: Seq[ModuleRegistration], cats: BigDecimal): ExamGridEntityYear =
    ExamGridEntityYear(
      moduleRegistrations = moduleRegistrations,
      cats = cats,
      route = route,
      overcattingModules = None,
      markOverrides = None,
      studentCourseYearDetails = students.get(universityId).map(_.mostSignificantCourse.latestStudentCourseYearDetails),
      level = Some(level),
      yearOfStudy = 1
    )

  val studentIds: Seq[String] = (1 to 200).map { i => f"u1800$i%03d" }

  private def generateStudents(): Map[String, StudentMember] =
    studentIds.map { universityId =>
      Fixtures.student(universityId = universityId, userId = s"u$universityId", department = department, courseDepartment = department, sprStatus = sprFullyEnrolledStatus)
    }.map { student =>
      student.mostSignificantCourse.latestStudentCourseYearDetails.modeOfAttendance = Fixtures.modeOfAttendance()
      student.universityId -> student
    }.toMap

  val students: Map[String, StudentMember] = generateStudents()

  val coreModules: Seq[(Module, BigDecimal)] = Seq(
    (modules("ma106"), BigDecimal(12.00)),
    (modules("ma124"), BigDecimal(6.00)),
    (modules("ma131"), BigDecimal(24.00)),
    (modules("ma132"), BigDecimal(12.00)),
    (modules("ma133"), BigDecimal(12.00)),
    (modules("ma134"), BigDecimal(12.00)),
    (modules("ma136"), BigDecimal(6.00)),
    (modules("st111"), BigDecimal(6.00)),
  )
  val optionalModules: Seq[(Module, BigDecimal)] = Seq(
    (modules("cs126"), BigDecimal(15.00)),
    (modules("cs137"), BigDecimal(12.00)),
    (modules("ec106"), BigDecimal(24.00)),
    (modules("es193"), BigDecimal(15.00)),
    (modules("ib104"), BigDecimal(7.50)),
    (modules("ib104"), BigDecimal(15.00)),
    (modules("ll113"), BigDecimal(24.00)),
    (modules("ll129"), BigDecimal(30.00)),
    (modules("ll130"), BigDecimal(30.00)),
    (modules("ll209"), BigDecimal(30.00)),
    (modules("ll212"), BigDecimal(24.00)),
    (modules("ll215"), BigDecimal(24.00)),
    (modules("ll216"), BigDecimal(24.00)),
    (modules("ll224"), BigDecimal(30.00)),
    (modules("ll228"), BigDecimal(24.00)),
    (modules("ll230"), BigDecimal(24.00)),
    (modules("ll234"), BigDecimal(24.00)),
    (modules("ll235"), BigDecimal(24.00)),
    (modules("ma112"), BigDecimal(6.00)),
    (modules("ma117"), BigDecimal(12.00)),
    (modules("ma125"), BigDecimal(6.00)),
    (modules("ph136"), BigDecimal(15.00)),
    (modules("ph144"), BigDecimal(15.00)),
    (modules("px101"), BigDecimal(6.00)),
    (modules("px120"), BigDecimal(12.00)),
    (modules("px144"), BigDecimal(6.00)),
    (modules("px147"), BigDecimal(6.00)),
    (modules("px148"), BigDecimal(12.00)),
    (modules("st104"), BigDecimal(12.00)),
    (modules("st112"), BigDecimal(6.00)),
  )

  private def generateExamGridEntityYears(): Map[String, ExamGridEntityYear] =
    studentIds.zipWithIndex.map { case (universityId, i) =>
      val (moduleRegistrations, cats): (Seq[ModuleRegistration], BigDecimal) = i % 10 match {
        case 0 => (Nil, BigDecimal(0))
        case _ =>
          val modules = coreModules ++ Random.shuffle(optionalModules).take(i % 5)
          val moduleRegistrations = modules.map { case (module, c) =>
            moduleRegistration(universityId, module.code, c)
          }
          val cats = modules.map { case (_, c) => c }.sum

          (moduleRegistrations, cats)
      }

      universityId -> examGridEntityYear(universityId, moduleRegistrations, cats)
    }.toMap

  val examGridEntityYears: Map[String, ExamGridEntityYear] = generateExamGridEntityYears()

  private def generateExamGridEntities(): Seq[ExamGridEntity] =
    studentIds.zipWithIndex.map { case (universityId, i) =>
      val name = SandboxData.randomName(i.toLong, if (i % 2 == 0) Gender.Male else Gender.Female)

      ExamGridEntity(firstName = name.givenName, lastName = name.familyName, universityId = universityId, lastImportDate = Some(DateTime.now()), years = Map(1 -> examGridEntityYears.get(universityId)), yearWeightings = yearWeightings)
    }

  val entities: Seq[ExamGridEntity] = generateExamGridEntities()

  val examGridColumnState: ExamGridColumnState = ExamGridColumnState(
    entities = entities,
    overcatSubsets = examGridEntityYears.values.map { examGridEntityYear =>
      examGridEntityYear -> Nil // TODO
    }.toMap,
    coreRequiredModuleLookup = NullCoreRequiredModuleLookup,
    normalLoadLookup = normalLoadLookup,
    routeRulesLookup = upstreamRouteRuleLookup,
    academicYear = academicYear,
    yearOfStudy = 1,
    department = department,
    nameToShow = ExamGridStudentIdentificationColumnValue.BothName,
    showComponentMarks = true,
    showZeroWeightedComponents = false,
    showComponentSequence = false,
    showModuleNames = ExamGridDisplayModuleNameColumnValue.LongNames,
    calculateYearMarks = false,
    isLevelGrid = false
  )

  val firstNameColumn: NameColumnOption#FirstNameColumn = new NameColumnOption().FirstNameColumn(examGridColumnState)
  val lastNameColumn: NameColumnOption#LastNameColumn = new NameColumnOption().LastNameColumn(examGridColumnState)
  val universityIDColumn: UniversityIDColumnOption#Column = new UniversityIDColumnOption().Column(examGridColumnState)
  val routeColumn: RouteColumnOption#Column = new RouteColumnOption().Column(examGridColumnState)

  val leftColumns: Seq[ChosenYearExamGridColumn] = Seq(
    firstNameColumn,
    lastNameColumn,
    universityIDColumn,
    routeColumn,
  )

  val perYearColumns: Map[StudentCourseYearDetails.YearOfStudy, Seq[PerYearExamGridColumn]] = Map(1 ->
    (coreModules.map { case (module, cats) =>
      new CoreModulesColumnOption().Column(examGridColumnState, module, isDuplicate = false, cats.underlying())
    } ++ optionalModules.map { case (module, cats) =>
      new OptionalModulesColumnOption().Column(examGridColumnState, module, isDuplicate = false, cats.underlying())
    })
  )

  val moduleRegistrationService: ModuleRegistrationService = smartMock[ModuleRegistrationService]
  moduleRegistrationService.weightedMeanYearMark(Nil, Map.empty, allowEmpty = false) returns Left(s"The year mark cannot be calculated because there are no module marks")
  moduleRegistrationService.weightedMeanYearMark(any[Seq[ModuleRegistration]], any[Map[Module, BigDecimal]], any[Boolean]) returns Left(s"The year mark cannot be calculated because there are no module marks")

  val best90MA2WeightedColumnOption = new Best90MA2WeightAverageMarksColumn()
  best90MA2WeightedColumnOption.moduleRegistrationService = moduleRegistrationService

  val best90MA2CourseStatusColumnOption = new Best90MA2CourseStatusColumn()
  best90MA2CourseStatusColumnOption.moduleRegistrationService = moduleRegistrationService

  val currentYearMarkColumnOption = new CurrentYearMarkColumnOption()
  currentYearMarkColumnOption.moduleRegistrationService = moduleRegistrationService

  val best90MA2WeightedColumn: Best90MA2WeightAverageMarksColumn#Column = best90MA2WeightedColumnOption.Column(examGridColumnState)
  val best90MA2CourseStatusColumn: Best90MA2CourseStatusColumn#Column = best90MA2CourseStatusColumnOption.Column(examGridColumnState)
  val fortyCATSColumn: FortyCATSColumnOption#Column = new FortyCATSColumnOption().Column(examGridColumnState)
  val totalCATSColumn: TotalCATSColumnOption#Column = new TotalCATSColumnOption().Column(examGridColumnState)
  val passedCATSColumn: PassedCATSColumnOption#Column = new PassedCATSColumnOption().Column(examGridColumnState, 1)
  val currentYearMarkColumn: CurrentYearMarkColumnOption#Column = currentYearMarkColumnOption.Column(examGridColumnState)
  val overcattedYearMarkColumn: OvercattedYearMarkColumnOption#Column = new OvercattedYearMarkColumnOption().Column(examGridColumnState)

  val rightColumns: Seq[ChosenYearExamGridColumn] = Seq(
    best90MA2WeightedColumn,
    best90MA2CourseStatusColumn,
    fortyCATSColumn,
    totalCATSColumn,
    passedCATSColumn,
    currentYearMarkColumn,
    overcattedYearMarkColumn,
  )

  val chosenYearColumnValues: Map[ChosenYearExamGridColumn, Map[ExamGridEntity, ExamGridColumnValue]] =
    (leftColumns ++ rightColumns).map { column => column -> column.result }.toMap

  val perYearColumnValues: Map[PerYearExamGridColumn, Map[ExamGridEntity, Map[StudentCourseYearDetails.YearOfStudy, Map[ExamGridColumnValueType, Seq[ExamGridColumnValue]]]]] =
    perYearColumns(1).map { column => column -> column.values }.toMap

  val showComponentMarks: Boolean = true
  val status: StatusAdapter = NullStatusAdapter

  /**
    * HALT! Are you thinking of reducing the timeout or perhaps @Ignore-ing this test? Read the Javadoc on the class.
    */
  @Test
  def fullGridPerformanceMergedCells(): Unit = Command.timed { sw =>
    sw.start("Generate workbook")
    val workbook: Workbook = GenerateExamGridExporter(
      department = department,
      academicYear = academicYear,
      courses = courses,
      routes = routes,
      yearOfStudy = yearOfStudy,
      normalLoadLookup = normalLoadLookup,
      entities = entities,
      leftColumns = leftColumns,
      perYearColumns = perYearColumns,
      rightColumns = rightColumns,
      chosenYearColumnValues = chosenYearColumnValues,
      perYearColumnValues = perYearColumnValues,
      showComponentMarks = showComponentMarks,
      mergedCells = true,
      status = status
    )
    sw.stop()
    workbook should not be null

    sw.start("Write workbook to ByteArrayOutputStream")

    val out = new ByteArrayOutputStream
    workbook.write(out)
    out.close()

    sw.stop()

    if (sw.getTotalTimeMillis > 20000)
      fail(s"Grid generation took too long! ${sw.prettyPrint()}")
  }

  /**
    * HALT! Are you thinking of reducing the timeout or perhaps @Ignore-ing this test? Read the Javadoc on the class.
    */
  @Test
  def fullGridPerformanceUnmerged(): Unit = Command.timed { sw =>
    sw.start("Generate workbook")
    val workbook: Workbook = GenerateExamGridExporter(
      department = department,
      academicYear = academicYear,
      courses = courses,
      routes = routes,
      yearOfStudy = yearOfStudy,
      normalLoadLookup = normalLoadLookup,
      entities = entities,
      leftColumns = leftColumns,
      perYearColumns = perYearColumns,
      rightColumns = rightColumns,
      chosenYearColumnValues = chosenYearColumnValues,
      perYearColumnValues = perYearColumnValues,
      showComponentMarks = showComponentMarks,
      mergedCells = false,
      status = status
    )
    sw.stop()
    workbook should not be null

    sw.start("Write workbook to ByteArrayOutputStream")

    val out = new ByteArrayOutputStream
    workbook.write(out)
    out.close()

    sw.stop()

    if (sw.getTotalTimeMillis > 20000)
      fail(s"Grid generation took too long! ${sw.prettyPrint()}")
  }

  private[this] abstract class ShortGridFixture(sw: StopWatch) {
    sw.start("Generate short grid extra information")
    val perYearModuleMarkColumns: Map[YearOfStudy, Seq[ModuleExamGridColumn]] =
      perYearColumns.map { case (year, columns) => year -> columns.collect { case marks: ModuleExamGridColumn => marks } }

    val perYearModuleReportColumns: Map[YearOfStudy, Seq[ModuleReportsColumn]] =
      perYearColumns.map { case (year, columns) => year -> columns.collect { case marks: ModuleReportsColumn => marks } }

    val maxYearColumnSize: Map[YearOfStudy, YearOfStudy] =
      perYearModuleMarkColumns.map { case (year, columns) =>
        val maxModuleColumns = (entities.map(entity => columns.count(c => !c.isEmpty(entity, year))) ++ Seq(1)).max
        year -> maxModuleColumns
      }

    // for each entity have a list of all modules with marks and padding at the end for empty cells
    val moduleColumnsPerEntity: Map[ExamGridEntity, Map[YearOfStudy, Seq[Option[ModuleExamGridColumn]]]] =
      entities.map(entity => {
        entity -> perYearModuleMarkColumns.map { case (year, mods) =>
          val hasValue: Seq[Option[ModuleExamGridColumn]] = mods.filter(m => !m.isEmpty(entity, year)).map(Some.apply)
          val padding: Seq[Option[ModuleExamGridColumn]] = (1 to maxYearColumnSize(year) - hasValue.size).map(_ => None)
          year -> (hasValue ++ padding)
        }
      }).toMap
    sw.stop()
  }

  /**
    * HALT! Are you thinking of reducing the timeout or perhaps @Ignore-ing this test? Read the Javadoc on the class.
    */
  @Test
  def shortGridPerformanceMergedCells(): Unit = Command.timed { sw => new ShortGridFixture(sw) {
    sw.start("Generate workbook")
    val workbook: Workbook = GenerateExamGridShortFormExporter(
      department = department,
      academicYear = academicYear,
      courses = courses,
      routes = routes,
      yearOfStudy = yearOfStudy,
      normalLoadLookup = normalLoadLookup,
      entities = entities,
      leftColumns = leftColumns,
      perYearColumns = perYearColumns,
      rightColumns = rightColumns,
      chosenYearColumnValues = chosenYearColumnValues,
      perYearColumnValues = perYearColumnValues,
      moduleColumnsPerEntity = moduleColumnsPerEntity,
      perYearModuleMarkColumns = perYearModuleMarkColumns,
      perYearModuleReportColumns = perYearModuleReportColumns,
      maxYearColumnSize,
      showComponentMarks = showComponentMarks,
      mergedCells = true,
      status = status
    )
    sw.stop()
    workbook should not be null

    sw.start("Write workbook to ByteArrayOutputStream")

    val out = new ByteArrayOutputStream
    workbook.write(out)
    out.close()

    sw.stop()

    if (sw.getTotalTimeMillis > 20000)
      fail(s"Grid generation took too long! ${sw.prettyPrint()}")
  }}

  /**
    * HALT! Are you thinking of reducing the timeout or perhaps @Ignore-ing this test? Read the Javadoc on the class.
    */
  @Test
  def shortGridPerformanceUnmerged(): Unit = Command.timed { sw => new ShortGridFixture(sw) {
    sw.start("Generate workbook")
    val workbook: Workbook = GenerateExamGridShortFormExporter(
      department = department,
      academicYear = academicYear,
      courses = courses,
      routes = routes,
      yearOfStudy = yearOfStudy,
      normalLoadLookup = normalLoadLookup,
      entities = entities,
      leftColumns = leftColumns,
      perYearColumns = perYearColumns,
      rightColumns = rightColumns,
      chosenYearColumnValues = chosenYearColumnValues,
      perYearColumnValues = perYearColumnValues,
      moduleColumnsPerEntity = moduleColumnsPerEntity,
      perYearModuleMarkColumns = perYearModuleMarkColumns,
      perYearModuleReportColumns = perYearModuleReportColumns,
      maxYearColumnSize,
      showComponentMarks = showComponentMarks,
      mergedCells = false,
      status = status
    )
    sw.stop()
    workbook should not be null

    sw.start("Write workbook to ByteArrayOutputStream")

    val out = new ByteArrayOutputStream
    workbook.write(out)
    out.close()

    sw.stop()

    if (sw.getTotalTimeMillis > 20000)
      fail(s"Grid generation took too long! ${sw.prettyPrint()}")
  }}

}
