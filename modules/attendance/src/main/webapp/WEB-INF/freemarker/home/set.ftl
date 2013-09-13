<script>

(function ($) {
	$(function() {
		$('.persist-area').fixHeaderFooter();

		// toggleCheckboxes
		// toggles the checkboxes, and returns the number selected
		var toggleCheckboxes = function(areaClass) {
			var allCheckboxes = $(areaClass + ' [type=checkbox]');
			var checked = allCheckboxes.not(":checked").length;
			if(checked == 0) {
				setAllCheckboxes(false);
				return 0;
			} else {
				setAllCheckboxes(true);
				return allCheckboxes.length;
			}

			function setAllCheckboxes(setting) {
				allCheckboxes.each(function() { this.checked = setting });
			}
		}

		$('.select-all').click(function(e) {
			e.preventDefault();
			var link = this;
			if(toggleCheckboxes(".attendees") > 0) {
				$(link).text("(unselect all)");
			} else {
				$(link).text("(select all)");
			}
		});

	});
} (jQuery));

</script>



<div class="recordCheckpointForm">

	<div class="persist-area">
		<div class="persist-header">
			<h1>Record attendance for <#if (monitoringPoint.pointSet.year)??>Year ${monitoringPoint.pointSet.year}</#if> ${monitoringPoint.pointSet.route.code?upper_case} ${monitoringPoint.pointSet.route.name} : ${monitoringPoint.name}</h1>


			<div class="row-fluid">
				<div class="span10"></div>
				<div class="span2 text-center">Attended <br /> <a href="#" class="select-all">(select all)</button></div>
			</div>
		</div>

		<div class="striped-section-contents attendees">

			<form action="" method="post">
				<input type="hidden" name="monitoringPoint" value="${monitoringPoint.id}" />
				<input type="hidden" value="<@url page="${returnTo}" />" />
				<#list command.members?sort_by("lastName") as student>


					<div class="row-fluid item-info clickable">
						<label>
							<div class="span10">

								<@fmt.member_photo student "tinythumbnail" true />
								<div class="full-height">${student.fullName}</div>
							</div>
							<div class="span2 text-center">
								<div class="full-height">
									<#assign universityId = student.universityId />
									<input type="checkbox" name="studentIds" class="collection-checkbox" value="${student.universityId}" <#if command.studentsChecked[universityId]!false>checked="checked"</#if>/>
								</div>
							</div>
						</label>
					</div>
				</#list>


				<div class="persist-footer save-row">
					<div class="pull-right">
						<input type="submit" value="Save" class="btn btn-primary">
						<a class="btn" href="<@url page="${returnTo}" context="/attendance" />">Cancel</a>
					</div>
				</div>
			</form>
		</div>
	</div>
</div>