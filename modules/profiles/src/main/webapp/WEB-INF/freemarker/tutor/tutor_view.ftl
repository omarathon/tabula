<#escape x as x?html>
	<h3>Personal Tutee: ${student.firstName} ${student.lastName} (${student.universityId})</h3>

<#if user.staff>
	<#include "tutor_form.ftl" />
	
	<#if pickedTutor??>
		<hr class="full-width">
	<br />
		<div style="text-align:right;margin-right:220px">
			<a href="<@routes.tutor_save studentUniId="${student.universityId}" tutorUniId="${pickedTutor.universityId}" />" class="btn btn-primary">Save</a>
		</div>
		<br />
		<br />
		<br />
	</#if>
</#if>
		&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;
	<a href="<@routes.profile studentUniId="${student.universityId}" />" class="btn">Return to ${student.firstName}'s profile page</a>
</#escape>
