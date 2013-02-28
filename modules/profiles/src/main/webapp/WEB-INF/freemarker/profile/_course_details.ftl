<section id="course-details">
	<#if profile.studyDetails??>
		<h4>Course details</h4>
		<div class="data clearfix">
			<div class="col1">
				<table class="course-info">
					<tbody>
					
					<!-- about the course -->
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
						<th>Department</th>
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
					<tr>
						<th>Intended Award</th>
						<td>
							<#if profile.studyDetails.intendedAward??>
								${profile.studyDetails.intendedAward}
							</#if>
						</td>
					</tr>										
					

					<!-- about time -->
					<tr>
						<th>Year of study</th>
						<td>
							<#if profile.studyDetails.yearOfStudy??>
								${profile.studyDetails.yearOfStudy}
							</#if>
						</td>
					</tr>
					<tr>
						<th>Length of course</th>
						<td>
							<#if profile.studyDetails.courseYearLength??>
								${profile.studyDetails.courseYearLength}
							</#if>
						</td>
					</tr>
					<tr>
						<th>Start date</th>
						<td>
							<#if profile.studyDetails.beginDate??>
								${profile.studyDetails.beginDate}
							</#if>
						</td>
					</tr>					
					<tr>
						<th>End date</th>
						<td>
							<#if profile.studyDetails.endDate??>
								${profile.studyDetails.endDate}
							<#else>
								${profile.studyDetails.expectedEndDate} (expected)
							</#if>
						</td>
					</tr>					
					
					<!-- about the student -->
					<tr>
						<th>Group</th>
						<td>
							<#if profile.groupName??>						
								${profile.groupName}
							</#if>
						</td>
					</tr>
					<tr>
						<th>Source of funding</th>
						<td>
							<#if profile.studyDetails.fundingSource??>						
								${profile.studyDetails.fundingSource}
							</#if>
						</td>
					</tr>
					<tr>
						<th>Status</th>
						<td>
							<#if profile.studyDetails.studentStatus??>
								${profile.studyDetails.studentStatus}
							</#if>
						</td>
					</tr>
			</table>
		</p>
		</div>
	</#if>
</section>

