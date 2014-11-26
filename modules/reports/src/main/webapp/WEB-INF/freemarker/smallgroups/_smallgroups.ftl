<#escape x as x?html>

<style>
	th.rotated .rotate, td.rotated .rotate {
		float: left;
		position: absolute;
		-webkit-transform-origin: 0% 100%;
		-moz-transform-origin: 0% 100%;
		-ms-transform-origin: 0% 100%;
		-o-transform-origin: 0% 100%;
		transform-origin: 0% 100%;
		-webkit-transform: rotateZ(90deg);
		-moz-transform: rotateZ(90deg);
		-ms-transform: rotateZ(90deg);
		-o-transform: rotateZ(90deg);
		transform: rotateZ(90deg);
		white-space: nowrap;
	}
	th.event, td.tutors {
		font-size: 80%;
		position: relative;
	}
	.tabula-page #main-content table.tablesorter > thead > tr th.header,
	.tabula-page #sb-container table.tablesorter > thead > tr th.header {
		background-position: bottom 10px right 0;
	}
</style>

<div><div class="sb-wide-table-wrapper">
	<table class="table table-bordered table-condensed table-striped table-sortable" style="width: auto; max-width: none;">
		<thead>
			<tr>
				<th class="sortable">First name</th>
				<th class="sortable">Last name</th>
				<th class="sortable">University ID</th>
				<#list events as event>
					<th class="event rotated">
						<div class="rotate">
							${event.moduleCode}
							${event.setName}
							${event.format}
							${event.groupName}
							${event.dayString} Week ${event.week}
						</div>
					</th>
				</#list>
				<th class="sortable"><i title="Unrecorded" class="icon-warning-sign icon-fixed-width late"></i></th>
				<th class="sortable"><i title="Missed events" class="icon-remove icon-fixed-width unauthorised"></i></th>
			</tr>
		</thead>
		<tbody>
			<#list students as student>
				<#assign missedCount = 0 />
				<#assign unrecordedCount = 0 />
				<tr>
					<td>
						${student.firstName}
					</td>
					<td>
						${student.lastName}
					</td>
					<td>
						<a href="<@routes.profile student />" target="_blank">${student.universityId}</a>
					</td>
					<#list events as event>
						<td>
							<#assign hasState = mapGet(mapGet(attendance, student), event)?? />
							<#if hasState>
								<#assign state = mapGet(mapGet(attendance, student), event).dbValue />
								<#if state == 'attended'>
									<i class="icon-fixed-width icon-ok attended"></i>
								<#elseif state == 'authorised'>
									<i class="icon-fixed-width icon-remove-circle authorised"></i>
								<#elseif state == 'unauthorised'>
									<i class="icon-fixed-width icon-remove unauthorised"></i>
									<#assign missedCount = missedCount + 1 />
								<#elseif event.late>
									<i class="icon-fixed-width icon-warning-sign late"></i>
									<#assign unrecordedCount = unrecordedCount + 1 />
								<#else>
									<i class="icon-fixed-width icon-minus unrecorded"></i>
								</#if>
							<#else>
								<i class="icon-fixed-width"></i>
							</#if>
						</td>
					</#list>
					<td class="unrecorded">
						<span class="badge badge-<#if (unrecordedCount > 2)>important<#elseif (unrecordedCount > 0)>warning<#else>success</#if>">
							${unrecordedCount}
						</span>
					</td>
					<td class="missed">
						<span class="badge badge-<#if (missedCount > 2)>important<#elseif (missedCount > 0)>warning<#else>success</#if>">
							${missedCount}
						</span>
					</td>
				</tr>
			</#list>
		</tbody>
		<tfoot>
			<tr>
				<th colspan="3" style="text-align: right;">
					Tutor/s
				</th>
				<#list events as event>
					<td class="tutors rotated">
						<div class="rotate">
							${event.tutors}
						</div>
					</td>
				</#list>
				<td></td>
				<td></td>
			</tr>
		</tfoot>
	</table>
</div></div>

<script>
	jQuery(function($){
		$('th.rotated').each(function() {
			var width = $(this).find('.rotate').width();
			var height = $(this).find('.rotate').height();
			$(this).css('height', width + 20).css('width', height + 5);
			$(this).find('.rotate').css('margin-top', -(width + 25));
		});
		$('td.rotated').each(function() {
			var width = $(this).find('.rotate').width();
			var height = $(this).find('.rotate').height();
			$(this).css('height', width).css('width', height + 5);
			$(this).find('.rotate').css('margin-top', -(height));
		});

		var popoutLinkHandler = function(event) {
			event.stopPropagation();
			event.preventDefault();
			if (!Shadowbox.initialized) {
				Shadowbox.initialized = true;
				Shadowbox.init(shadowboxOptions);
			}
			var tableWrapper = $(this).closest('div').find('div.sb-wide-table-wrapper');
			Shadowbox.open({
				link : this,
				content: '<div class="sb-wide-table-wrapper" style="background: white;">'
				+ tableWrapper.html()
				+ '</div>',
				player: 'html',
				width: $(window).width(),
				height: $(window).height(),
				options: {
					onFinish: function(){
						$('#sb-container').find('.table-sortable')
							.find('th.header').removeClass('header')
							.end().removeClass('tablesorter').sortableTable();
					}
				}
			});
		};

		var generatePopoutLink = function(){
			return $('<span/>')
				.addClass('sb-table-wrapper-popout')
				.append('(')
				.append(
				$('<a/>')
					.attr('href', '#')
					.html('Pop-out table')
					.on('click', popoutLinkHandler)
			).append(')');
		};

		$('div.sb-wide-table-wrapper > table').each(function(){
			var $this = $(this);
			$this.parent().parent('div').prepend(generatePopoutLink()).append(generatePopoutLink())
		}).sortableTable();
	})
</script>

</#escape>