package uk.ac.warwick.tabula.helpers

import uk.ac.warwick.tabula.TestBase
import org.junit.Test

class StringUtilsTest extends TestBase with StringUtils {
	
	@Test def superString {
		val empty:String = ""
		val whitespace:String = "  "
		val nullString:String = null
		
		empty.hasText should be (false)
		empty.hasLength should be (false)
		
		whitespace.hasText should be (false)
		whitespace.hasLength should be (true)
		
		// should even work on null values! as long as the variable is String type
		nullString.hasText should be (false)
		nullString.hasText should be (false)
		
		"bo".orEmpty should be ("bo")
		empty.orEmpty should be ("")
		whitespace.orEmpty should be ("  ")
		nullString.orEmpty should be ("")
		
		"bo".maybeText should be (Some("bo"))
		empty.maybeText should be (None)
		whitespace.maybeText should be (None)
		nullString.maybeText should be (None)
		
		"bo".textOrEmpty should be ("bo")
		empty.textOrEmpty should be ("")
		whitespace.textOrEmpty should be ("")
		nullString.textOrEmpty should be ("")
	}
}