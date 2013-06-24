<section id="course-details">
	<h4>Course details</h4>
	<table class="course-info">
		<tbody>
			<tr>
				<th>Route</th>
				<td>${(studentCourseDetails.route.name)!} (${(studentCourseDetails.route.code?upper_case)!})
				</td>
			</tr>
			<#if profile.studentCourseDetails?size lt 2>
				<tr>
					<th>Course</th>
					<td><@fmt.course_description studentCourseDetails /></td>
				</tr>
			</#if>
			<tr>
				<th>Department</th>
				<td>${(studentCourseDetails.department.name)!} (${(studentCourseDetails.department.code?upper_case)})
				</td
			</tr>
			<tr>
				<th>Intended award</th>
					<td>${(studentCourseDetails.awardCode)!}</td>
			</tr>
		</tbody>
		<tbody>
			<tr>
				<th>Year of study</th>
				<td>${(studentCourseDetails.latestStudentCourseYearDetails.yearOfStudy)!}
				</td>
			</tr>
	 		<tr>
				<th>Length of course</th>
				<td>
					<#if studentCourseDetails.courseYearLength??>
						${studentCourseDetails.courseYearLength} years
					</#if>
					<#if studentCourseDetails.modeOfAttendance??>
						<#if studentCourseDetails.latestStudentCourseYearDetails.modeOfAttendance.code != "F">
							(full-time equivalent)
						</#if>
					</#if>
				</td>
			</tr>
			<tr>
				<th>Start date</th>
				<td>
					<#if studentCourseDetails.beginDate??>
						<@fmt.date date=studentCourseDetails.beginDate includeTime=false />
					</#if>
				</td>
			</tr>
			<tr>
				<th>End date</th>
				<td>
					<#if studentCourseDetails.endDate??>
						<@fmt.date date=studentCourseDetails.endDate includeTime=false />

					<#elseif studentCourseDetails.expectedEndDate??>
						<@fmt.date date=studentCourseDetails.expectedEndDate includeTime=false/> (expected)
					</#if>
				</td>
			</tr>
		</tbody>
		<tbody>
			<tr>
				<th>UG/PG</th>
				<td>${(studentCourseDetails.route.degreeType.toString)!}
				</td>
			</tr>
			<tr>
				<th>Attendance</th>
				<td>${(studentCourseDetails.latestStudentCourseYearDetails.modeOfAttendance.fullNameToDisplay)!}
				</td>
			</tr>
			<tr>
				<th>Status</th>
				<td>${(studentCourseDetails.statusString)!}
				</td>
			</tr>
		</tbody>
	</table>
</section>
