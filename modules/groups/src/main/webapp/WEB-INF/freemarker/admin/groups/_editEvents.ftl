<#escape x as x?html>
<input type="hidden" name="action" value="submit" id="action-input">
<#assign groups = smallGroupSet.groups />

<fieldset>
	<@form.row>
		<span class="legend" >Events <small>Add weekly events for these groups</small> </span>
	</@form.row>

	<div class="striped-section">
		<div class="clearfix">
			<h2 class="section-title with-button">Groups</h2>
		</div>

		<#if groups?size gt 0>
			<#include "_events.ftl" />
		</#if>
	</div>
</fieldset>

<#include "_events_modals.ftl" />

<script type="text/javascript">
	jQuery(function($) {
		$('#action-input').closest('form').on('click', '.update-only', function() {
			$('#action-input').val('update');
		});

		<#-- controller detects action=refresh and does a bind without submit -->
		$('.modal.refresh-form').on('hide', function(e) {
			// Ignore events that are something ELSE hiding and being propagated up!
			if (!$(e.target).hasClass('modal')) return;

			$('#action-input').val('refresh');
			$('#action-input').closest('form').submit();
		});

		// Open the first modal with an error in it
		$('.modal .error').first().closest('.modal').modal('show');

		// repeat these hooks for modals when shown
		$('body').on('shown', '.modal', function() {
			var $m = $(this);
			$m.find('input.lazy-time-picker').tabulaTimePicker();
		});
	});
</script>
</#escape>