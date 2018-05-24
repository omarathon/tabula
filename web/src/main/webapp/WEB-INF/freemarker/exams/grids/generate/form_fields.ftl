<#escape x as x?html>

<#macro select_course_fields>
	<#if jobId??>
		<input type="hidden" name="jobId" value="${jobId}" />
	</#if>
	<#list selectCourseCommand.courses as course>
		<input type="hidden" name="courses" value="${course.code}" />
	</#list>
	<#list selectCourseCommand.routes as route>
		<input type="hidden" name="routes" value="${route.code}" />
	</#list>
	<#if selectCourseCommand.yearOfStudy??><input type="hidden" name="yearOfStudy" value="${selectCourseCommand.yearOfStudy}" /></#if>
	<#if selectCourseCommand.levelCode??><input type="hidden" name="levelCode" value="${selectCourseCommand.levelCode}" /></#if>
	<input type="hidden" name="includeTempWithdrawn" value="${selectCourseCommand.includeTempWithdrawn?string('true','false')}" />
	<#list selectCourseCommand.courseYearsToShow as column>
		<input  type ="hidden" name="courseYearsToShow" value="${column}" />
	</#list>
</#macro>

<#macro grid_options_fields>
	<#list gridOptionsCommand.predefinedColumnIdentifiers as column>
		<input type="hidden" name="predefinedColumnIdentifiers" value="${column}" />
	</#list>
	<#list gridOptionsCommand.customColumnTitles as column>
		<input type="hidden" name="customColumnTitles[${column_index}]" value="${column}" />
	</#list>
	<input type="hidden" name="nameToShow" value="${gridOptionsCommand.nameToShow.toString}" />
	<input type="hidden" name="marksToShow" value="${gridOptionsCommand.marksToShow}" />
	<input type="hidden" name="componentsToShow" value="${gridOptionsCommand.componentsToShow}" />
	<input type="hidden" name="componentSequenceToShow" value="${gridOptionsCommand.componentSequenceToShow}" />
	<input type="hidden" name="moduleNameToShow" value="${gridOptionsCommand.moduleNameToShow.toString}" />
	<input type="hidden" name="layout" value="${gridOptionsCommand.layout}" />
	<input type="hidden" name="yearMarksToUse" value="${gridOptionsCommand.yearMarksToUse}" />
	<input type="hidden" name="mandatoryModulesAndYearMarkColumns" value="${gridOptionsCommand.mandatoryModulesAndYearMarkColumns?c}" />
</#macro>

</#escape>
