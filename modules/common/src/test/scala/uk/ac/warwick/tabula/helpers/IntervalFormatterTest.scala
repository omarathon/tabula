package uk.ac.warwick.tabula.helpers

import uk.ac.warwick.tabula.TestBase
import org.joda.time.{DateTime, Interval}
import freemarker.template.TemplateModel
import freemarker.template.SimpleHash
import uk.ac.warwick.tabula.JavaImports._

// scalastyle:off magic.number
class IntervalFormatterTest extends TestBase {
	import IntervalFormatter.format

	// TAB-546 hourminute format changed
	
	@Test
	def sameYear {
		val open = new DateTime(2012,10,10,/**/9,0,0)
		val close = new DateTime(2012,11,5,/**/12,0,0)
		format(open, close) should be ("09:00 Wed 10<sup>th</sup> Oct - 12:00 Mon 5<sup>th</sup> Nov 2012")
		format(new Interval(open, close)) should be ("09:00 Wed 10<sup>th</sup> Oct - 12:00 Mon 5<sup>th</sup> Nov 2012")
	}

	/* When year changes, specify year both times. */
	@Test 
	def differentYear { 
		val open = new DateTime(2012,12,10,/**/9,0,0)
		val close = new DateTime(2013,1,15,/**/12,0,0)
		format(open, close) should be ("09:00 Mon 10<sup>th</sup> Dec 2012 - 12:00 Tue 15<sup>th</sup> Jan 2013")
	}

	@Test
	def partPastTheHour {
		val open = new DateTime(2012,10,10,/**/9,15,7)
		val close = new DateTime(2012,11,5,/**/14,0,7)
		format(open, close) should be ("09:15 Wed 10<sup>th</sup> Oct - 14:00 Mon 5<sup>th</sup> Nov 2012")
	}

	@Test
	def endless {
		val open = new DateTime(2012,10,10,/**/9,15,7)
		format(open) should be ("09:15 Wed 10<sup>th</sup> Oct 2012")
	}
	
	@Test def freemarker {
		val formatter = new IntervalFormatter
		
		val args: JList[TemplateModel] = JArrayList()
		
		// Use a SimpleHash as a workaround to wrapping things manually
		val model = new SimpleHash
		model.put("start", new DateTime(2012,10,10,/**/9,15,7))
		model.put("end", new DateTime(2012,11,5,/**/0,0,7))
		
		args.add(model.get("start"))
		
		formatter.exec(args) should be ("09:15 Wed 10<sup>th</sup> Oct 2012")
		
		args.add(model.get("end"))
		
		formatter.exec(args) should be ("09:15 Wed 10<sup>th</sup> Oct - 00:00 Mon 5<sup>th</sup> Nov 2012")
	}

}