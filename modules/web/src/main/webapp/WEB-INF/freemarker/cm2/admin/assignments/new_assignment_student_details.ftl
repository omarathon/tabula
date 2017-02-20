<#assign spring=JspTaglibs["/WEB-INF/tld/spring.tld"]>
<#assign f=JspTaglibs["/WEB-INF/tld/spring-form.tld"]>
<#escape x as x?html>
<#import "*/assignment_components.ftl" as components />
	<#--
	HFC-166 Don't use #compress on this file because
	the comments textarea needs to maintain newlines.
	-->

<div class="assignment-header">
	<h1>Create assignment</h1>
	<h4 class="with-related"><span class="muted">for</span> <@fmt.module_name module /></h4>
</div>
<div class="fix-area assignment-student-details">
	<#assign actionUrl><@routes.cm2.assignemnts_students_new assignment /></#assign>
	<@f.form method="post" action=actionUrl>
		<@components.set_wizard true 'students'  />
		<@f.errors cssClass="error form-errors" />
		<#assign newRecord=true />
	<div>
		<#include "_student_fields.ftl" />
	</div>
		<div class="fix-footer">
			<input
				type="submit"
				class="btn btn-primary"
				name="${ManageAssignmentMappingParameters.createAndAddMarkers}"
				value="Save and continue"
			/>
			<input
				type="submit"
				class="btn btn-primary"
				name="${ManageAssignmentMappingParameters.createAndAddStudents}"
				value="Save and exit"
			/>
		</div>
	</@f.form>
</div>
</#escape>