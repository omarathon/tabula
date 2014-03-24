<section id="course-details" class="clearfix">
	<hr class="full-width"></hr>

	<!-- course heading -->
	<h3 class="course-title">
		Course: ${(studentCourseDetails.course.name)!}, ${(studentCourseDetails.course.code)!}
	<@fmt.course_year_span studentCourseDetails />

	</h3>

	<!-- drop-down to choose other courses: -->
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
						<a href="/profiles/view/course/${scd.urlSafeId}" role="menuitem"
							<#if scd.scjCode == profile.mostSignificantCourse.scjCode>
						   id="most-significant"
							</#if>
								>
						<#-- when choosing from the "Other courses" drop-down, make the most signicant course
						  -- bold to assist with navigating back if you have previously gone into a different course -->
								${(scd.course.code)!}
								<@fmt.course_year_span scd /> ${scd.scjCode}
						</a>
					</li>
				</#if>
			</#list>
		</ul>
	</div>
</#if>


	<!-- basic course details across years -->
	<div class="data clearfix col1">
		<div class="col1 basic-course-details">
			<table class="profile-or-course-info">
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
				<#if !isSelf>
				<tr>
					<th>Status on Route</th>
					<td><@fmt.status_on_route studentCourseDetails />
					</td>
				</tr>
				</#if>
				<tr>
					<th>UG/PG</th>
					<td>${(studentCourseDetails.route.degreeType.toString)!}
					</td>
				</tr>
				</tbody>
			</table>
		</div>
	</div>

	<!-- expandable course details -->

	<div class="col2">
		<details>
			<summary class="collapsible large-chevron">
				<span>More details</span>
			</summary>
			<table class="profile-or-course-info">
				<tbody>
				<tr>
					<th>Intended award</th>
					<td>${(studentCourseDetails.award.name)!}</td>
				</tr>
				<tr>
					<th>Length of course</th>
					<td>
					<#if studentCourseDetails.courseYearLength??>
					${studentCourseDetails.courseYearLength} years
					</#if>
					<#if studentCourseDetails.modeOfAttendance??>
						<#if studentCourseYearDetails.modeOfAttendance.code != "F">
							(full-time equivalent)
						</#if>
					</#if>
					</td>
				</tr>
				<#if !isSelf>
				<tr>
					<th>Status on Course</th>
					<td><@fmt.status_on_course studentCourseDetails />
					</td>
				</tr>
				</#if>
				<tr>
					<th>Start date</th>
					<td>
					<#if studentCourseDetails.beginDate??>
										<@fmt.date date=studentCourseDetails.beginDate includeTime=false />
									</#if>
					</td>
				</tr>
				<tr>
				<#if studentCourseDetails.endDate??>
					<th>End date</th>
					<td><@fmt.date date=studentCourseDetails.endDate includeTime=false /></td>
				<#elseif studentCourseDetails.expectedEndDate?? >
					<th>Expected end date</th>
					<td><@fmt.date date=studentCourseDetails.expectedEndDate includeTime=false/></td>
				</#if>
				</tr>
				<tr>
					<th>Programme route code</th>
					<td>${studentCourseDetails.sprCode}
					</td>
				</tr>
				<tr>
					<th>Course join code</th>
					<td>${studentCourseDetails.scjCode}
					</td>
				</tr>
				</tbody>
			</table>
		</details>
	</div>
</section>
