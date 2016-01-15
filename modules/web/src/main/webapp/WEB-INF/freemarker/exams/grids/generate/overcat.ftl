<#escape x as x?html>
<#import "/WEB-INF/freemarker/modal_macros.ftl" as modal />
<#import "grid_macros.ftl" as grid />
<@modal.wrapper cssClass="modal-xl">
	<@modal.body>
		<div class="exam-grid-preview">
			<div class="overcat-key">
				<table class="table table-condensed">
					<thead>
					<tr>
						<th colspan="2">Key</th>
					</tr>
					</thead>
					<tbody>
						<tr>
							<td><span class="exam-grid-override">#</span></td>
							<td>Manually adjusted and not stored in SITS</td>
						</tr>
					</tbody>
				</table>
			</div>

			<h3>Select an overcatted mark for ${scyd.studentCourseDetails.student.fullName!} - ${scyd.studentCourseDetails.student.universityId}</h3>

			<p>You can change module marks but any changes made at this stage will not be saved back to SITS.</p>

			<form>
				<table class="table table-condensed grid overcat">
					<thead>
						<tr>
							<th colspan="${overcatView.optionsColumns?size}">Options</th>
							<td></td>
							<th colspan="${mapGet(overcatView.columnsBySCYD, scyd)?size}">${scyd.academicYear.toString} Modules (${scyd.studentCourseDetails.scjCode})</th>
							<#list overcatView.previousYearsSCYDs as thisSCYD>
								<td></td>
								<th colspan="${mapGet(overcatView.columnsBySCYD, thisSCYD)?size}">${thisSCYD.academicYear.toString} Modules (${thisSCYD.studentCourseDetails.scjCode})</th>
							</#list>
						</tr>
					</thead>
					<tbody>
						<tr class="category">
							<@grid.categoryRow categories=overcatView.optionsColumnsCategories columns=overcatView.optionsColumns showSectionLabels=false />
							<@grid.categoryRow categories=mapGet(overcatView.columnsBySCYDCategories, scyd) columns=mapGet(overcatView.columnsBySCYD, scyd) showSectionLabels=true />
							<#list overcatView.previousYearsSCYDs as thisSCYD>
								<td></td>
								<@grid.categoryRow categories=mapGet(overcatView.columnsBySCYDCategories, thisSCYD) columns=mapGet(overcatView.columnsBySCYD, thisSCYD) showSectionLabels=false />
							</#list>
						</tr>
						<tr class="title-in-category">
							<@grid.titleInCategoryRow categories=overcatView.optionsColumnsCategories columns=overcatView.optionsColumns showSectionLabels=false />
							<@grid.titleInCategoryRow categories=mapGet(overcatView.columnsBySCYDCategories, scyd) columns=mapGet(overcatView.columnsBySCYD, scyd) showSectionLabels=true />
							<#list overcatView.previousYearsSCYDs as thisSCYD>
								<td></td>
								<@grid.titleInCategoryRow categories=mapGet(overcatView.columnsBySCYDCategories, thisSCYD) columns=mapGet(overcatView.columnsBySCYD, thisSCYD) showSectionLabels=false />
							</#list>
						</tr>
						<tr>
							<@grid.headerRow columns=overcatView.optionsColumns showSectionLabels=false />
							<@grid.headerRow columns=mapGet(overcatView.columnsBySCYD, scyd) showSectionLabels=true />
							<#list overcatView.previousYearsSCYDs as thisSCYD>
								<td></td>
								<@grid.headerRow columns=mapGet(overcatView.columnsBySCYD, thisSCYD) showSectionLabels=false />
							</#list>
						</tr>
						<#list overcatView.overcattedEntities as entity>
							<tr class="student clickable">
								<#assign isFirstEntity = entity_index == 0 />
								<@grid.entityRows
									entity=entity
									isFirstEntity=isFirstEntity
									entityCount=overcatView.overcattedEntities?size
									columns=overcatView.optionsColumns
									columnValues=overcatView.optionsColumnValues
									showSectionLabels=false
								/>
								<@grid.entityRows
									entity=entity
									isFirstEntity=isFirstEntity
									entityCount=overcatView.overcattedEntities?size
									columns=mapGet(overcatView.columnsBySCYD, scyd)
									columnValues=mapGet(overcatView.columnsBySCYDValues, scyd)
									showSectionLabels=true
								/>
								<#list overcatView.previousYearsSCYDs as thisSCYD>
									<td></td>
									<@grid.entityRows
										entity=entity
										isFirstEntity=isFirstEntity
										entityCount=overcatView.overcattedEntities?size
										columns=mapGet(overcatView.columnsBySCYD, thisSCYD)
										columnValues=mapGet(overcatView.columnsBySCYDValues, thisSCYD)
										showSectionLabels=false
									/>
								</#list>
							</tr>
						</#list>
						<tr class="new-marks">
							<td colspan="${overcatView.optionsColumns?size}" class="borderless"></td>
							<th class="section-value-label">New Module Mark</th>
							<#assign currentCategory = "" />
							<#list mapGet(overcatView.columnsBySCYD, scyd) as column>
								<#if column.category?has_content && currentCategory != column.category>
									<#assign firstInCategory = true />
									<#assign currentCategory = column.category />
								<#else>
									<#assign firstInCategory = false />
								</#if>
								<td <#if firstInCategory!false>class="first-in-category"</#if>>
									<input type="text" class="form-control" name="newModuleMarks[${column.module.code}]" value="${mapGet(command.newModuleMarks, column.module)!}"/>
								</td>
							</#list>
						</tr>
					</tbody>
				</table>
			</form>
		</div>
	</@modal.body>
	<@modal.footer>
		<button type="button" class="btn btn-default" name="recalculate">Recalculate</button>
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
		}).on('keyup', updateButtons);

		$('.modal-footer').on('click', 'button[name=recalculate]', function(){
			$('.modal-footer button').prop('disabled', true);
			var $form = $('.modal-body form');
			$form.append($('<input/>').attr({
				'type': 'hidden',
				'name': 'recalculate'
			}));
			$.post('<@routes.exams.generateGridOvercatting department academicYear scyd/>', $form.serialize(), function(data){
				$('#edit-overcatting-modal').html(data);
			});
		}).on('click', 'button[name=continue]', function(){
			$('.modal-footer button').prop('disabled', true);
			var $form = $('.modal-body form');
			$form.append($('<input/>').attr({
				'type': 'hidden',
				'name': 'continue'
			}));
			$.post('<@routes.exams.generateGridOvercatting department academicYear scyd/>', $form.serialize(), function(data){
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