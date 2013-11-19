<#escape x as x?html>
<#import "../attendance_variables.ftl" as attendance_variables />
<#import "../attendance_macros.ftl" as attendance_macros />
<#import "*/modal_macros.ftl" as modal />

<script>
	(function ($) {
		$(function() {
			$('.persist-area').fixHeaderFooter();

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
			<h1>Record attendance</h1>
			<h6><span class="muted">for</span> ${command.student.fullName}, ${command.pointSet.route.name}</h6>
		</div>

		<#if command.checkpointMap?keys?size == 0>

			<p><em>There are no monitoring points in this scheme.</em></p>

		<#else>

			<#macro monitoringPointsByTerm term>
				<div class="striped-section">
					<h2 class="section-title">${term}</h2>
					<div class="striped-section-contents">
						<#list command.monitoringPointsByTerm[term] as point>
							<div class="item-info row-fluid point">
								<div class="span12">
									<div class="pull-right">
										<select id="checkpointMap-${point.id}" name="checkpointMap[${point.id}]">
											<#assign hasState = mapGet(command.checkpointMap, point)?? />
											<option value="" <#if !hasState >selected</#if>>Not recorded</option>
											<option value="unauthorised" <#if hasState && mapGet(command.checkpointMap, point).dbValue == "unauthorised">selected</#if>>Missed (unauthorised)</option>
											<option value="authorised" <#if hasState && mapGet(command.checkpointMap, point).dbValue == "authorised">selected</#if>>Missed (authorised)</option>
											<option value="attended" <#if hasState && mapGet(command.checkpointMap, point).dbValue == "attended">selected</#if>>Attended</option>
										</select>
										<#if point.pointType?? && point.pointType.dbValue == "meeting">
											<a class="meetings" title="Meetings with this student" href="<@routes.studentMeetings point command.student />"><i class="icon-info-sign icon-fixed-width"></i></a>
										<#else>
											<i class="icon-fixed-width"></i>
										</#if>
									</div>
									${point.name} (<@fmt.weekRanges point />)
									<@spring.bind path="command.checkpointMap[${point.id}]">
										<#if status.error>
											<div class="text-error"><@f.errors path="command.checkpointMap[${point.id}]" cssClass="error"/></div>
										</#if>
									</@spring.bind>
								</div>
								<script>
									Attendance.createButtonGroup('#checkpointMap-${point.id}');
								</script>
							</div>
						</#list>
					</div>
				</div>
			</#macro>

			<form id="recordAttendance" action="" method="post">
				<input type="hidden" name="returnTo" value="${returnTo}"/>
				<script>
					Attendance.bindButtonGroupHandler();
				</script>
				<#list attendance_variables.monitoringPointTermNames as term>
					<#if command.monitoringPointsByTerm[term]??>
						<@monitoringPointsByTerm term />
					</#if>
				</#list>

				<div class="persist-footer save-row">
					<div class="pull-right">
						<input type="submit" value="Save" class="btn btn-primary">
						<a class="btn" href="${returnTo}">Cancel</a>
					</div>
				</div>
			</form>
		</#if>
	</div>
</div>

<div id="modal" class="modal hide fade" style="display:none;">
	<@modal.header>
		<h3>Meetings</h3>
	</@modal.header>
	<@modal.body></@modal.body>
</div>

</#escape>