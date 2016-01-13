<#escape x as x?html>
<#import "/WEB-INF/freemarker/modal_macros.ftl" as modal />
<#import "grid_macros.ftl" as grid />
<@modal.wrapper cssClass="modal-xl">
	<@modal.body>

		<h3>Select an overcatted mark for ${scyd.studentCourseDetails.student.fullName!} - ${scyd.studentCourseDetails.student.universityId}</h3>

		<div class="exam-grid-preview">
			<table class="table table-condensed grid">
				<thead>
					<tr>
						<th colspan="${command.optionsColumns?size}">Options</th>
						<td></td>
						<th colspan="${mapGet(command.columnsBySCYD, scyd)?size}">${scyd.academicYear.toString} Modules (${scyd.studentCourseDetails.scjCode})</th>
						<#list command.previousYearsSCYDs as thisSCYD>
							<td></td>
							<th colspan="${mapGet(command.columnsBySCYD, thisSCYD)?size}">${thisSCYD.academicYear.toString} Modules (${thisSCYD.studentCourseDetails.scjCode})</th>
						</#list>
					</tr>
				</thead>
				<tbody>
					<tr class="category">
						<@grid.categoryRow categories=command.optionsColumnsCategories columns=command.optionsColumns showSectionLabels=false />
						<@grid.categoryRow categories=mapGet(command.columnsBySCYDCategories, scyd) columns=mapGet(command.columnsBySCYD, scyd) showSectionLabels=true />
						<#list command.previousYearsSCYDs as thisSCYD>
							<td></td>
							<@grid.categoryRow categories=mapGet(command.columnsBySCYDCategories, thisSCYD) columns=mapGet(command.columnsBySCYD, thisSCYD) showSectionLabels=false />
						</#list>
					</tr>
					<tr class="title-in-category">
						<@grid.titleInCategoryRow categories=command.optionsColumnsCategories columns=command.optionsColumns showSectionLabels=false />
						<@grid.titleInCategoryRow categories=mapGet(command.columnsBySCYDCategories, scyd) columns=mapGet(command.columnsBySCYD, scyd) showSectionLabels=true />
						<#list command.previousYearsSCYDs as thisSCYD>
							<td></td>
							<@grid.titleInCategoryRow categories=mapGet(command.columnsBySCYDCategories, thisSCYD) columns=mapGet(command.columnsBySCYD, thisSCYD) showSectionLabels=false />
						</#list>
					</tr>
					<tr>
						<@grid.headerRow columns=command.optionsColumns showSectionLabels=false />
						<@grid.headerRow columns=mapGet(command.columnsBySCYD, scyd) showSectionLabels=true />
						<#list command.previousYearsSCYDs as thisSCYD>
							<td></td>
							<@grid.headerRow columns=mapGet(command.columnsBySCYD, thisSCYD) showSectionLabels=false />
						</#list>
					</tr>
					<#list command.overcattedEntities as entity>
						<tr class="student clickable">
							<#assign isFirstEntity = entity_index == 0 />
							<@grid.entityRows
								entity=entity
								isFirstEntity=isFirstEntity
								entityCount=command.overcattedEntities?size
								columns=command.optionsColumns
								columnValues=command.optionsColumnValues
								showSectionLabels=false
							/>
							<@grid.entityRows
								entity=entity
								isFirstEntity=isFirstEntity
								entityCount=command.overcattedEntities?size
								columns=mapGet(command.columnsBySCYD, scyd)
								columnValues=mapGet(command.columnsBySCYDValues, scyd)
								showSectionLabels=true
							/>
							<#list command.previousYearsSCYDs as thisSCYD>
								<td></td>
								<@grid.entityRows
									entity=entity
									isFirstEntity=isFirstEntity
									entityCount=command.overcattedEntities?size
									columns=mapGet(command.columnsBySCYD, thisSCYD)
									columnValues=mapGet(command.columnsBySCYDValues, thisSCYD)
									showSectionLabels=false
								/>
							</#list>
						</tr>
					</#list>
				</tbody>
			</table>
		</div>

	</@modal.body>
	<@modal.footer>
		<button type="button" class="btn btn-default" name="continue">Continue</button>
		<button type="button" class="btn btn-default" name="cancel">Cancel</button>
	</@modal.footer>
</@modal.wrapper>

<script>
	jQuery(function($){
		$('.modal-body th.rotated, .modal-body td.rotated').each(function() {
			var width = $(this).find('.rotate').width();
			var height = $(this).find('.rotate').height();
			$(this).css('height', width + 15).css('width', height + 5);
			$(this).find('.rotate').css({
				'margin-top': -(height),
				'margin-left': height / 2
			});
		});
		var updateButtons = function(){
			if ($('.modal-body input:checked').length > 0) {
				$('.modal-footer button[name=continue]').prop('disabled', false);
			} else {
				$('.modal-footer button[name=continue]').prop('disabled', true);
			}
		};
		updateButtons();
		$('.modal-body table.grid').on('click', 'tr.clickable', function(){
			$(this).find('input').prop('checked', true);
			updateButtons();
		});
		$('.modal-footer').on('click', 'button[name=continue]', function(){
			$('.modal-footer button').prop('disabled', true);
			$.post('<@routes.exams.generateGridOvercatting department academicYear scyd/>', {'overcatChoice':$('.modal-body table.grid input:checked').val()}, function(data){
				if (!data.errors) {
					$('form.exam-grid-preview').append($('<input/>').attr({
						'type': 'hidden',
						'name': '${GenerateExamGridMappingParameters.previewAndDownload}'
					})).submit();
				} else {
					updateButtons();
				}
			});
		}).on('click', 'button[name=cancel]', function(){
			$(this).closest('.modal').modal('hide');
		});
	});
</script>

</#escape>