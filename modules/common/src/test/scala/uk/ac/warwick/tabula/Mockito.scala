package uk.ac.warwick.tabula

import org.specs2.matcher.Matchers._
import scala.reflect.ClassTag

trait Mockito extends org.specs2.mock.Mockito {
  def isEq[A](arg:A): A = argThat(equalTo(arg)).asInstanceOf[A]
  def isNotEq[A](arg:A): A = argThat(not(equalTo(arg))).asInstanceOf[A]
	def isA[A](implicit tag: ClassTag[A]): A = org.mockito.Matchers.isA(tag.runtimeClass.asInstanceOf[Class[A]])
	def isNull[A](implicit tag: ClassTag[A]): A = org.mockito.Matchers.isNull(tag.runtimeClass.asInstanceOf[Class[A]])
	def reset[A](arg:A) = org.mockito.Mockito.reset(arg)

	def verifyNoMoreInteractions(mocks: AnyRef*) = org.mockito.Mockito.verifyNoMoreInteractions(mocks : _*)
}