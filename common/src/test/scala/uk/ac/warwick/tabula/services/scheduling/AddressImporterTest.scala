package uk.ac.warwick.tabula.services.scheduling

import org.junit.After
import org.springframework.jdbc.datasource.embedded.{EmbeddedDatabase, EmbeddedDatabaseBuilder}
import uk.ac.warwick.tabula.services.scheduling.AddressImporter.AddressInfo
import uk.ac.warwick.tabula.{Mockito, TestBase}


class AddressImporterTest extends TestBase with Mockito {
	
	val sits: EmbeddedDatabase = new EmbeddedDatabaseBuilder().addScript("sits-addresses.sql").build()

	@After def fin() {
		sits.shutdown()
	}

	val addressImporter = new AddressImporterImpl
	addressImporter.sits = sits
	AddressImporter.sitsSchema = "public"
	AddressImporter.dialectRegexpLike = "regexp_matches"

	@Test
	def addresses(): Unit = {
		val expected = AddressInfo("HB1406","Heronbank","University of Warwick","Coventry",null,"CV4 7ES",null)
		val result = addressImporter.getAddressInfo("1234567")
		result.currentAddress should be (Some(expected))
		result.hallOfResidence should be (Some(expected))

		val result2 = addressImporter.getAddressInfo("7654321")
		result2.currentAddress should be (Some(AddressInfo("21 Spencer Ave","Earlsdon",null,"Coventry",null,"CV5 6BS",null)))
		result2.hallOfResidence should be (None)
	}

}
