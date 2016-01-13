package uk.ac.warwick.tabula.services

import uk.ac.warwick.tabula.data.model.ModuleSelectionStatus
import uk.ac.warwick.tabula.data.{ModuleRegistrationDao, ModuleRegistrationDaoComponent}
import uk.ac.warwick.tabula.{AcademicYear, Fixtures, Mockito, TestBase}

class ModuleRegistrationServiceTest extends TestBase with Mockito {

	val mockModuleRegistrationDao = smartMock[ModuleRegistrationDao]

	trait Fixture {
		val service = new AbstractModuleRegistrationService with ModuleRegistrationDaoComponent {
			val moduleRegistrationDao = mockModuleRegistrationDao
		}
	}

	@Test
	def weightedMeanYearMark(): Unit = {
		val module = Fixtures.module("xx101") // Doesn't matter if they're all for the same module
		new Fixture {
			val moduleRegistrations = Seq(
				Fixtures.moduleRegistration(null, module, BigDecimal(30).underlying, null, agreedMark = BigDecimal(100)),
				Fixtures.moduleRegistration(null, module, BigDecimal(45).underlying, null, agreedMark = BigDecimal(58)),
				Fixtures.moduleRegistration(null, module, BigDecimal(15).underlying, null, agreedMark = BigDecimal(30)),
				Fixtures.moduleRegistration(null, module, BigDecimal(7.5).underlying, null, agreedMark = BigDecimal(0)),
				Fixtures.moduleRegistration(null, module, BigDecimal(7.5).underlying, null, agreedMark = BigDecimal(97)),
				Fixtures.moduleRegistration(null, module, BigDecimal(15).underlying, null, agreedMark = BigDecimal(64))
			)
			val result = service.weightedMeanYearMark(moduleRegistrations)
			result.isDefined should be {true}
			result.get.scale should be (1)
			result.get.doubleValue() should be (64.6)

			val moduleRegistrationsWithMissingAgreedMark = Seq(
				Fixtures.moduleRegistration(null, module, BigDecimal(30).underlying, null, agreedMark = BigDecimal(100)),
				Fixtures.moduleRegistration(null, module, BigDecimal(45).underlying, null, agreedMark = BigDecimal(58)),
				Fixtures.moduleRegistration(null, module, BigDecimal(15).underlying, null, agreedMark = BigDecimal(30)),
				Fixtures.moduleRegistration(null, module, BigDecimal(7.5).underlying, null, agreedMark = null),
				Fixtures.moduleRegistration(null, module, BigDecimal(7.5).underlying, null, agreedMark = BigDecimal(97)),
				Fixtures.moduleRegistration(null, module, BigDecimal(15).underlying, null, agreedMark = BigDecimal(64))
			)
			val noResult = service.weightedMeanYearMark(moduleRegistrationsWithMissingAgreedMark)
			noResult.isDefined should be {false}
		}
	}

	@Test
	def overcattedModuleSubsets(): Unit = {
		new Fixture {
			val scd = Fixtures.student().mostSignificantCourse
			val academicYear = AcademicYear(2014)
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
			val result = service.overcattedModuleSubsets(scd.latestStudentCourseYearDetails.toGenerateExamGridEntity())
			// There are 81 CATS of core modules, leaving 39 to reach the normal load of 120
			// All the options are 15 CATS, so there are 5 combinations of modules that are valid (4 with 3 in each and 1 with 4)
			result.size should be (5)
		}
	}

}
