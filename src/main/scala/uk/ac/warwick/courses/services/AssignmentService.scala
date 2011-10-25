package uk.ac.warwick.courses.services
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import org.springframework.stereotype.Repository
import org.hibernate.SessionFactory

trait AssignmentService {
  
}

/**
 * Service for performing actions on assignments.
 */
@Repository
class AssignmentServiceImpl @Autowired()( sessionFactory:SessionFactory ) extends AssignmentService {
	
	 
  
}