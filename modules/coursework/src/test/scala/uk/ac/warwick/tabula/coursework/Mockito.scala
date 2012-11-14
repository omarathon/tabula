package uk.ac.warwick.tabula.coursework
import org.specs.specification.DefaultExampleExpectationsListener

trait Mockito extends org.specs.mock.Mockito with DefaultExampleExpectationsListener {
	def isEq[T](arg:T) = argThat(org.hamcrest.Matchers.equalTo(arg)) 
}