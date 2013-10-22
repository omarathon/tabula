<section id="course-details" class="clearfix">
	<h4>Course details</h4>


	<div class="data clearfix">
		<div class="col1">
			<table class="course-info">
				<tbody>
					<#if (studentCourseDetails.route)??>
						<tr>
							<th>Route</th>
							<td>${(studentCourseDetails.route.name)!} (${(studentCourseDetails.route.code?upper_case)!})
							</td>
						</tr>
					</#if>
					<#if profile.studentCourseDetails?size lt 2 && (studentCourseDetails.route)?? && (studentCourseDetails.course)??>
						<tr>
							<th>Course</th>
							<td>${(studentCourseDetails.course.name)!}</td>
						</tr>
					</#if>
					<#if (studentCourseDetails.department)??>
						<tr>
							<th>Department</th>
							<td>${(studentCourseDetails.department.name)!} (${((studentCourseDetails.department.code)!)?upper_case})
							</td>
						</tr>
					</#if>
					<tr>
						<th>Intended award</th>
							<td>${(studentCourseDetails.awardCode)!}</td>
					</tr>
				</tbody>
				<tbody>

				</tbody>
				<tbody>
					<#if (studentCourseDetails.route)??>
						<tr>
							<th>UG/PG</th>
							<td>${(studentCourseDetails.route.degreeType.toString)!}
							</td>
						</tr>
					</#if>
					<tr>
						<th>Attendance</th>
						<td>${(studentCourseDetails.latestStudentCourseYearDetails.modeOfAttendance.fullNameToDisplay)!}
						</td>
					</tr>
					<#if !isSelf>
						<tr>
							<th>Status</th>
							<td>${(studentCourseDetails.statusString)!}
							</td>
						</tr>
					</#if>
				</tbody>
			</table>
		</div>

		<div class="col2">
			<table class="course-info">
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

				</tbody>
				<tbody>
					<tr>
						<th>Student course join (SCJ) code, from SITS</th>
							<td>${(studentCourseDetails.scjCode)!}
							</td>
					</tr>
				</tbody>
			</table>
		</div>

	</div>

</section>
