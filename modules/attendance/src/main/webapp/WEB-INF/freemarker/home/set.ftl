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

	<div class="persist-area">
		<div class="persist-header">
			<h1>Record attendance for <#if (monitoringPoint.pointSet.year)??>Year ${monitoringPoint.pointSet.year}</#if> <@fmt.route_name monitoringPoint.pointSet.route /> : ${monitoringPoint.name}</h1>


			<div class="row-fluid record-attendance-form-header">
				<div class="span2 offset10 text-center ">Attended <br /><input type="checkbox" name="select-all" class="select-all"/></div>
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
								<div class="span10">
									<a id="student-${student.universityId}" style="width: 0px; height: 0px; position: relative; top: -200px;"></a>
									<@fmt.member_photo student "tinythumbnail" true />
									<div class="full-height">${student.fullName}</div>
								</div>
								<div class="span2 text-center">
									<div class="full-height">
										<#assign universityId = student.universityId />
										<input type="checkbox" name="studentIds" value="${student.universityId}" <#if command.studentsChecked[universityId]!false>checked="checked"</#if>/>
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