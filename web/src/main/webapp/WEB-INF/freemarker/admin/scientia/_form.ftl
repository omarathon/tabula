<@f.form method="post" commandName="command">
	<@bs3form.labelled_form_group labelText="Syllabus+ name" path="upstreamName">
		<@f.input class="form-control" path="upstreamName" />
	</@bs3form.labelled_form_group>

	<@bs3form.labelled_form_group labelText="Campus map location" path="name">
		<@f.input class="form-control" path="name" id="mapLocation" />
	</@bs3form.labelled_form_group>

	<@f.hidden path="mapLocationId" id="mapLocationId" />

		<script>
			jQuery(function ($) {
				$('#mapLocation')
					.on('change', function () {
						$('#mapLocationId').val($(this).data('lid'));
					})
					.locationPicker();
			});
		</script>

		<button class="btn btn-primary">Save location</button>
		<a class="btn btn-default" href="<@routes.admin.locations />">Cancel</a>
</@f.form>
