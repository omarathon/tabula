package uk.ac.warwick.tabula
import org.specs.specification.DefaultExampleExpectationsListener
import scala.reflect.ClassTag

trait Mockito extends org.specs.mock.Mockito with DefaultExampleExpectationsListener {
  def isEq[A](arg:A) = argThat(org.hamcrest.Matchers.equalTo(arg))
	def isA[A](implicit tag: ClassTag[A]) = org.mockito.Matchers.isA(tag.runtimeClass.asInstanceOf[Class[A]])
	def isNull[A](implicit tag: ClassTag[A]) = org.mockito.Matchers.isNull(tag.runtimeClass.asInstanceOf[Class[A]])
	def reset[A](arg:A) = org.mockito.Mockito.reset(arg)
}