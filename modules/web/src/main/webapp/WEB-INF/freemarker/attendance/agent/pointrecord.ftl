<#escape x as x?html>

<#import "*/attendance_macros.ftl" as attendance_macros />
<#import "*/modal_macros.ftl" as modal />

<#assign titleHeader>
	<h1>Record attendance</h1>
	<h6><span class="muted">for</span> ${templatePoint.name}</h6>
</#assign>
<#assign numberOfStudents = command.checkpointMap?keys?size />

<#if numberOfStudents == 0>
	<#noescape>${titleHeader}</#noescape>

	<p class="alert alert-info">
		<i class="icon-info-sign"></i> There are no students registered to these points.
	</p>
<#elseif (numberOfStudents > 600)>
	<#noescape>${titleHeader}</#noescape>

	<div class="alert alert-warning">
		<i class="icon-thumbs-down"></i> There are too many students to display at once. Please go back and choose a more restrictive filter.
	</div>
<#else>

	<#noescape>${titleHeader}</#noescape>

	<#import "*/_profile_link.ftl" as pl />
	<div id="profile-modal" class="modal fade profile-subset"></div>

	<script>
		(function ($) {
			$(function() {
				$('.fix-area').fixHeaderFooter();

				$('.select-all').change(function(e) {
					$('.attendees').selectDeselectCheckboxes(this);
				});

			});
		} (jQuery));
	</script>

	<div class="recordCheckpointForm">

		<@attendance_macros.attendanceButtons />

		<div class="fix-area">
			<div class="fix-header pad-when-fixed">
				<div class="row-fluid check-all">
					<div class="span12">
							<span class="studentsLoadingMessage" style="display: none;">
								<i class="icon-spinner icon-spin"></i><em> Loading&hellip;</em>
							</span>

						<script>
							jQuery('.studentsLoadingMessage').show();
							jQuery(function($){
								$('.studentsLoadingMessage').hide();
							})
						</script>

						<div class="pull-right" style="display: none;">
							<span class="checkAllMessage">
								Record all attendance
								<#assign popoverContent>
									<p><i class="icon-minus icon-fixed-width"></i> Not recorded</p>
									<p><i class="icon-remove icon-fixed-width unauthorised"></i> Missed (unauthorised)</p>
									<p><i class="icon-remove-circle icon-fixed-width authorised"></i> Missed (authorised)</p>
									<p><i class="icon-ok icon-fixed-width attended"></i> Attended</p>
								</#assign>
								<a class="use-popover"
								   data-title="Key"
								   data-placement="bottom"
								   data-container="body"
								   data-content='${popoverContent}'
								   data-html="true">
									<i class="icon-question-sign"></i>
								</a>
							</span>
							<div class="btn-group">
								<button
										type="button"
										class="btn use-tooltip"
										title="Set all to 'Not recorded'"
										data-html="true"
										data-container="body"
										>
									<i class="icon-minus icon-fixed-width"></i>
								</button>
								<button
										type="button"
										class="btn btn-unauthorised use-tooltip"
										title="Set all to 'Missed (unauthorised)'"
										data-html="true"
										data-container="body"
										>
									<i class="icon-remove icon-fixed-width"></i>
								</button>
								<button
										type="button"
										class="btn btn-authorised use-tooltip"
										title="Set all to 'Missed (authorised)'"
										data-html="true"
										data-container="body"
										>
									<i class="icon-remove-circle icon-fixed-width"></i>
								</button>
								<button
										type="button"
										class="btn btn-attended use-tooltip"
										title="Set all to 'Attended'"
										data-html="true"
										data-container="body"
										>
									<i class="icon-ok icon-fixed-width"></i>
								</button>
							</div>
							<#if features.attendanceMonitoringNote>
								<a style="visibility: hidden" class="btn"><i class="icon-edit"></i></a>
							</#if>
							<i class="icon-fixed-width"></i>
						</div>
					</div>
				</div>
			</div>



			<form id="recordAttendance" action="" method="post" class="dirty-check">
				<div class="striped-section-contents attendees">
					<script>
						AttendanceRecording.bindButtonGroupHandler();
					</script>
					<input type="hidden" name="returnTo" value="${returnTo}"/>

					<#list command.checkpointMap?keys?sort_by("lastName") as student>
						<#list mapGet(command.checkpointMap, student)?keys as point>
							<div class="row-fluid item-info">
								<div class="span8">
									<#if numberOfStudents <= 50>
										<@fmt.member_photo student "tinythumbnail" true />
									</#if>
									${student.fullName}&nbsp;<@pl.profile_link student.universityId />
									<#if mapGet(command.hasReportedMap, student)>
										<br/><span class="hint">A checkpoint cannot be set as this student's attendance data has already been submitted for this period.</span>
									</#if>
									<@spring.bind path="command.checkpointMap[${student.universityId}][${point.id}]">
										<#if status.error>
											<div class="text-error"><@f.errors path="command.checkpointMap[${student.universityId}][${point.id}]" cssClass="error"/></div>
										</#if>
									</@spring.bind>
								</div>
								<div class="span4">
									<div class="pull-right">
										<#if mapGet(command.hasReportedMap, student)>
											<@attendance_macros.checkpointLabel
												department=department
												student=student
												checkpoint=(mapGet(mapGet(command.studentPointCheckpointMap, student), point))!""
												note=(mapGet(mapGet(attendanceNoteMap, student), point))!""
												point=point
											/>
										<#else>
											<@attendance_macros.checkpointSelect
												id="checkpointMap-${student.universityId}-${point.id}"
												name="checkpointMap[${student.universityId}][${point.id}]"
												department=department
												checkpoint=(mapGet(mapGet(command.studentPointCheckpointMap, student), point))!""
												note=(mapGet(mapGet(attendanceNoteMap, student), point))!""
												student=student
												point=point
											/>
										</#if>

										<#if mapGet(mapGet(attendanceNoteMap, student), point)??>
											<#assign note = mapGet(mapGet(attendanceNoteMap, student), point) />
											<#if note.hasContent>
												<a id="attendanceNote-${student.universityId}-${point.id}" class="btn use-tooltip attendance-note edit" title="Edit attendance note" href="<@routes.attendance.noteEdit academicYear.startYear?c student point />?dt=${.now?string('iso')}">
													<i class="icon-edit attendance-note-icon"></i>
												</a>
											<#else>
												<a id="attendanceNote-${student.universityId}-${point.id}" class="btn use-tooltip attendance-note" title="Add attendance note" href="<@routes.attendance.noteEdit academicYear.startYear?c student point />">
													<i class="icon-edit attendance-note-icon"></i>
												</a>
											</#if>
										<#else>
											<a id="attendanceNote-${student.universityId}-${point.id}" class="btn use-tooltip attendance-note" title="Add attendance note" href="<@routes.attendance.noteEdit academicYear.startYear?c student point />">
												<i class="icon-edit attendance-note-icon"></i>
											</a>
										</#if>

										<#if point.pointType.dbValue == "meeting">
											<a class="meetings" title="Meetings with this student" href="<@routes.attendance.profileMeetings student academicYear.startYear?c point />"><i class="icon-info-sign icon-fixed-width"></i></a>
										<#elseif point.pointType.dbValue == "smallGroup">
											<a class="small-groups" title="Small group teaching events for this student" href="<@routes.attendance.profileGroups student academicYear.startYear?c point />"><i class="icon-info-sign icon-fixed-width"></i></a>
										<#else>
											<i class="icon-fixed-width"></i>
										</#if>
									</div>
								</div>
								<script>
									AttendanceRecording.createButtonGroup('#checkpointMap-${student.universityId}-${point.id}');
								</script>
							</div>
						</#list>
					</#list>
				</div>

				<div class="fix-footer submit-buttons">
					<div class="pull-right">
						<input type="submit" value="Save" class="btn btn-primary" data-loading-text="Saving&hellip;" autocomplete="off">
						<a class="btn dirty-check-ignore" href="${returnTo}">Cancel</a>
					</div>
				</div>
			</form>
		</div>

	</div>

</#if>

<div id="meetings-modal" class="modal hide fade" style="display:none;">
	<@modal.header>
		<h3>Meetings</h3>
	</@modal.header>
	<@modal.body></@modal.body>
</div>
<div id="small-groups-modal" class="modal hide fade" style="display:none;"></div>

</#escape>