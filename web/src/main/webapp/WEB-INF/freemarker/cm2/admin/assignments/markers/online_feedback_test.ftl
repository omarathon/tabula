<#import "/WEB-INF/freemarker/_profile_link.ftl" as pl />
<h1>${assignment.name} (${assignment.module.code?upper_case})</h1>

<table class="table table-striped">
	<thead>
	<tr>
		<th>First name</th>
		<th>Last name</th>
		<th>Progress</th>
	</tr>
	</thead>
	<tbody>
		<tr data-toggle="collapse" data-target="#student${student.userId}" class="clickable collapsed expandable-row">
			<td><h6 class="toggle-icon-large">&nbsp;${student.firstName}</h6></td>
			<td><h6>${student.lastName}&nbsp;<#if student.warwickId??><@pl.profile_link student.warwickId /><#else><@pl.profile_link student.userId /></#if></h6></td>
			<td><marquee>PROGRESSSSSSS!!</marquee></td>
		</tr>
		<#assign detailUrl><@routes.cm2.markerOnlineFeedback assignment marker student /></#assign>
		<tr id="student${student.userId}" data-detailurl="${detailUrl}" class="collapse detail-row">
			<td colspan="3" class="detailrow-container">
				<i class="fa fa-spinner fa-spin"></i> Loading
			</td>
		</tr>
	</tbody>
</table>
<script type="text/javascript">
	(function($) {

		var $body = $('body');

		// on cancel collapse the row and nuke the form
		$body.on('click', '.cancel', function(e){
			e.preventDefault();
			var $row = $(e.target).closest('.detail-row');
			$row.collapse("hide");

			$row.on('hidden.bs.collapse', function(e) {
				$row.data('loaded', false);
				$row.find('.detailrow-container').html('<i class="fa fa-spinner fa-spin"></i> Loading');
				$(this).unbind(e);
			});
		});

		// on reset fetch the form again
		$body.on('click', '.reset', function(e){
			e.preventDefault();
			var $row = $(e.target).closest('.detail-row');
			$row.data('loaded', false);
			$row.trigger('show.bs.collapse');
		});

		// remove attachment js
		$body.on("click", ".remove-attachment", function(e) {
			e.preventDefault();
			var $this = $(this);
			var $form = $this.closest('form');
			var $li = $this.closest("li");
			$li.find('input, a').remove();
			$li.find('span').wrap('<del />');
			$li.find('i').css('display', 'none');
			var $ul = $li.closest('ul');

			if (!$ul.find('li').last().is('.pending-removal')) {
				var alertMarkup = '<li class="pending-removal">Files marked for removal won\'t be deleted until you <samp>Save</samp>.</li>';
				$ul.append(alertMarkup);
			}

			if($('input[name=attachedFiles]').length === 0){
				var $blankInput = $('<input name="attachedFiles" type="hidden" />');
				$form.append($blankInput);
			}
		});

	})(jQuery);
</script>