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
			<button type="button" class="btn" data-state="unauthorised">
				<i class="icon-remove icon-fixed-width unauthorised" title="Set to 'Missed (unauthorised)'"></i>
			</button>
			<button type="button" class="btn" data-state="authorised">
				<i class="icon-remove icon-fixed-width authorised" title="Set to 'Missed (authorised)'"></i>
			</button>
			<button type="button" class="btn" data-state="attended">
				<i class="icon-ok icon-fixed-width attended" title="Set to 'Attended'"></i>
			</button>
		</div>
	</div>

	<div class="persist-area">
		<div class="persist-header">
			<h1>Record attendance for <#if (monitoringPoint.pointSet.year)??>Year ${monitoringPoint.pointSet.year}</#if> <@fmt.route_name monitoringPoint.pointSet.route /> : ${monitoringPoint.name}</h1>


			<div class="row-fluid record-attendance-form-header">
				<div class="span9">
					<span><i class="icon-minus icon-fixed-width"></i> Not recorded</span>
					<span><i class="icon-remove icon-fixed-width unauthorised"></i> Missed (unauthorised)</span>
					<span><i class="icon-remove icon-fixed-width authorised"></i> Missed (authorised)</span>
					<span><i class="icon-ok icon-fixed-width attended"></i> Attended</span>
				</div>
				<div class="span3 text-center">
					<div class="btn-group">
						<button type="button" class="btn">
							<i class="icon-minus icon-fixed-width" title="Set all to 'Not recorded'"></i>
						</button>
						<button type="button" class="btn">
							<i class="icon-remove icon-fixed-width unauthorised" title="Set all to 'Missed (unauthorised)'"></i>
						</button>
                        <button type="button" class="btn">
							<i class="icon-remove icon-fixed-width authorised" title="Set all to 'Missed (authorised)'"></i>
						</button>
						<button type="button" class="btn">
							<i class="icon-ok icon-fixed-width attended" title="Set all to 'Attended'"></i>
						</button>
					</div>
				</div>
			</div>
		</div>

		<#if command.members?size == 0>

			<p><em>There are no students registered to this course for this year of study.</em></p>

		<#else>

			<div class="striped-section-contents attendees">

				<form action="" method="post">
					<input type="hidden" name="monitoringPoint" value="${monitoringPoint.id}" />
					<input type="hidden" value="<@url page="${returnTo}" />" />
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