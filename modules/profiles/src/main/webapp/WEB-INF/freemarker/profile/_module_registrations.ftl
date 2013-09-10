<section id="module-registrations" class="clearfix">
	<h4>Module Registrations for ${(studentCourseDetails.latestStudentCourseYearDetails.academicYear.toString)!}</h4>
	<p><span class="muted">Module Registration Status:</span> ${(studentCourseDetails.latestStudentCourseYearDetails.moduleRegistrationStatus)!}</p>
	<table class="module-registration-table">
		<tbody>
			<tr>
				<th>Code</th>
				<th>Title</th>
				<th>Year</th>
				<th>CATS</th>
				<th>Assessment Group </th>
				<th>Selection Status</th>
			</tr>
			<#list studentCourseDetails.moduleRegistrations as moduleRegistration>
				<tr>
					<td>${(moduleRegistration.moduleCode?upper_case)!}</td>
					<td>${(moduleRegistration.moduleCode)!}</td>
					<td>${(moduleRegistration.academicYear.toString)!}</td>
					<td>${(moduleRegistration.cats)!}</td>
					<td>${(moduleRegistration.assessmentGroup)!}</td>
					<td>${(moduleRegistration.selectionStatus.convertToValue)!}</td>
				</tr>
			</#list>
		</tbody>
	</table>
</section>
