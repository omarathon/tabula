package uk.ac.warwick.courses.services
import org.hibernate.SessionFactory
import org.springframework.stereotype.Repository
import org.springframework.transaction.annotation.Transactional
import uk.ac.warwick.courses.data.Daoisms
import uk.ac.warwick.courses.web.forms.AddAssignmentForm
import org.springframework.beans.factory.annotation.Autowired

trait AssignmentService {
  def create(form:AddAssignmentForm)
}

/**
 * Service for performing actions on assignments.
 */
@Repository
class AssignmentServiceImpl @Autowired()(val sessionFactory:SessionFactory) extends AssignmentService with Daoisms {
	
  def this() { this(null) } /* for CGLIB */
  
  @Transactional
  def create(form:AddAssignmentForm) = {
    val assignment = form.createAssignment
    session.save(assignment)
    
  }
  
}