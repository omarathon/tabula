package uk.ac.warwick.tabula.web.views

import uk.ac.warwick.tabula.TestBase
import scala.collection.JavaConverters._

class UriParserTest extends TestBase {

	val uriParser: UriParser = new UriParser

	@Test def encodeQueryStringCorrectly(): Unit = {
		val crazyQueryString = "jobId=fdd75224-c9b9-4d1c-9624-c975916a0c86&courses=[1,2]"
		uriParser.exec(Seq(crazyQueryString).asJava) should be("jobId=fdd75224-c9b9-4d1c-9624-c975916a0c86&courses=%5B1,2%5D")
	}

	@Test def parseURI(): Unit = {
		val absolute = "https://thisisfine.com/goodpoint?jobId=fdd75224-c9b9-4d1c-9624-c975916a0c86&courses=[1,2]"
		uriParser.exec(Seq(absolute).asJava) should be("https://thisisfine.com/goodpoint?jobId=fdd75224-c9b9-4d1c-9624-c975916a0c86&courses=%5B1,2%5D")
	}

	@Test def handleBadURI(): Unit = {
		uriParser.exec(Seq("").asJava).asInstanceOf[String] should be("")
		uriParser.exec(Seq.empty.asJava).asInstanceOf[String] should be("")
	}


}