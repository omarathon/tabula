<section id="course-details">
	<#if profile.studyDetails??>
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
					<#if profile.studyDetails.studentStatus??>
						<tr>
							<th>Status</th>
							<td>${profile.studyDetails.studentStatus}</td>
						</tr>
					</#if>
					<tr>
						<th>Study department</th>
						<td>
							<#if profile.studyDetails.studyDepartment??>		
								${profile.studyDetails.studyDepartment.name} (${profile.studyDetails.studyDepartment.code?upper_case})
							<#else>
								<#if profile.studyDetails.route.department.name??>
									${profile.studyDetails.route.department.name}
								</#if>							
							</#if>
						</td
					</tr>
					<#if profile.studyDetails.yearOfStudy??>
						<tr>
							<th>Year of study</th>
							<td>${profile.studyDetails.yearOfStudy}</td>
						</tr>
					</#if>
					<tr>
						<th>Group</th>
						<td>${profile.groupName}</td>
					</tr>
			</table>
		</p>
		</div>
	</#if>
</section>

