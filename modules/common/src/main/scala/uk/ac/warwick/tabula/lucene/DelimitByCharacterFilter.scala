package uk.ac.warwick.tabula.lucene

import org.apache.lucene.analysis.TokenFilter
import org.apache.lucene.analysis.TokenStream
import uk.ac.warwick.tabula.helpers.StringUtils._
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute

final class DelimitByCharacterFilter(val source: TokenStream, val character: Char) extends TokenFilter(source) {
	
	val term = addAttribute(classOf[CharTermAttribute])
	
	var useRemainingText = false
	var remainingText: String = _
	
	override def incrementToken(): Boolean = {
		var text = if (!useRemainingText) {
			if (!source.incrementToken()) ""
			else new String(term.buffer, 0, term.length)
		} else {
			useRemainingText = false
			remainingText
		}
		
		if (text.hasText) {
		
			val matchedLength = text.indexOf(character)
			if (matchedLength != -1) {
				useRemainingText = true
				
				val firstToken = text.substring(0, matchedLength)
				val secondToken = text.substring(matchedLength + 1, text.length)
				
				remainingText = secondToken
				
				setTermBuffer(firstToken)
			} else {
				setTermBuffer(text)
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