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
			<div class="row-fluid record-attendance-form-header">
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
						<i class="icon-fixed-width"></i>
					</div>
				</div>
			</div>
		</div>

		<#if !command.members?has_content>

			<p><em>There are no students allocated to this group.</em></p>

		<#else>

			<#macro studentRow student>
				<div class="row-fluid item-info">
					<div class="span12">
						<div class="pull-right">
							<#local hasState = mapGet(command.studentsState, student.universityId)?? />
							<#if hasState>
								<#local currentState = mapGet(command.studentsState, student.universityId) />
							</#if>
							<select id="studentsState-${student.universityId}" name="studentsState[${student.universityId}]">
								<option value="" <#if !hasState>selected</#if>>Not recorded</option>
								<#list allCheckpointStates as state>
									<option value="${state.dbValue}" <#if hasState && currentState.dbValue == state.dbValue>selected</#if>>${state.description}</option>
								</#list>
							</select>
							<i class="icon-fixed-width"></i>
						</div>

						<@fmt.member_photo student "tinythumbnail" true />
						${student.fullName}

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
				<form id="recordAttendance" action="" method="post">
					<script>
						AttendanceRecording.bindButtonGroupHandler();
					</script>

					<#list command.members as student>
						<@studentRow student />
					</#list>

					<div class="fix-footer save-row">
						<div class="pull-right">
							<input type="submit" value="Save" class="btn btn-primary" data-loading-text="Saving&hellip;" autocomplete="off">
							<a class="btn" href="${returnTo}">Cancel</a>
						</div>
					</div>
				</form>
			</div>

		</#if>

	</div>
</div>