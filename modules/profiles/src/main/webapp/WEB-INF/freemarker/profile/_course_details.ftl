<section id="course-details" class="clearfix">
	<hr class="full-width"></hr>
	<h3 class="course-title">
		Course: ${(studentCourseDetails.course.name)!}, ${(studentCourseDetails.course.code)!}
		<@fmt.course_year_span studentCourseDetails />

	</h3>

	<#if (profile.freshStudentCourseDetails)?? && (profile.freshStudentCourseDetails?size > 1)>
	<div class="dropdown">
		<a id="course-dropdown" class="dropdown-toggle pull-right" data-toggle="dropdown" role="button" href="#">
			Other courses
			<b class="caret"></b>
		</a>
		<ul class="dropdown-menu pull-right" aria-labelledby="course-dropdown" role="menu">
			<#list profile.freshStudentCourseDetails as scd>
				<#if scd.scjCode != studentCourseDetails.scjCode>
					<li role="presentation">
						<a href="/profiles/view/course/${scd.urlSafeId}" role="menuitem">
							${(scd.course.code)!} <@fmt.course_year_span studentCourseDetails /> ${scd.scjCode}
						</a>
					</li>
				</#if>
			</#list>
		</ul>
	</div>
	</#if>

	<div class="data clearfix">
		<div class="col1">
			<table class="profile-or-course-info upper">
				<tbody>
					<#if (studentCourseDetails.route)??>
						<tr>
							<th>Route</th>
							<td>${(studentCourseDetails.route.name)!} (${(studentCourseDetails.route.code?upper_case)!})
							</td>
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
			</table>
			<br />
			<table class="profile-or-course-info">
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
						<td>${(studentCourseDetails.latestStudentCourseYearDetails.modeOfAttendance.fullNameToDisplay?cap_first)!}
						</td>
					</tr>
					<tr>
						<th>Course join code (SITS)</th>
						<td>${studentCourseDetails.scjCode}
						</td>
					</tr>
				</tbody>
			</table>
		</div>

		<div class="col2">
			<table class="profile-or-course-info upper">
				<tbody>
					<#if !isSelf>
						<tr>
							<th>Status</th>
							<td><@fmt.spr_status studentCourseDetails />
							</td>
						</tr>
						<tr>
							<th>Enrolment status</th>
							<td><@fmt.enrolment_status studentCourseDetails />
							</td>
						</tr>
					</#if>
				</tbody>
			</table>
			<br />
			<table class="profile-or-course-info">
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
			</table>
		</div>
	</div>
</section>
