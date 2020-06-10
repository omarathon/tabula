package uk.ac.warwick.tabula.sandbox

import uk.ac.warwick.tabula.JavaImports.JBigDecimal
import uk.ac.warwick.tabula.data.model.{AssessmentType, _}
import uk.co.halfninja.randomnames.Gender._
import uk.co.halfninja.randomnames.{CompositeNameGenerator, Name, NameGenerators}

import scala.math.BigDecimal.RoundingMode
import scala.util.Try

// scalastyle:off magic.number
object SandboxData {
  final val NameGenerator: CompositeNameGenerator = NameGenerators.standardGenerator()
  type NameGender = uk.co.halfninja.randomnames.Gender

  final val Departments = Map(
    "arc" -> Department("School of Architecture", "arc", "S", Map(
      "arc101" -> Module("Introduction to Architecture", "INTROARC", "arc101", BigDecimal(15)),
      "arc102" -> Module("Architectural Design 1", "ARCDES1", "arc102", BigDecimal(15)),
      "arc103" -> Module("Introduction to Architectural History", "INTROARCH", "arc103", BigDecimal(15)),
      "arc106" -> Module("Architectural Technology 1", "ARCTECH1", "arc106", BigDecimal(15)),
      "arc115" -> Module("20th Century Architecture", "ARCTECH20", "arc115", BigDecimal(30)),
      "arc129" -> Module("Environmental Design and Services", "ENVDESSERV", "arc129", BigDecimal(30)),

      "arc201" -> Module("Architectural Technology 2", "ARCTECH2", "arc201", BigDecimal(15)),
      "arc203" -> Module("Professional Practice and Management", "PPM", "arc203", BigDecimal(15)),
      "arc204" -> Module("Principles and Theories of Architecture", "PTA", "arc204", BigDecimal(15)),
      "arc210" -> Module("The Place of Houses", "PAH", "arc210", BigDecimal(15)),
      "arc219" -> Module("Tectonic Practice", "TECPRA", "arc219", BigDecimal(30)),
      "arc222" -> Module("Sustainable Principles", "SUVPRI", "arc222", BigDecimal(30)),

      "arc3a1" -> Module("Integrating Technology", "INTGTECH", "arc3a1", BigDecimal(30)),
      "arc330" -> Module("History of Modern Architecture", "HMTEC", "arc330", BigDecimal(30)),
      "arc339" -> Module("Dissertation (Architecture)", "SISARC", "arc339", BigDecimal(60))
    ), Map(
      "ac801" ->
        Route("Architecture", "ac801", DegreeType.Undergraduate, CourseType.UG, awardCode = "BA", isResearch = false,
          Seq("arc101", "arc102", "arc103", "arc201", "arc203", "arc204", "arc339"),
          Seq("arc106", "arc115", "arc129", "arc210", "arc219", "arc222", "arc3a1", "arc330"),
          4200001, 4200100),
      "ac802" ->
        Route("Architecture with Intercalated Year", "ac802", DegreeType.Undergraduate, CourseType.UG, awardCode = "MBCHB", isResearch = false,
          Seq("arc101", "arc102", "arc103", "arc201", "arc203", "arc204", "arc339"),
          Seq("arc106", "arc115", "arc129", "arc210", "arc219", "arc222", "arc3a1", "arc330"),
          4200101, 4200130),
      "ac8p0" ->
        Route("Architecture (Research)", "ac8p0", DegreeType.Postgraduate, CourseType.PGR, awardCode = "PHD", isResearch = true, Seq(), Seq(), 4200201, 4200300),
      "ac8p1" ->
        Route("Architecture (Taught)", "ac8p1", DegreeType.Postgraduate, CourseType.PGT, awardCode = "MA", isResearch = false,
          Seq("arc222", "arc3a1", "arc330"), Seq(),
          4200301, 4200350)
    ), 5200001, 5200030),
    "hom" -> Department("History of Music", "hom", "A", Map(
      "hom101" -> Module("History of Musical Techniques", "HOM101", "hom101", BigDecimal(15)),
      "hom102" -> Module("Introduction to Ethnomusicology", "HOM102", "hom102", BigDecimal(15)),
      "hom103" -> Module("The Long Nineteenth Century", "HOM103", "hom103", BigDecimal(15)),
      "hom106" -> Module("History of Composition", "HOM106", "hom106", BigDecimal(15)),
      "hom115" -> Module("20th Century Music", "HOM115", "hom115", BigDecimal(30)),
      "hom128" -> Module("Theory of Music", "HOM128", "hom128", BigDecimal(30)),
      "hom129" -> Module("Theory and Analysis", "HOM129", "hom129", BigDecimal(30)),

      "hom201" -> Module("Russian and Soviet Music, 1890-1975", "HOM201", "hom201", BigDecimal(15)),
      "hom203" -> Module("Studies in Popular Music", "HOM203", "hom203", BigDecimal(15)),
      "hom204" -> Module("History of Opera", "HOM204", "hom204", BigDecimal(15)),
      "hom210" -> Module("Writing Practices in Music", "HOM210", "hom210", BigDecimal(15)),
      "hom218" -> Module("Popular Music", "HOM218", "hom218", BigDecimal(30)),
      "hom219" -> Module("Popular Music and Theories of Mass Culture", "HOM219", "hom219", BigDecimal(30)),
      "hom222" -> Module("Late 19th and Early 20th Century English Song", "HOM222", "hom222", BigDecimal(30)),

      "hom3a1" -> Module("Britten's Chamber Operas", "HOM3A1", "hom3a1", BigDecimal(30)),
      "hom332" -> Module("Influences of Hip-hop on Popular Culture", "HOM332", "hom332", BigDecimal(30)),

      "hom334" -> Module("Music and Ideas from Romanticism to the Late Twentieth Century", "HOM334", "hom334", BigDecimal(10)),
      "hom335" -> Module("Creative Music Technology", "HOM335", "hom335", BigDecimal(10), equalSplits),
      "hom336" -> Module("Topics in Popular Music", "HOM336", "hom336", BigDecimal(10), smallAssessments),
      "hom339" -> Module("Dissertation (History of Music)", "HOM339", "hom339", BigDecimal(60)),
      "hom340" -> Module("Dissertation", "HOM340", "hom340", BigDecimal(60)),

      "hom401" -> Module("Advanced Theory and Analysis", "HOM401", "hom401", BigDecimal(15)),
      "hom402" -> Module("Baroque composers", "HOM402", "hom402", BigDecimal(15)),
      "hom422" -> Module("Medieval musical instruments", "HOM422", "hom422", BigDecimal(15)),
      "hom4a1" -> Module("History of music in the biblical period", "HOM4A1", "hom4a1", BigDecimal(15)),
      "hom433" -> Module("Dissertation (History of Music PGT)", "hom433", "hom433", BigDecimal(60)),
    ), Map(
      "hm801" ->
        Route("History of Music", "hm801", DegreeType.Undergraduate, CourseType.UG, awardCode = "BA", isResearch = false,
          Seq("hom101", "hom102", "hom103",  "hom201", "hom203", "hom204", "hom210",  "hom340"),
          Seq("hom106", "hom115", "hom128", "hom218", "hom222", "hom3a1", "hom334", "hom335", "hom336"),
          4300001, 4300100),
      "hm802" ->
        Route("History of Music with Intercalated Year", "hm802", DegreeType.Undergraduate, CourseType.UG, awardCode = "BA", isResearch = false,
          Seq("hom101", "hom102", "hom103",  "hom201", "hom203", "hom204", "hom210",  "hom340"),
          Seq("hom106", "hom115", "hom128", "hom218", "hom222", "hom3a1", "hom334", "hom335", "hom336"),
          4300101, 4300130),
      "hm8p0" ->
        Route("History of Music (Research)", "hm8p0", DegreeType.Postgraduate, CourseType.PGR, awardCode = "PHD", isResearch = true, Seq(), Seq(), 4300201, 4300300),
      "hm8p1" ->
        Route("History of Music (Taught)", "hm8p1", DegreeType.Postgraduate, CourseType.PGT, awardCode = "MA", isResearch = false,
          Seq("hom422", "hom4a1", "hom433"), Seq("hom3a1", "hom332", "hom401", "hom402"),
          4300301, 4300350)
    ), 5300001, 5300030),
    "psp" -> Department("Public Speaking", "psp", "I", Map(
      "psp101" -> Module("Pronunciation and Enunciation", "PSP101", "psp101", BigDecimal(60)),
      "psp102" -> Module("Professional Speaking", "PSP102", "psp102", BigDecimal(60))
    ), Map(
      "xp301" ->
        Route("Public Speaking", "xp301", DegreeType.Undergraduate, CourseType.UG, awardCode = "BA", isResearch = false,
          Seq("psp101", "psp102"), Seq(),
          4400001, 4400100),
      "xp302" ->
        Route("Public Speaking with Intercalated Year", "xp302", DegreeType.Undergraduate, CourseType.UG, awardCode = "BA", isResearch = false,
          Seq("psp101", "psp102"), Seq(),
          4400101, 4400130),
      "xp3p0" ->
        Route("Public Speaking (Research)", "xp3p0", DegreeType.Postgraduate, CourseType.PGR, awardCode = "PHD", isResearch = true, Seq(), Seq(), 4400201, 4400300),
      "xp3p1" ->
        Route("Public Speaking (Taught)", "xp3p1", DegreeType.Postgraduate, CourseType.PGT, awardCode = "MA", isResearch = false, Seq(), Seq(), 4400301, 4400350)
    ), 5400001, 5400030),
    "trn" -> Department("Training Methods", "trn", "I", Map(
      "trn101" -> Module("Introduction to Tabula Training", "INT-TAB-TRNG", "trn101", BigDecimal(60)),
      "trn102" -> Module("Advanced Sitebuilder Training", "ADV-SB-TRNG", "trn102", BigDecimal(60))
    ), Map(
      "tr301" ->
        Route("Training Methods", "tr301", DegreeType.Undergraduate, CourseType.UG, awardCode = "BA", isResearch = false,
          Seq("trn101", "trn102"), Seq(),
          4500001, 4500100),
      "tr302" ->
        Route("Training Methods with Intercalated Year", "tr302", DegreeType.Undergraduate, CourseType.UG, awardCode = "BA", isResearch = false,
          Seq("trn101", "trn102"), Seq(),
          4500101, 4500130),
      "tr3p0" ->
        Route("Training Methods (Research)", "tr3p0", DegreeType.Postgraduate, CourseType.PGR, awardCode = "PHD", isResearch = true, Seq(), Seq(), 4500201, 4500300),
      "tr3p1" ->
        Route("Training Methods (Taught)", "tr3p1", DegreeType.Postgraduate, CourseType.PGT, awardCode = "MA", isResearch = false, Seq(), Seq(), 4500301, 4500350)
    ), 5500001, 5500030),
    "tss" -> Department("School of Tabula Sandbox Studies", "tss", "T", Map(
      "tss101" -> Module("Introduction to Permissions", "TSS101", "tss101", BigDecimal(15)),
      "tss102" -> Module("Coursework Management 1", "TSS102", "tss102", BigDecimal(15)),
      "tss103" -> Module("Introduction to Student Profiles", "TSS103", "tss103", BigDecimal(15)),
      "tss106" -> Module("Small Group Teaching 1", "TSS106", "tss106", BigDecimal(15)),
      "tss115" -> Module("Timetabling Students", "TSS115", "tss115", BigDecimal(30)),
      "tss129" -> Module("Exam Management and Grids", "TSS129", "tss129", BigDecimal(30)),

      "tss201" -> Module("Coursework Management 2", "TSS201", "tss201", BigDecimal(15)),
      "tss203" -> Module("Management of Monitoring Point Schemes and Points", "TSS203", "tss203", BigDecimal(15)),
      "tss204" -> Module("Principles of Student Relationships", "TSS204", "tss204", BigDecimal(15)),
      "tss210" -> Module("Advanced Marks Management", "TSS210", "tss210", BigDecimal(15)),
      "tss219" -> Module("Marking Descriptors", "TSS219", "tss219", BigDecimal(30)),
      "tss222" -> Module("Sustainable Reports", "TSS222", "tss222", BigDecimal(30)),

      "tss3a1" -> Module("Departmental Small Group Sets", "TSS3A1", "tss3a1", BigDecimal(30)),
      "tss330" -> Module("History of Tabula Development", "TSS330", "tss330", BigDecimal(30)),
      "tss339" -> Module("Dissertation (Sandbox Studies)", "TSS339", "tss339", BigDecimal(60))
    ), Map(
      "ts801" ->
        Route("Sandbox Studies", "ts801", DegreeType.Undergraduate, CourseType.UG, awardCode = "BA", isResearch = false,
          Seq("tss101", "tss102", "tss103", "tss106", "tss115", "tss129", "tss201",
            "tss203", "tss204", "tss210", "tss219", "tss222", "tss3a1", "tss330", "tss339"), Seq(),
          4600001, 4600100),
      "ts802" ->
        Route("Sandbox Studies with Intercalated Year", "ts802", DegreeType.Undergraduate, CourseType.UG, awardCode = "MBCHB", isResearch = false,
          Seq("tss101", "tss102", "tss103", "tss106", "tss115", "tss129", "tss201",
            "tss203", "tss204", "tss210", "tss219", "tss222", "tss3a1", "tss330", "tss339"), Seq(),
          4600101, 4600130),
      "ts8p0" ->
        Route("Sandbox Studies (Research)", "ts8p0", DegreeType.Postgraduate, CourseType.PGR, awardCode = "PHD", isResearch = true, Seq(), Seq(), 4600201, 4600300),
      "ts8p1" ->
        Route("Sandbox Studies (Taught)", "ts8p1", DegreeType.Postgraduate, CourseType.PGT, awardCode = "MA", isResearch = false,
          Seq("tss222", "tss3a1", "tss330"), Seq(),
          4600301, 4600350)
    ), 5600001, 5600030),
  )

  final val GradeBoundaries = Seq(
    GradeBoundary("TABULA-UG", "SAS", 1, "1", Some(80), Some(100), "N", Some(ModuleResult.Pass)),
    GradeBoundary("TABULA-UG", "SAS", 2, "1", Some(70), Some(79), "N", Some(ModuleResult.Pass)),
    GradeBoundary("TABULA-UG", "SAS", 3, "21", Some(60), Some(69), "N", Some(ModuleResult.Pass)),
    GradeBoundary("TABULA-UG", "SAS", 4, "22", Some(50), Some(59), "N", Some(ModuleResult.Pass)),
    GradeBoundary("TABULA-UG", "SAS", 5, "3", Some(40), Some(49), "N", Some(ModuleResult.Pass)),
    GradeBoundary("TABULA-UG", "SAS", 6, "F", Some(0), Some(39), "N", Some(ModuleResult.Fail)),
    GradeBoundary("TABULA-UG", "SAS", 7, GradeBoundary.WithdrawnGrade, Some(0), Some(100), "S", Some(ModuleResult.Fail)),
    GradeBoundary("TABULA-UG", "RAS", 1, "1", Some(80), Some(100), "N", Some(ModuleResult.Pass)),
    GradeBoundary("TABULA-UG", "RAS", 2, "1", Some(70), Some(79), "N", Some(ModuleResult.Pass)),
    GradeBoundary("TABULA-UG", "RAS", 3, "21", Some(60), Some(69), "N", Some(ModuleResult.Pass)),
    GradeBoundary("TABULA-UG", "RAS", 4, "22", Some(50), Some(59), "N", Some(ModuleResult.Pass)),
    GradeBoundary("TABULA-UG", "RAS", 5, "3", Some(40), Some(49), "N", Some(ModuleResult.Pass)),
    GradeBoundary("TABULA-UG", "RAS", 6, "F", Some(0), Some(39), "N", Some(ModuleResult.Fail)),
    GradeBoundary("TABULA-UG", "RAS", 7, GradeBoundary.WithdrawnGrade, Some(0), Some(100), "S", Some(ModuleResult.Fail)),
    GradeBoundary("TABULA-PG", "SAS", 1, "A+", Some(80), Some(100), "N", Some(ModuleResult.Pass)),
    GradeBoundary("TABULA-PG", "SAS", 2, "A", Some(70), Some(79), "N", Some(ModuleResult.Pass)),
    GradeBoundary("TABULA-PG", "SAS", 3, "B", Some(60), Some(69), "N", Some(ModuleResult.Pass)),
    GradeBoundary("TABULA-PG", "SAS", 4, "C", Some(50), Some(59), "N", Some(ModuleResult.Pass)),
    GradeBoundary("TABULA-PG", "SAS", 5, "D", Some(40), Some(49), "N", Some(ModuleResult.Fail)),
    GradeBoundary("TABULA-PG", "SAS", 6, "E", Some(0), Some(39), "N", Some(ModuleResult.Fail)),
    GradeBoundary("TABULA-PG", "SAS", 7, GradeBoundary.WithdrawnGrade, Some(0), Some(100), "S", Some(ModuleResult.Fail)),
    GradeBoundary("TABULA-PG", "RAS", 1, "A+", Some(80), Some(100), "N", Some(ModuleResult.Pass)),
    GradeBoundary("TABULA-PG", "RAS", 2, "A", Some(70), Some(79), "N", Some(ModuleResult.Pass)),
    GradeBoundary("TABULA-PG", "RAS", 3, "B", Some(60), Some(69), "N", Some(ModuleResult.Pass)),
    GradeBoundary("TABULA-PG", "RAS", 4, "C", Some(50), Some(59), "N", Some(ModuleResult.Pass)),
    GradeBoundary("TABULA-PG", "RAS", 5, "D", Some(40), Some(49), "N", Some(ModuleResult.Fail)),
    GradeBoundary("TABULA-PG", "RAS", 6, "E", Some(0), Some(39), "N", Some(ModuleResult.Fail)),
    GradeBoundary("TABULA-PG", "RAS", 7, GradeBoundary.WithdrawnGrade, Some(0), Some(100), "S", Some(ModuleResult.Fail)),
    GradeBoundary("TABULA-PF", "SAS", 1, "P", None, None, "S", Some(ModuleResult.Pass)),
    GradeBoundary("TABULA-PF", "SAS", 2, "F", None, None, "S", Some(ModuleResult.Fail)),
    GradeBoundary("TABULA-PF", "SAS", 3, GradeBoundary.WithdrawnGrade, None, None, "S", Some(ModuleResult.Fail)),
    GradeBoundary("TABULA-PF", "RAS", 1, "P", None, None, "S", Some(ModuleResult.Pass)),
    GradeBoundary("TABULA-PF", "RAS", 2, "F", None, None, "S", Some(ModuleResult.Fail)),
    GradeBoundary("TABULA-PF", "RAS", 3, GradeBoundary.WithdrawnGrade, None, None, "S", Some(ModuleResult.Fail)),
  )

  def randomName(id: Long, gender: Gender): Name = {
    val nameGender = gender match {
      case Gender.Male => male
      case Gender.Female => female
      case _ => nonspecific
    }

    NameGenerator.generate(nameGender, id)
  }

  def route(id: Long): Route =
    Departments
      .flatMap { case (_, d) => d.routes }
      .find { case (_, r) => r.studentsStartId <= id && r.studentsEndId >= id }
      .map { case (_, r) => r }
      .get

  def module(code: String): Module =
    Departments
      .flatMap { case (_, d) => d.modules }
      .apply(code)

  case class Department(
    name: String,
    code: String,
    facultyCode: String,
    modules: Map[String, Module],
    routes: Map[String, Route],
    staffStartId: Int,
    staffEndId: Int
  )

  case class AssessmentComponent(
    name: String,
    sequence: String,
    assessmentType: AssessmentType,
    weighting: Int
  )

  def defaultComponents: Seq[AssessmentComponent] = Seq(
    AssessmentComponent("Report (2,000 words)", "A01", AssessmentType.Essay, 30),
    AssessmentComponent("2 hour examination (Summer)", "E01", AssessmentType.SummerExam, 70)
  )

  def equalSplits: Seq[AssessmentComponent] = Seq(
    AssessmentComponent("Report (2,000 words)", "A01", AssessmentType.Essay, 33),
    AssessmentComponent("Essay", "A02", AssessmentType.Essay, 33),
    AssessmentComponent("2 hour examination (Summer)", "E01", AssessmentType.SummerExam, 34)
  )

  def smallAssessments: Seq[AssessmentComponent] = Seq(
    AssessmentComponent("Report 1 (2,000 words)", "A01", AssessmentType.Essay, 10),
    AssessmentComponent("Report 2 (2,000 words)", "A02", AssessmentType.Essay, 10),
    AssessmentComponent("Report 3 (2,000 words)", "A03", AssessmentType.Essay, 10),
    AssessmentComponent("Report 4 (2,000 words)", "A04", AssessmentType.Essay, 10),
    AssessmentComponent("Report 5 (2,000 words)", "A05", AssessmentType.Essay, 10),
    AssessmentComponent("2 hour examination (Summer)", "E01", AssessmentType.SummerExam, 50)
  )

  case class Module(name: String, shortName: String, code: String, cats: BigDecimal, components: Seq[AssessmentComponent] = defaultComponents) {
    def catsString: String = JBigDecimal(Option(cats).map(_.setScale(1, RoundingMode.HALF_UP))).toPlainString
    def fullModuleCode: String = "%s-%s".format(code.toUpperCase, catsString)
  }

  case class Route(
    name: String,
    code: String,
    degreeType: DegreeType,
    courseType: CourseType,
    awardCode: String,
    isResearch: Boolean,
    coreModules: Seq[String],
    optionalModules: Seq[String],
    studentsStartId: Int,
    studentsEndId: Int,
  ) {
    def moduleCodes: Seq[String] = coreModules ++ optionalModules
  }

  def randomMarkSeed(universityId: String, moduleCode:String): Int = {
    val seed = (universityId ++ universityId ++ moduleCode.substring(3)).toCharArray.map(char =>
      Try(char.toString.toInt).toOption.getOrElse(0) * universityId.toCharArray.apply(0).toString.toInt
    ).sum

    // bump up half of all fails so they aren't as common
    seed % 100 match {
      case i if i < 40 && i % 2 == 0 => seed + 30
      case i => seed
    }
  }

}
