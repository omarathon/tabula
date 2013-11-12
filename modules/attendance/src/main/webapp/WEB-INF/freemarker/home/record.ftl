<#import "*/modal_macros.ftl" as modal />

<#assign titleHeader>
<h1>Record attendance</h1>
<h6><span class="muted">for</span> ${command.templateMonitoringPoint.name}</h6>
</#assign>
<#assign numberOfStudents = command.studentsState?keys?size />

<#if numberOfStudents == 0>

${titleHeader}

<p><em>There are no students registered to these points.</em></p>

<#elseif (numberOfStudents > 600)>

${titleHeader}

<div class="alert alert-warning">
	There are too many students to display at once. Please go back and choose a more restrictive filter.
</div>

<#else>

<script>
(function ($) {
	$(function() {
		$('.persist-area').fixHeaderFooter();

		$('.select-all').change(function(e) {
			$('.attendees').selectDeselectCheckboxes(this);
		});

	});
} (jQuery));
var createButtonGroup = function(id){
	var $this = jQuery(id), selectedValue = $this.find('option:selected').val();
	jQuery('.recordCheckpointForm div.forCloning div.btn-group')
			.clone(true)
			.insertAfter($this)
			.find('button').filter(function(){
				return jQuery(this).data('state') == selectedValue;
			}).addClass('active');
	$this.hide();
};
</script>

<div class="recordCheckpointForm">

	<div style="display:none;" class="forCloning">
		<div class="btn-group" data-toggle="buttons-radio">
			<button type="button" class="btn" data-state="">
				<i class="icon-minus icon-fixed-width" title="Set to 'Not recorded'"></i>
			</button>
			<button type="button" class="btn btn-unauthorised" data-state="unauthorised">
				<i class="icon-remove icon-fixed-width" title="Set to 'Missed (unauthorised)'"></i>
			</button>
			<button type="button" class="btn btn-authorised" data-state="authorised">
				<i class="icon-remove-circle icon-fixed-width" title="Set to 'Missed (authorised)'"></i>
			</button>
			<button type="button" class="btn btn-attended" data-state="attended">
				<i class="icon-ok icon-fixed-width" title="Set to 'Attended'"></i>
			</button>
		</div>
	</div>

	<div class="persist-area">
		<div class="persist-header">
			${titleHeader}

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
							<a class="use-popover" id="popover-choose-set"
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
							<button type="button" class="btn btn-unauthorised">
								<i class="icon-remove icon-fixed-width" title="Set all to 'Missed (unauthorised)'"></i>
							</button>
							<button type="button" class="btn btn-authorised">
								<i class="icon-remove-circle icon-fixed-width" title="Set all to 'Missed (authorised)'"></i>
							</button>
							<button type="button" class="btn btn-attended">
								<i class="icon-ok icon-fixed-width" title="Set all to 'Attended'"></i>
							</button>
						</div>
						<i class="icon-fixed-width"></i>
					</div>
				</div>

			</div>
		</div>


		<#macro studentRow student point hasMultiple>
			<div class="row-fluid item-info">
				<div class="span12">
					<div class="pull-right">
						<#local hasState = mapGet(mapGet(command.studentsState, student), point)?? />
						<#if hasState>
							<#local checkpointState = mapGet(mapGet(command.studentsState, student), point) />
						</#if>
						<select id="studentsState-${student.universityId}-${point.id}" name="studentsState[${student.universityId}][${point.id}]">
							<option value="" <#if !hasState >selected</#if>>Not recorded</option>
							<#list allCheckpointStates as state>
								<option value="${state.dbValue}" <#if hasState && checkpointState.dbValue == state.dbValue>selected</#if>>${state.description}</option>
							</#list>
						</select>
						<#if point.pointType?? && point.pointType.dbValue == "meeting">
							<a class="meetings" title="Meetings information" href="<@routes.studentMeetings point student />"><i class="icon-info-sign"></i></a>
						<#else>
							<i class="icon-fixed-width"></i>
						</#if>
					</div>
					<#if numberOfStudents <= 50>
						<@fmt.member_photo student "tinythumbnail" true />
					</#if>
					${student.fullName}
					<#if hasMultiple><span class="muted">${point.pointSet.route.code?upper_case}</span></#if>
					<@spring.bind path="command.studentsState[${student.universityId}][${point.id}]">
						<#if status.error>
							<div class="text-error"><@f.errors path="command.studentsState[${student.universityId}][${point.id}]" cssClass="error"/></div>
						</#if>
					</@spring.bind>
				</div>
			</div>
		<script>
			createButtonGroup('#studentsState-${student.universityId}-${point.id}');
		</script>
		</#macro>

		<div class="striped-section-contents attendees">

			<form id="recordAttendance" action="" method="post">
				<script>
					jQuery('#recordAttendance').on('click', 'div.btn-group button', function(){
						var $this = jQuery(this);
						$this.closest('div.pull-right').find('option').filter(function(){
							return jQuery(this).val() == $this.data('state');
						}).prop('selected', true);
					});
				</script>
				<input type="hidden" name="monitoringPoint" value="${command.templateMonitoringPoint.id}" />
				<input type="hidden" name="returnTo" value="${returnTo}"/>
				<#list command.studentsState?keys?sort_by("lastName") as student>
					<#assign points = mapGet(command.studentsState, student) />
					<#if (points?keys?size > 1)>
						<#list points?keys as point><@studentRow student point true/></#list>
					<#else>
						<@studentRow student points?keys?first false/>
					</#if>
				</#list>


				<div class="persist-footer save-row">
					<div class="pull-right">
						<input type="submit" value="Save" class="btn btn-primary">
						<a class="btn" href="${returnTo}">Cancel</a>
					</div>
				</div>
			</form>
		</div>


	</div>
</div>

<div id="modal" class="modal hide fade" style="display:none;">
	<@modal.header>
		<h3>Meetings</h3>
	</@modal.header>
	<@modal.body></@modal.body>
</div>

</#if>

