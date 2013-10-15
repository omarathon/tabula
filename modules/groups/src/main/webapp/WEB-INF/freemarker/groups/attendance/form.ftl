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

<div class="persist-area">

	<div class="persist-header">
		<h1>Record attendance for
			${command.event.group.groupSet.name},
			${command.event.group.name}:<br />
			${command.event.day.name} <@fmt.time event.startTime /> - <@fmt.time event.endTime />, Week ${command.week}
		</h1>

		<div class="row-fluid record-attendance-form-header">
			<div class="span1 offset10 text-center ">
				Attended <br />
				<input type="checkbox" name="select-all" class="select-all"/>
			</div>
		</div>
	</div>

	<#if !command.members?has_content>

		<p><em>There are no students allocated to this group.</em></p>

	<#else>

		<div class="striped-section-contents attendees">
			<form action="" method="post">
				<input type="hidden" name="returnTo" value="<#if returnTo?starts_with('http')>${returnTo}<#else><@url page="${returnTo}" /></#if>" />
				<#list command.members as student>
					<#assign checked = false />
					<#list command.attendees as attendedStudent>
						<#if attendedStudent == student.universityId>
							<#assign checked = true />
						</#if>
					</#list>
					<div class="row-fluid item-info clickable">
						<label>
						<div class="span10">
							<@fmt.member_photo student "tinythumbnail" false />
							<div class="full-height">${student.fullName}</div>
						</div>
						<div class="span1">
							<div class="full-height">
								<input type="checkbox" name="attendees" value="${student.universityId}" <#if checked>checked="checked"</#if>/>
							</div>
						</div>
						</label>
					</div>
				</#list>

				<div class="persist-footer">
					<div class="pull-right span2">
						<input type="submit" value="Save" class="btn btn-primary">
						<a class="btn" href="${returnTo}">Cancel</a>
					</div>
				</div>
			</form>
		</div>

	</#if>

</div>