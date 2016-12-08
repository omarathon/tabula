<#escape x as x?html>

<input type="hidden" name="jobId" value="${jobId}" />
<input type="hidden" name="course" value="${selectCourseCommand.course.code}" />
<#list selectCourseCommand.routes as route>
	<input type="hidden" name="routes" value="${route.code}" />
</#list>
<input type="hidden" name="yearOfStudy" value="${selectCourseCommand.yearOfStudy}" />
<#list gridOptionsCommand.predefinedColumnIdentifiers as column>
	<input type="hidden" name="predefinedColumnIdentifiers" value="${column}" />
</#list>
<#list gridOptionsCommand.customColumnTitles as column>
	<input type="hidden" name="customColumnTitles[${column_index}]" value="${column}" />
</#list>
<input type="hidden" name="nameToShow" value="${gridOptionsCommand.nameToShow}" />
<input type="hidden" name="yearsToShow" value="${gridOptionsCommand.yearsToShow}" />
<input type="hidden" name="marksToShow" value="${gridOptionsCommand.marksToShow}" />
<input type="hidden" name="moduleNameToShow" value="${gridOptionsCommand.moduleNameToShow}" />

</#escape>