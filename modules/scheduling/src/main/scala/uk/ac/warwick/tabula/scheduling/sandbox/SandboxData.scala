package uk.ac.warwick.tabula.scheduling.sandbox

import uk.ac.warwick.tabula.data.model.DegreeType
import uk.co.halfninja.randomnames.NameGenerators
import uk.co.halfninja.randomnames.Gender._
import uk.ac.warwick.tabula.data.model.Gender
import uk.ac.warwick.tabula.data.model.CourseType

object SandboxData {
	final val NameGenerator = NameGenerators.standardGenerator()
	type NameGender = uk.co.halfninja.randomnames.Gender
	
	final val Departments = Map(
		"arc" -> Department("School of Architecture", "arc", "S", Map(
			"arc101" -> Module("Introduction to Architecture", "arc101"),
			"arc102" -> Module("Architectural Design 1", "arc102"),
			"arc103" -> Module("Introduction to Architectural History", "arc103"),
			"arc106" -> Module("Architectural Technology 1", "arc106"),
			"arc115" -> Module("20th Century Architecture", "arc115"),
			"arc129" -> Module("Environmental Design and Services", "arc129"),
			"arc201" -> Module("Architectural Technology 2", "arc201"),
			"arc203" -> Module("Professional Practice and Management", "arc203"),
			"arc204" -> Module("Principles and Theories of Architecture", "arc204"),
			"arc210" -> Module("The Place of Houses", "arc210"),
			"arc219" -> Module("Tectonic Practice", "arc219"),
			"arc222" -> Module("Sustainable Principles", "arc222"),
			"arc3a1" -> Module("Integrating Technology", "arc3a1"),
			"arc330" -> Module("History of Modern Architecture", "arc330"),
			"arc339" -> Module("Dissertation (Architecture)", "arc339")
		), Map(
			"ac801" -> 
				Route("Architecture", "ac801", DegreeType.Undergraduate, CourseType.UG, false, 
					Seq("arc101", "arc102", "arc103", "arc106", "arc115", "arc129", "arc201", 
						"arc203", "arc204", "arc210", "arc219",	"arc222", "arc3a1", "arc330", "arc339"), 
				4200001, 4200100),
			"ac802" -> 
				Route("Architecture with Intercalated Year", "ac802", DegreeType.Undergraduate, CourseType.UG, false, 
					Seq("arc101", "arc102", "arc103", "arc106", "arc115", "arc129", "arc201", 
						"arc203", "arc204", "arc210", "arc219",	"arc222", "arc3a1", "arc330", "arc339"), 
				4200101, 4200130),
			"ac8p0" -> 
				Route("Architecture (Research)", "ac8p0", DegreeType.Postgraduate, CourseType.PGR, true, Seq(), 4200201, 4200300),
			"ac8p1" -> 
				Route("Architecture (Taught)", "ac8p1", DegreeType.Postgraduate, CourseType.PGT, false, 
					Seq("arc222", "arc3a1", "arc330"), 
				4200301, 4200350)
		)),
		"hom" -> Department("History of Music", "hom", "A", Map(
			"hom101" -> Module("History of Musical Techniques", "hom101"),
			"hom102" -> Module("Introduction to Ethnomusicology", "hom102"),
			"hom103" -> Module("The Long Nineteenth Century", "hom103"),
			"hom106" -> Module("History of Composition", "hom106"),
			"hom115" -> Module("20th Century Music", "hom115"),
			"hom129" -> Module("Theory and Analysis", "hom129"),
			"hom201" -> Module("Russian and Soviet Music, 1890-1975", "hom201"),
			"hom203" -> Module("Studies in Popular Music", "hom203"),
			"hom204" -> Module("History of Opera", "hom204"),
			"hom210" -> Module("Writing Practices in Music", "hom210"),
			"hom219" -> Module("Popular Music and Theories of Mass Culture", "hom219"),
			"hom222" -> Module("Late 19th and Early 20th Century English Song", "hom222"),
			"hom3a1" -> Module("Britten's Chamber Operas", "hom3a1"),
			"hom330" -> Module("Influences of Hip-hop on Popular Culture", "hom330"),
			"hom339" -> Module("Dissertation (History of Music)", "hom339")
		), Map(
			"hm801" -> 
				Route("History of Music", "hm801", DegreeType.Undergraduate, CourseType.UG, false,
					Seq("hom101", "hom102", "hom103", "hom106", "hom115", "hom129", "hom201", 
						"hom203", "hom204", "hom210", "hom219",	"hom222", "hom3a1", "hom330", "hom339"), 
				4300001, 4300100),
			"hm802" -> 
				Route("History of Music with Intercalated Year", "hm802", DegreeType.Undergraduate, CourseType.UG, false,
					Seq("hom101", "hom102", "hom103", "hom106", "hom115", "hom129", "hom201", 
						"hom203", "hom204", "hom210", "hom219",	"hom222", "hom3a1", "hom330", "hom339"), 
				4300101, 4300130),
			"hm8p0" -> 
				Route("History of Music (Research)", "hm8p0", DegreeType.Postgraduate, CourseType.PGR, true, Seq(), 4300201, 4300300),
			"hm8p1" -> 
				Route("History of Music (Taught)", "hm8p1", DegreeType.Postgraduate, CourseType.PGT, false,
					Seq("hom222", "hom3a1", "hom330"), 
				4300301, 4300350)
		))
	)
	
	def randomName(id: Long, gender: Gender) = {
		val nameGender = gender match {
			case Gender.Male => male
			case Gender.Female => female
			case _ => nonspecific
		}
		
		NameGenerator.generate(nameGender, id)
	}
	
	def route(id: Long) = 
		Departments
			.flatMap { case (code, d) => d.routes }
			.find { case (code, r) => r.studentsStartId <= id && r.studentsEndId >= id }
			.map { case (code, r) => r }
			.get
	
	case class Department(
		val name: String, 
		val code: String, 
		val facultyCode: String, 
		val modules: Map[String, Module], 
		val routes: Map[String, Route]
	)
	case class Module(val name: String, val code: String)
	case class Route(
		val name: String, 
		val code: String, 
		val degreeType: DegreeType,
		val courseType: CourseType,
		val isResearch: Boolean,
		val moduleCodes: Seq[String],
		val studentsStartId: Int,
		val studentsEndId: Int
	)
}