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
	<div style="display:none;" class="forCloning">
		<div class="btn-group" data-toggle="buttons-radio">
			<button type="button" class="btn" data-state="">
				<i class="icon-minus icon-fixed-width" title="Set to 'Not recorded'"></i>
			</button>
			<button type="button" class="btn btn-unauthorised<#if eventInFuture> disabled use-tooltip" data-container="body" title="You can only record authorised absence for future events</#if>" data-state="unauthorised">
				<i class="icon-remove icon-fixed-width" title="Set to 'Missed (unauthorised)'"></i>
			</button>
			<button type="button" class="btn btn-authorised" data-state="authorised">
				<i class="icon-remove-circle icon-fixed-width" title="Set to 'Missed (authorised)'"></i>
			</button>
			<button type="button" class="btn btn-attended<#if eventInFuture> disabled use-tooltip" data-container="body" title="You can only record authorised absence for future events</#if>" data-state="attended">
				<i class="icon-ok icon-fixed-width" title="Set to 'Attended'"></i>
			</button>
		</div>
	</div>

	<div class="fix-area">
		<h1>Record attendance</h1>
		<h4><span class="muted">for</span>
			${command.event.group.groupSet.name},
			${command.event.group.name}</h4>
		<h6>
			${command.event.day.name} <@fmt.time event.startTime /> - <@fmt.time event.endTime />, Week ${command.week}
		</h6>

		<div class="fix-header">
			<div class="row-fluid record-attendance-form-header check-all">
				<div class="span12">
					<span class="studentsLoadingMessage">
						<i class="icon-spinner icon-spin"></i><em> Loading&hellip;</em>
					</span>
					<script type="text/javascript">
						jQuery(function($){
							$('.studentsLoadingMessage').hide();
						});
					</script>
					<div class="pull-left">
						<div class="form-inline">
							<label for="additionalStudentQuery">Add student:</label>
							<span class="profile-search input-append" data-target="<@routes.students_json command.event.group.groupSet />?excludeEvent=${command.event.id}&excludeWeek=${command.week}">
								<input type="hidden" name="additionalStudent" />
								<input type="text" name="query" value="" id="additionalStudentQuery" placeholder="Search for students&hellip;" />
								<button class="btn" type="button" style="margin-top: 0px;">
									<i class="icon-search"></i>
								</button>
							</span>

							<#assign helpText>
								<p>If a student not normally in this group has attended or will be attending this session, you can search for them here.</p>
							</#assign>
							<a href="#"
							   class="use-introductory<#if showIntro("sgt-add-adhoc-student", "anywhere")></#if>"
							   data-title="Adding a student not normally present to an event occurrence"
							   data-trigger="click"
							   data-placement="bottom"
							   data-html="true"
							   data-hash="${introHash("sgt-add-adhoc-student", "anywhere")}"
							   data-content="${helpText}"><i class="icon-question-sign icon-fixed-width"></i></a>
						</div>
					</div>
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
							<button type="button" class="btn">
								<i class="icon-minus icon-fixed-width" title="Set all to 'Not recorded'"></i>
							</button>
							<button type="button" class="btn btn-unauthorised<#if eventInFuture> disabled use-tooltip" data-container="body" title="You can only record authorised absence for future events</#if>">
								<i class="icon-remove icon-fixed-width" title="Set all to 'Missed (unauthorised)'"></i>
							</button>
							<button type="button" class="btn btn-authorised">
								<i class="icon-remove-circle icon-fixed-width" title="Set all to 'Missed (authorised)'"></i>
							</button>
							<button type="button" class="btn btn-attended<#if eventInFuture> disabled use-tooltip" data-container="body" title="You can only record authorised absence for future events</#if>">
								<i class="icon-ok icon-fixed-width" title="Set all to 'Attended'"></i>
							</button>
						</div>
						<#if features.attendanceMonitoringNote>
							<a style="visibility: hidden" class="btn"><i class="icon-edit"></i></a>
						</#if>
					</div>
				</div>
			</div>
		</div>

		<#if !command.members?has_content>

			<p><em>There are no students allocated to this group.</em></p>

		<#else>

			<#macro studentRow student added_manually>
				<div class="row-fluid item-info">
					<div class="span12">
						<div class="pull-right">
							<#local hasState = mapGet(command.studentsState, student.universityId)?? />
							<#if hasState>
								<#local currentState = mapGet(command.studentsState, student.universityId) />
							</#if>
							<select id="studentsState-${student.universityId}" name="studentsState[${student.universityId}]" data-universityid="${student.universityId}">
								<option value="" <#if !hasState>selected</#if>>Not recorded</option>
								<#list allCheckpointStates as state>
									<option value="${state.dbValue}" <#if hasState && currentState.dbValue == state.dbValue>selected</#if>>${state.description}</option>
								</#list>
							</select>
							<#if features.attendanceMonitoringNote>
								<#local hasNote = mapGet(mapGet(command.attendanceNotes, student), command.occurrence)?? />
								<#if hasNote>
									<#local note = mapGet(mapGet(command.attendanceNotes, student), command.occurrence) />
									<#if note.note?has_content || note.attachment?has_content>
										<a
											id="attendanceNote-${student.universityId}-${command.occurrence.id}"
											class="btn use-tooltip attendance-note"
											title="Edit attendance note"
											data-container="body"
											href="<@routes.editNote student=student occurrence=command.occurrence returnTo=((info.requestedUri!"")?url) />"
										>
											<i class="icon-edit-sign attendance-note-icon"></i>
										</a>
									<#else>
										<a
											id="attendanceNote-${student.universityId}-${command.occurrence.id}"
											class="btn use-tooltip attendance-note"
											title="Add attendance note"
											data-container="body"
											href="<@routes.editNote student=student occurrence=command.occurrence returnTo=((info.requestedUri!"")?url) />"
										>
											<i class="icon-edit attendance-note-icon"></i>
										</a>
									</#if>
								<#else>
									<a
										id="attendanceNote-${student.universityId}-${command.occurrence.id}"
										class="btn use-tooltip attendance-note"
										title="Add attendance note"
										data-container="body"
										href="<@routes.editNote student=student occurrence=command.occurrence returnTo=((info.requestedUri!"")?url) />"
									>
										<i class="icon-edit attendance-note-icon"></i>
									</a>
								</#if>
							</#if>
						</div>

						<@fmt.member_photo student "tinythumbnail" true />
						${student.fullName}

						<#if added_manually>
							<i class="icon-hand-up use-tooltip" title="Added manually to this event occurrence" data-container="body"></i>
							<button class="btn btn-mini btn-danger" type="button">Remove from this occurrence</button>
						</#if>

						<@spring.bind path="command.studentsState[${student.universityId}]">
							<#list status.errorMessages as err>${err}</#list>
							<div class="text-error"><@f.errors path="studentsState[${student.universityId}]" cssClass="error"/></div>
						</@spring.bind>
					</div>
					<script>
						AttendanceRecording.createButtonGroup('#studentsState-${student.universityId}');
					</script>
				</div>
			</#macro>

			<div class="striped-section-contents attendees">
				<form id="recordAttendance" action="" method="post" data-occurrence="${command.occurrence.id}">
					<script type="text/javascript">
						AttendanceRecording.bindButtonGroupHandler(true);
					</script>

					<#if command.additionalStudent??>
						<@studentRow command.additionalStudent true />
					</#if>

					<#list command.members as student>
						<@studentRow student command.manuallyAddedUniversityIds?seq_contains(student.universityId) />
					</#list>

					<div class="fix-footer submit-buttons">
						<div class="pull-right">
							<input type="submit" value="Save" class="btn btn-primary" data-loading-text="Saving&hellip;" autocomplete="off">
							<a class="btn" href="${returnTo}">Cancel</a>
						</div>
						<div class="pull-left checkpoints-message alert alert-info" style="display: none;">
							Saving this attendance will set monitoring points for
							<span class="students use-popover" data-placement="top" data-container="body" data-content=" " data-html="true"></span>
							as attended.
						</div>
					</div>
				</form>
			</div>

		</#if>

	</div>
</div>