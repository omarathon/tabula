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
			<h1><span class="yearAndRoute">Record attendance for <#if (monitoringPoint.pointSet.year)??>Year ${monitoringPoint.pointSet.year}</#if> <@fmt.route_name monitoringPoint.pointSet.route /> : </span>${monitoringPoint.name}</h1>


			<div class="row-fluid record-attendance-form-header">
				<div class="span8">&nbsp;</div>
				<div class="span4 text-center">
					<span class="checkAllMessage">Check all</span>
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
					<#if monitoringPoint.pointType?? && monitoringPoint.pointType.dbValue == "meeting">
						<i class="icon-fixed-width"></i>
					</#if>

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

				</div>
			</div>
		</div>

		<#if command.members?size == 0>

			<p><em>There are no students registered to this course for this year of study.</em></p>

		<#else>

			<div class="striped-section-contents attendees">

				<form action="" method="post">
					<input type="hidden" name="monitoringPoint" value="${monitoringPoint.id}" />
					<input type="hidden" name="returnTo" value="<@url page="${returnTo}" />" />
					<#list command.members?sort_by("lastName") as student>
						<div class="row-fluid item-info clickable">
							<label>
								<div class="span9">
									<a id="student-${student.universityId}" style="width: 0px; height: 0px; position: relative; top: -200px;"></a>
									<@fmt.member_photo student "tinythumbnail" true />
									<div class="full-height">${student.fullName}</div>
								</div>
								<div class="span3 text-center">
									<div class="full-height">
										<select name="studentsState[${student.universityId}]">
											<#assign hasState = command.studentsState[student.universityId]?? />
											<option value="" <#if !hasState >selected</#if>>Not recorded</option>
											<option value="unauthorised" <#if hasState && command.studentsState[student.universityId].dbValue == "unauthorised">selected</#if>>Missed (unauthorised)</option>
											<option value="authorised" <#if hasState && command.studentsState[student.universityId].dbValue == "authorised">selected</#if>>Missed (authorised)</option>
											<option value="attended" <#if hasState && command.studentsState[student.universityId].dbValue == "attended">selected</#if>>Attended</option>
										</select>
										<#if monitoringPoint.pointType?? && monitoringPoint.pointType.dbValue == "meeting">
											<a class="meetings" title="Meetings information" href="<@routes.studentMeetings monitoringPoint student />"><i class="icon-info-sign"></i></a>
										</#if>
									</div>
								</div>
							</label>
						</div>
					</#list>


					<div class="persist-footer save-row">
						<div class="pull-right">
							<input type="submit" value="Save" class="btn btn-primary">
							<a class="btn" href="${returnTo}">Cancel</a>
						</div>
					</div>
				</form>
			</div>

		</#if>
	</div>
</div>

<div id="modal" class="modal hide fade" style="display:none;">
	<@modal.header>
		<h3>Meetings</h3>
	</@modal.header>
	<@modal.body></@modal.body>
</div>

