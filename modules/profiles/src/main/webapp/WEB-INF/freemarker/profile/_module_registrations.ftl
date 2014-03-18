<section id="module-registrations" class="clearfix">
	<div class="pull-right">
		<a href="https://mrm.warwick.ac.uk/mrm/student/student.htm?sprCode=${((studentCourseDetails.sprCode)!)?url}&acYear=${((studentCourseYearDetails.academicYear.toString)!)?url}" target="_blank">
			View in MRM<img class="targetBlank" alt="" title="Link opens in a new window" src="/static/images/shim.gif">
		</a>
	</div>
	
	<h4>Module Registrations for ${(studentCourseYearDetails.academicYear.toString)!}</h4>
	<p><span class="muted">Module Registration Status:</span>
		<#if studentCourseYearDetails.moduleRegistrationStatus??>
			${(studentCourseYearDetails.moduleRegistrationStatus.description)!}
		<#else>
			Unknown (not in SITS)
		</#if>
	</p>
	<table class="module-registration-table">
		<tbody>
			<tr>
				<th>Code</th>
				<th>Title</th>
				<th>CATS</th>
				<th>Assess Group </th>
				<th>Select Status</th>
			</tr>
				<#list studentCourseYearDetails.moduleRegistrations as moduleRegistration>
					<tr>
						<td>${(moduleRegistration.module.code?upper_case)!}</td>
						<td>${(moduleRegistration.module.name)!}</td>
						<td>${(moduleRegistration.cats)!}</td>
						<td>${(moduleRegistration.assessmentGroup)!}</td>
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
</section>
