package uk.ac.warwick.courses.helpers

import uk.ac.warwick.courses.TestBase
import org.junit.Test

class StringUtilsTest extends TestBase {
	import uk.ac.warwick.courses.helpers.StringUtils._
	
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
	}
}