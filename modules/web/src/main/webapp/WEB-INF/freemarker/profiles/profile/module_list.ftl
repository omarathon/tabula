<#escape x as x?html>

<#if user.staff>
	<div class="pull-right">
		<@routes.profiles.mrm_link studentCourseDetails command.studentCourseYearDetails />
		View in MRM<img class="targetBlank" alt="" title="Link opens in a new window" src="/static/images/shim.gif"/> </a>
	</div>
</#if>

<h4>Modules</h4>
<p><span class="muted">Module Registration Status:</span>
	<#if command.studentCourseYearDetails.moduleRegistrationStatus??>
		${(command.studentCourseYearDetails.moduleRegistrationStatus.description)!}
	<#else>
		Unknown (not in SITS)
	</#if>
</p>

<#if command.studentCourseYearDetails.hasModuleRegistrations>
	<#assign latestYear = command.studentCourseYearDetails.latest />
	<#assign showModuleResultsForLatest = false />
	<#list moduleRegs as moduleRegistration>
		<#if moduleRegistration.agreedMark?has_content || moduleRegistration.agreedGrade?has_content>
			<#assign showModuleResultsForLatest = true />
		</#if>
	</#list>
	<#assign showModuleResults = features.showModuleResults && (!latestYear || showModuleResultsForLatest) />
	<table class="module-registration-table">
		<tbody>
			<tr>
				<th>Code</th>
				<th>Title</th>
				<th>CATS</th>
				<#if latestYear>
					<th>Assess</th>
					<#if command.studentCourseYearDetails.hasModuleRegistrationWithNonStandardOccurrence>
						<th>Occur</th>
					</#if>
				</#if>
				<#if showModuleResults>
					<th>Mark</th>
					<th>Grade</th>
				</#if>
				<th>Status</th>
			</tr>
			<#list moduleRegs as moduleRegistration>
				<tr>
					<td>${(moduleRegistration.module.code?upper_case)!}</td>
					<td>${(moduleRegistration.module.name)!}</td>
					<td>${(moduleRegistration.cats)!}</td>

					<#if latestYear>
						<td>${(moduleRegistration.assessmentGroup)!}</td>
						<#if command.studentCourseYearDetails.hasModuleRegistrationWithNonStandardOccurrence>
							<td>${(moduleRegistration.occurrence)!}</td>
						</#if>
					</#if>

					<#if showModuleResults>
						<td>${(moduleRegistration.agreedMark)!}</td>
						<td>${(moduleRegistration.agreedGrade)!}</td>
					</#if>

					<td>
						<#if moduleRegistration.selectionStatus??>
							${(moduleRegistration.selectionStatus.description)!}
						<#else>
							-
						</#if>
					</td>
				</tr>
			</#list>
		</tbody>
	</table>
<#else>
	<em>There are no module registrations for this academic year</em>
</#if>

</#escape>