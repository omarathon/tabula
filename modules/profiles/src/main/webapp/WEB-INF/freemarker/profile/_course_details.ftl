<section id="course-details">
	<h4>Course details</h4>
	<table class="course-info">
		<tbody>
			<tr>
				<th>Route</th>
				<td>
					<#if studentCourseDetails.route??>
						${studentCourseDetails.route.name} (${studentCourseDetails.route.code?upper_case})
					</#if>
				</td>
			</tr>
			<tr>
				<th>Course code</th>
				<td>
					<#if studentCourseDetails.sitsCourseCode??>
						${studentCourseDetails.sitsCourseCode?upper_case}
					</#if>
				</td>
			</tr>
			<tr>
				<th>Department</th>
				<td>
					<#if studentCourseDetails.studyDepartment??>
						${studentCourseDetails.department.name} (${studentCourseDetails.department.code?upper_case})
					<#else>
						<#if (studentCourseDetails.route.department.name)??>
							${studentCourseDetails.route.department.name}
						</#if>
					</#if>
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
				<td>
					<#if (studentCourseDetails.route.degreeType)??>
						${studentCourseDetails.route.degreeType.toString}
					</#if>
				</td>
			</tr>
			<tr>
				<th>Attendance</th>
				<td>
					<#if studentCourseDetails.modeOfAttendance??>
						${studentCourseDetails.latestStudentCourseYearDetails.modeOfAttendance.fullNameToDisplay}
					</#if>
				</td>
			</tr>
			<tr>
				<th>Status</th>
				<td>
					<#if studentCourseDetails.statusString??>
						${studentCourseDetails.statusString}
					</#if>
				</td>
			</tr>
		</tbody>
	</table>
</section>
