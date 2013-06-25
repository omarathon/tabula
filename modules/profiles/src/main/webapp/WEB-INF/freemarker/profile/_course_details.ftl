<section id="course-details" class="clearfix">
	<#if profile.hasCurrentEnrolment?? && profile.studyDetails??>
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
							<#if (profile.studyDetails.route.department.name)??>
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
							<#if profile.studyDetails.modeOfAttendance.code != "F">
								(full-time equivalent)
							</#if>
						</#if>
					</td>
				</tr>
				<tr>
					<th>Start date</th>
					<td>
						<#if profile.studyDetails.beginDate??>
							<@fmt.date date=profile.studyDetails.beginDate includeTime=false />
						</#if>
					</td>
				</tr>
				<tr>
					<th>End date</th>
					<td>
						<#if profile.studyDetails.endDate??>
							<@fmt.date date=profile.studyDetails.endDate includeTime=false />
						<#elseif profile.studyDetails.expectedEndDate??>
							<@fmt.date date=profile.studyDetails.expectedEndDate includeTime=false/> (expected)
						</#if>
					</td>
				</tr>
			</tbody>
			<tbody>
				<tr>
					<th>UG/PG</th>
					<td>
						<#if (profile.studyDetails.route.degreeType)??>
							${profile.studyDetails.route.degreeType.toString}
						</#if>
					</td>
				</tr>
				<tr>
					<th>Attendance</th>
					<td>
						<#if profile.studyDetails.modeOfAttendance??>
							${profile.studyDetails.modeOfAttendance.fullNameToDisplay}
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
	<#else>
		<p class="text-warning"><i class="icon-warning-sign"></i> No course details are recorded in Tabula for the current year.</p>
	</#if>
</section>
