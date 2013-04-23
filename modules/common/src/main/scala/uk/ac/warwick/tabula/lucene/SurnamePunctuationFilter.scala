package uk.ac.warwick.tabula.lucene

import org.apache.lucene.analysis.TokenFilter
import org.apache.lucene.analysis.TokenStream
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute
import uk.ac.warwick.tabula.helpers.StringUtils._

final class SurnamePunctuationFilter(val source: TokenStream) extends TokenFilter(source) {
	import SurnamePunctuationFilter._
	
	sealed abstract class State
	case object AwaitingInput extends State
	case object ParsingWords extends State
	case object StrippingPunctuation extends State
	
	val term = addAttribute(classOf[CharTermAttribute])
	
	var remainingText: String = _
	var wholeWordText: String = null
	var state: State = AwaitingInput
	
	override def incrementToken(): Boolean = {
		var text = state match {
			case AwaitingInput => {
				if (!source.incrementToken()) return false
				
				wholeWordText = new String(term.buffer, 0, term.length)
				wholeWordText
			}
			case ParsingWords => remainingText
			case StrippingPunctuation => wholeWordText
		}
		
		if (text.hasText) {
		
			if (state == StrippingPunctuation) {
				for (c <- Apostrophes) text = text.replace(c.toString, "")
				
				state = AwaitingInput
				setTermBuffer(text)
			} else {
			
				var minPoz: Int = -1
				for (c <- Apostrophes if (text.indexOf(c) != -1)) 
					minPoz = if (minPoz == -1) text.indexOf(c) else scala.math.min(text.indexOf(c), minPoz)
				
				if (minPoz != -1) {
					val firstToken = text.substring(0, minPoz)
					val secondToken = text.substring(minPoz + 1, text.length)
					
					remainingText = secondToken
					
					state = ParsingWords
					
					setTermBuffer(firstToken)
				} else if (state == ParsingWords) {
					state = StrippingPunctuation
					
					setTermBuffer(text)
				}
			}
				
			true
		} else {
			false
		}
	}
	
	private def setTermBuffer(buffer: String) = {
		val length = buffer.length
		
		// This is a no-op if the length is shorter or the same
		term.resizeBuffer(length)
		
		// Copy characters into the target buffer
		buffer.getChars(0, length, term.buffer, 0)
		term.setLength(length)
	}
}

object SurnamePunctuationFilter {
	val Apostrophes = "'\u2019"
}