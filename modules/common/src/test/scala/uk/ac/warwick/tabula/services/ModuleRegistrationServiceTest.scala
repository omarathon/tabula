package uk.ac.warwick.tabula.services

import uk.ac.warwick.tabula.data.{ModuleRegistrationDaoComponent, ModuleRegistrationDao}
import uk.ac.warwick.tabula.{Fixtures, Mockito, TestBase}

class ModuleRegistrationServiceTest extends TestBase with Mockito {

	val mockModuleRegistrationDao = smartMock[ModuleRegistrationDao]

	trait Fixture {
		val service = new AbstractModuleRegistrationService with ModuleRegistrationDaoComponent {
			val moduleRegistrationDao = mockModuleRegistrationDao
		}
	}

	@Test
	def weightedMeanYearMark(): Unit = {
		new Fixture {
			val moduleRegistraions = Seq(
				Fixtures.moduleRegistration(BigDecimal(30), BigDecimal(100)),
				Fixtures.moduleRegistration(BigDecimal(45), BigDecimal(58)),
				Fixtures.moduleRegistration(BigDecimal(15), BigDecimal(30)),
				Fixtures.moduleRegistration(BigDecimal(7.5), BigDecimal(0)),
				Fixtures.moduleRegistration(BigDecimal(7.5), BigDecimal(97)),
				Fixtures.moduleRegistration(BigDecimal(15), BigDecimal(64))
			)
			val result = service.weightedMeanYearMark(moduleRegistraions)
			result.isDefined should be {true}
			result.get.scale should be (1)
			result.get.doubleValue() should be (64.6)

			val moduleRegistraionsWithMissingAgreedMark = Seq(
				Fixtures.moduleRegistration(BigDecimal(30), BigDecimal(100)),
				Fixtures.moduleRegistration(BigDecimal(45), BigDecimal(58)),
				Fixtures.moduleRegistration(BigDecimal(15), BigDecimal(30)),
				Fixtures.moduleRegistration(BigDecimal(7.5), null),
				Fixtures.moduleRegistration(BigDecimal(7.5), BigDecimal(97)),
				Fixtures.moduleRegistration(BigDecimal(15), BigDecimal(64))
			)
			val noResult = service.weightedMeanYearMark(moduleRegistraionsWithMissingAgreedMark)
			noResult.isDefined should be {false}
		}
	}

}
