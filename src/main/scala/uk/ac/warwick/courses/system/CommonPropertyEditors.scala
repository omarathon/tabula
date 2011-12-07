package uk.ac.warwick.courses.system
import org.springframework.beans.PropertyEditorRegistrar
import org.springframework.beans.PropertyEditorRegistry
import java.beans.PropertyEditor
import uk.ac.warwick.util.web.bind.TrimmedStringPropertyEditor

class CommonPropertyEditors extends PropertyEditorRegistrar {

	// define a neater `register` method for P.E.R.
	implicit def cleverRegistry(registry:PropertyEditorRegistry) = new {
		def register[T](editor:PropertyEditor)(implicit m:Manifest[T]) = 
				registry.registerCustomEditor(m.erasure, editor)
	}
	
	override def registerCustomEditors(registry:PropertyEditorRegistry) { 		
		registry.register[String](new TrimmedStringPropertyEditor)
	}
	
}