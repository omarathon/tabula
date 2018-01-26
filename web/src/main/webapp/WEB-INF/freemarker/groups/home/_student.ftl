<#import "*/group_components.ftl" as components />
<#import "/WEB-INF/freemarker/modal_macros.ftl" as modal />

<#escape x as x?html>
<#if nonempty(memberGroupsetModules.moduleItems) || user.student>
	<div class="header-with-tooltip" id="my-groups">
		<h2 class="section">My groups in ${academicYear.toString}</h2>
		<span class="use-tooltip" data-toggle="tooltip" data-html="true" data-placement="bottom" data-title="Talk to your module convenor if you think a seminar, lab or tutorial is missing - maybe it isn't set up yet, or they aren't using Tabula. Please make sure that you select an assessment component when you register for modules in eMR.">Missing a group?</span>
	</div>

	<#if nonempty(memberGroupsetModules.moduleItems) >
		<div id="student-groups-view">
			<@components.module_info memberGroupsetModules />
		</div><!--student-groups-view-->
		<div class="modal fade timetable-clash-info" id="timetable-clash-modal">
			<@f.form method="post" commandName="command">
				<input type="hidden" name="group" value=""/>
				<@modal.wrapper>
					<@modal.header>
						<h3 class="modal-title">Timetable conflict</h3>
					</@modal.header>
					<@modal.body>
						<p>This group conflicts with another event on your timetable. Do you wish to still sign up for this group?</p>
					</@modal.body>
					<@modal.footer>
						<div class="pull-left">
							<input class="btn btn-primary spinnable spinner-auto" type="submit" value="Confirm sign up" data-loading-text="Loading&hellip;">
							<button type="button" class="btn btn-default" name="cancel" data-dismiss="modal">Cancel sign up</button>
						</div>
					</@modal.footer>
				</@modal.wrapper>
			</@f.form>
		</div>
	<#else>
		<div class="alert alert-block alert-info">
			There are no groups to show you right now for ${academicYear.toString}
		</div>
	</#if>
</#if>
<script type="text/javascript">
	jQuery(function($) {
		$('[id^=select-signup]').submit( function(e) {
			e.preventDefault();
			var form = this;
			var $form = $(this);
			var selectedFormGrpId = $form.find("input[name='group']:checked").val();
			var signupPostFormLink = $form.prop('action');
			var clashInfoLink = $form.find('a.timetable-clash-link').data('href');

			var $button = $form.find('.sign-up-button');
			var originalVal = $button.val();

			$button.val('Loading...').prop('disabled', true);

			$.getJSON(clashInfoLink, { group:selectedFormGrpId, ts: new Date().getTime()},function(data) {
				if(data.clash) {
					$('.timetable-clash-info input[name="group"]').attr("value", selectedFormGrpId);
					$('.timetable-clash-info form').attr("action", signupPostFormLink);
					$form.find('a.timetable-clash-link').click();

					$button.val(originalVal).prop('disabled', false);
				} else {
					form.submit();
				}
			});
		});
	});
</script>
</#escape>
