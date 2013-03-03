package uk.ac.warwick.tabula
import org.specs.specification.DefaultExampleExpectationsListener

trait Mockito extends org.specs.mock.Mockito with DefaultExampleExpectationsListener {
	def isEq[A](arg:A) = argThat(org.hamcrest.Matchers.equalTo(arg))
	def isA[A](implicit m: Manifest[A]) = org.mockito.Matchers.isA(m.erasure.asInstanceOf[Class[A]])
	def isNull[A](implicit m: Manifest[A]) = org.mockito.Matchers.isNull(m.erasure.asInstanceOf[Class[A]])
}