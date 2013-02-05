<#escape x as x?html>
<h2>Personal Tutee: ${student.firstName} ${student.lastName} (${student.universityId})</h2>

<#if user.staff>
	<#include "tutor_form.ftl" />

	<hr class="full-width" />
</#if>

</#escape>