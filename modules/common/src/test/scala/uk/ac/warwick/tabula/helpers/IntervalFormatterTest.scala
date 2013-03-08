package uk.ac.warwick.tabula.helpers

import uk.ac.warwick.tabula.TestBase
import org.joda.time.{DateTime, Interval}
import freemarker.template.TemplateModel
import freemarker.template.SimpleHash

class IntervalFormatterTest extends TestBase {
	import IntervalFormatter.format

	@Test
	def sameYear {
		val open = new DateTime(2012,10,10,/**/9,0,0)
		val close = new DateTime(2012,11,5,/**/12,0,0)
		format(open, close) should be ("9am Wed 10th Oct - 12 noon Mon 5th Nov 2012")
		format(new Interval(open, close)) should be ("9am Wed 10th Oct - 12 noon Mon 5th Nov 2012")
	}

	/* When year changes, specify year both times. */
	@Test 
	def differentYear { 
		val open = new DateTime(2012,12,10,/**/9,0,0)
		val close = new DateTime(2013,01,15,/**/12,0,0)
		format(open, close) should be ("9am Mon 10th Dec 2012 - 12 noon Tue 15th Jan 2013")
	}

	@Test
	def partPastTheHour {
		val open = new DateTime(2012,10,10,/**/9,15,7)
		val close = new DateTime(2012,11,5,/**/14,0,7)
		format(open, close) should be ("9:15am Wed 10th Oct - 2pm Mon 5th Nov 2012")
	}

	@Test
	def endless {
		val open = new DateTime(2012,10,10,/**/9,15,7)
		format(open) should be ("9:15am Wed 10th Oct 2012")
	}
	
	@Test def freemarker {
		val formatter = new IntervalFormatter
		
		val args: java.util.List[TemplateModel] = ArrayList()
		
		// Use a SimpleHash as a workaround to wrapping things manually
		val model = new SimpleHash
		model.put("start", new DateTime(2012,10,10,/**/9,15,7))
		model.put("end", new DateTime(2012,11,5,/**/0,0,7))
		
		args.add(model.get("start"))
		
		formatter.exec(args) should be ("9:15am Wed 10th Oct 2012")
		
		args.add(model.get("end"))
		
		formatter.exec(args) should be ("9:15am Wed 10th Oct - 12 midnight Mon 5th Nov 2012")
	}

}