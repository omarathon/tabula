package uk.ac.warwick.courses
import org.junit.Before
import org.jmock.Mockery
import org.junit.After
import org.jmock.integration.junit4.JUnit4Mockery

trait Mocking {
    var mockery:Mockery = null
	@Before def makeMockery = mockery = new JUnit4Mockery
	@After def checkMockery = mockery.assertIsSatisfied
}