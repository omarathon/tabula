<#import "*/modal_macros.ftl" as modal />
<#import "../attendance_macros.ftl" as attendance_macros />

<#assign titleHeader>
<h1>Record attendance</h1>
<h6><span class="muted">for</span> ${command.templateMonitoringPoint.name}</h6>
</#assign>
<#assign numberOfStudents = command.studentsState?keys?size />

<#if numberOfStudents == 0>
	${titleHeader}

	<p class="alert alert-info">
		<i class="icon-info-sign"></i> There are no students registered to these points.
	</p>
<#elseif (numberOfStudents > 600)>
	${titleHeader}

	<div class="alert alert-warning">
		<i class="icon-thumbs-down"></i> There are too many students to display at once. Please go back and choose a more restrictive filter.
	</div>
<#else>
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

		${titleHeader}

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
								Check all
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
							<i class="icon-fixed-width"></i>
						</div>
					</div>
				</div>
			</div>


			<#macro studentRow student point>
				<div class="row-fluid item-info">
					<div class="span12">
						<div class="pull-right">
							<#local hasState = mapGet(mapGet(command.studentsState, student), point)?? />
							<#if hasState>
								<#local checkpointState = mapGet(mapGet(command.studentsState, student), point) />
							</#if>
							<select
								id="studentsState-${student.universityId}-${point.id}"
								name="studentsState[${student.universityId}][${point.id}]"
								title="${mapGet(mapGet(command.checkpointDescriptions, student), point)?default("")}"
							>
								<option value="" <#if !hasState >selected</#if>>Not recorded</option>
								<#list allCheckpointStates as state>
									<option value="${state.dbValue}" <#if hasState && checkpointState.dbValue == state.dbValue>selected</#if>>${state.description}</option>
								</#list>
							</select>
							<#if point.pointType?? && point.pointType.dbValue == "meeting">
								<a class="meetings" title="Meetings with this student" href="<@routes.studentMeetings point student />"><i class="icon-fixed-width icon-info-sign"></i></a>
							<#else>
								<i class="icon-fixed-width"></i>
							</#if>
						</div>
						<#if numberOfStudents <= 50>
							<@fmt.member_photo student "tinythumbnail" true />
						</#if>
						${student.fullName}
						<@spring.bind path="command.studentsState[${student.universityId}][${point.id}]">
							<#if status.error>
								<div class="text-error"><@f.errors path="command.studentsState[${student.universityId}][${point.id}]" cssClass="error"/></div>
							</#if>
						</@spring.bind>
					</div>
					<script>
						AttendanceRecording.createButtonGroup('#studentsState-${student.universityId}-${point.id}');
					</script>
				</div>
			</#macro>

			<form id="recordAttendance" action="" method="post">
				<div class="striped-section-contents attendees">
					<script>
						AttendanceRecording.bindButtonGroupHandler();
					</script>
					<input type="hidden" name="monitoringPoint" value="${command.templateMonitoringPoint.id}" />
					<input type="hidden" name="returnTo" value="${returnTo}"/>
					<#list command.studentsState?keys?sort_by("lastName") as student>
						<@studentRow student mapGet(command.studentsState, student)?keys?first />
					</#list>
				</div>

				<div class="fix-footer submit-buttons">
					<div class="pull-right">
						<input type="submit" value="Save" class="btn btn-primary" data-loading-text="Saving&hellip;" autocomplete="off">
						<a class="btn" href="${returnTo}">Cancel</a>
					</div>
				</div>
			</form>
		</div>
	</div>

	<div id="modal" class="modal hide fade" style="display:none;">
		<@modal.header>
			<h3>Meetings</h3>
		</@modal.header>
		<@modal.body></@modal.body>
	</div>
</#if>

