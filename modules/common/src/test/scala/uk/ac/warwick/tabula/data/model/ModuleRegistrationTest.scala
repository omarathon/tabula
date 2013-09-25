package uk.ac.warwick.tabula.data.model

import uk.ac.warwick.tabula.TestBase
import org.junit.Test
import uk.ac.warwick.tabula.data.model.groups.SmallGroupSet
import scala.collection.JavaConverters._
import uk.ac.warwick.tabula.PersistenceTestBase
import uk.ac.warwick.tabula.AcademicYear

class ModuleRegistrationTest extends TestBase {
	@Test def selectionStatus {
			val stuMem = new StudentMember("0123456")
			stuMem.userId = "abcde"

			val scd: StudentCourseDetails = new StudentCourseDetails(stuMem, "0123456/1")

			val module = new Module
			module.code = "ab123"

			val modReg = new ModuleRegistration(scd, module, new java.math.BigDecimal("10"), AcademicYear(2012), "A")
			modReg.assessmentGroup = "D"
			modReg.selectionStatus = ModuleSelectionStatus.OptionalCore

			modReg.selectionStatus.description should be ("Optional Core")

			modReg.selectionStatus = ModuleSelectionStatus.fromCode("C")

			modReg.selectionStatus.description should be ("Core")
	}
}
