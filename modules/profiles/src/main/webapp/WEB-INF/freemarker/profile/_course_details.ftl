<section id="course-details">
	<h4>Course details</h4>
	<div class="data clearfix">
		<div class="col1">
			<table class="course-info">
				<tbody>
				<tr>
					<th>Route</th>
					<td>
						<#if profile.studyDetails.route??>
							${profile.studyDetails.route.name} (${profile.studyDetails.route.code?upper_case})
						</#if>
					</td>
				</tr>
				<tr>
					<th>Course code</th>
					<td>
						<#if profile.studyDetails.sitsCourseCode??>
							${profile.studyDetails.sitsCourseCode?upper_case}
						</#if>
					</td>
				</tr>
				<tr>
					<th>Status</th>
					<td>${profile.studyDetails.studentStatus}</td>
				</tr>
				<tr>
					<th>Study department</th>
					<td>
						<#if profile.studyDetails.studyDepartment??>		
							${profile.studyDetails.studyDepartment.name} (${profile.studyDetails.studyDepartment.code?upper_case})
						</#if>
					</td
				</tr>
				<tr>
					<th>Year of study</th>
					<td>${profile.studyDetails.yearOfStudy}</td>
				</tr>
				<tr>
					<th>Group</th>
					<td>${profile.groupName}</td>
				</tr>
		</table>
	</p>
	</div>
</section>

