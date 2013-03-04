<section id="course-details">
	<#if profile.studyDetails??>
		<h4>Course details</h4>
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
					<th>Intended award</th>
					<td>
						<#if profile.studyDetails.intendedAward??>
							${profile.studyDetails.intendedAward}
						</#if>
					</td>
				</tr>										
			</tbody>
			<tbody>		
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
							${profile.studyDetails.courseYearLength} years
						</#if>
						<#if profile.studyDetails.modeOfAttendance??>
							<#if profile.studyDetails.modeOfAttendance = "part-time">
								&nbsp; (full-time equivalent)
							</#if>
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
						<#elseif profile.studyDetails.expectedEndDate??>
							${profile.studyDetails.expectedEndDate} (expected)
						</#if>
					</td>
				</tr>	
			</tbody>
			<tbody>
				<tr>
					<th>UG/PG</th>
					<td>
						<#if profile.studyDetails.ugPg??>						
							${profile.studyDetails.ugPg}
						</#if>
					</td>
				</tr>
				<tr>
					<th>Attendance</th>
					<td>
						<#if profile.studyDetails.modeOfAttendance??>						
							${profile.studyDetails.modeOfAttendance}
						</#if>
					</td>
				</tr>
				<tr>
					<th>Status</th>
					<td>
						<#if profile.statusString??>
							${profile.statusString}
						</#if>
					</td>
				</tr>
			</tbody>
		</table>
	</p>
	</#if>
</section>
