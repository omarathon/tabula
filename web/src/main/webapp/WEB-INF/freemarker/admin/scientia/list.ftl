<h1>Mapped Syllabus+ Rooms</h1>
<p>This is a list of all hardcoded Syllabus+ locations in Tabula. Tabula names have initially been derived from the map API but can be hardcoded to any value.</p>
<table class="table table-bordered table-striped table-condensed sortable">
	<thead>
		<tr>
			<th class="sortable">Syllabus+ name</th>
			<th class="sortable">Tabula name</th>
			<th class="sortable">Location ID</th>
		</tr>
	</thead>
	<tbody>
		<#list rooms as room>
			<tr>
				<#-- None of these should ever be null but if we guard we can see which one is if a mistake is made. -->
				<td><#if room.syllabusPlusName??>${room.syllabusPlusName}</#if></td>
				<td><#if room.name??>${room.name}</#if></td>
				<td><#if room.locationId??><span class="map" data-lid="${room.locationId}">${room.locationId}</span></#if></td>
			</tr>
		</#list>
	</tbody>
</table>
<script type="text/javascript">
	jQuery(function($) {
		$('.map[data-lid]').mapPopups({
			placement: 'left',
			expandClickTarget: true,
			template: '<div class="popover extra-wide"><div class="arrow"></div><div class="popover-inner"><button type="button" class="close" aria-hidden="true">&#215;</button><h3 class="popover-title"></h3><div class="popover-content"><p></p></div></div></div>',
			content: _.template(['<iframe width="800" height="600" frameborder="0" src="<%- mapUrl %>"></iframe>'])
		});

		$('table.sortable').sortableTable();
	});
</script>sor