<#import "*/_filters.ftl" as filters />
<#import "*/cm2_macros.ftl" as cm2 />
<#escape x as x?html>
	<@cm2.assignmentHeader "Marking" assignment />

	<div id="profile-modal" class="modal fade profile-subset"></div>

	<#-- Filtering -->
	<div class="fix-area">
		<div class="fix-header pad-when-fixed">
			<div class="filters marker-feedback-filters btn-group-group well well-sm" data-lazy="true">
				<@f.form commandName="command" action="${info.requestedUri.path}" method="GET" cssClass="form-inline filter-form">
					<@f.errors cssClass="error form-errors" />
					<button type="button" class="clear-all-filters btn btn-link">
						<span class="fa-stack">
							<i class="fa fa-filter fa-stack-1x"></i>
							<i class="fa fa-ban fa-stack-2x"></i>
						</span>
					</button>

					<#assign placeholder = "All marking statuses" />
					<#assign currentfilter><@filters.current_filter_value "markerStateFilters" placeholder; f>${f.description}</@filters.current_filter_value></#assign>
					<@filters.filter "marker-status" "command.markerStateFilters" placeholder currentfilter allMarkerStateFilters; f>
						<input type="checkbox" name="${status.expression}"
									 value="${f.name}"
									 data-short-value="${f.description}"
									${filters.contains_by_filter_name(command.markerStateFilters, f)?string('checked','')}>
						${f.description}
					</@filters.filter>

					<#assign placeholder = "All submission states" />
					<#assign currentfilter><@filters.current_filter_value "submissionStatesFilters" placeholder; f>${f.description}</@filters.current_filter_value></#assign>
					<@filters.filter "submission-states" "command.submissionStatesFilters" placeholder currentfilter allSubmissionStatesFilters; f>
						<input type="checkbox" name="${status.expression}"
									 value="${f.name}"
									 data-short-value="${f.description}"
									 ${filters.contains_by_filter_name(command.submissionStatesFilters, f)?string('checked','')}>
						${f.description}
					</@filters.filter>

					<#assign placeholder = "All plagiarism statuses" />
					<#assign currentfilter><@filters.current_filter_value "plagiarismFilters" placeholder; f>${f.description}</@filters.current_filter_value></#assign>
					<@filters.filter "plagiarism-status" "command.plagiarismFilters" placeholder currentfilter allPlagiarismFilters; f>
						<input type="checkbox" name="${status.expression}"
									 value="${f.name}"
									 data-short-value="${f.description}"
									 ${filters.contains_by_filter_name(command.plagiarismFilters, f)?string('checked','')}>
						${f.description}
					</@filters.filter>

				</@f.form>
			</div>
		</div>
	</div>

	<div class="filter-results admin-assignment-list">
		<i class="fa fa-spinner fa-spin"></i> Loading&hellip;
	</div>

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

			$body.on('shown.bs.collapse', function(e){
				var $row = $(e.target);
				$row.tabulaAjaxForm();
			});

			// remove attachment
			$body.on("click", '.remove-attachment', function(e) {
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

				if($form.find('input[name=attachedFiles]').length === 0){
					var $blankInput = $('<input name="attachedFiles" type="hidden" />');
					$form.append($blankInput);
				}
			});

			// copy feedback
			$body.on('click', '.copy-feedback', function(e){
				e.preventDefault();
				var $this = $(this);
				var $prevFeedback = $this.closest('.previous-marker-feedback');
				var $comments = $prevFeedback.find('.feedback-comments');
				var $attachments = $prevFeedback.find('.feedback-attachments li');
				var $form = $('.marking-and-feedback form');
				var $newComments = $form.find('textarea');
				if ($newComments.val()){
					$newComments.val($newComments.val() + '\n\n')
				}
				$newComments.val($newComments.val() + $comments.val());
				var $newAttachments = $form.find('ul.attachments');
				$newAttachments.append($attachments.clone());
				$newAttachments.parent('.form-group.hide').removeClass('hide');
				$this.addClass('disabled').text('Feedback copied');
			});

			var bigListOptions = {

				setup: function(){

					var $container = this, $outerContainer = $container.closest('.marking-stage');

					$('.form-post', $outerContainer).click(function(event){
						event.preventDefault();
						var $this = $(this);
						if(!$this.hasClass("disabled")) {
							var action = this.href;
							var $form = $('<form></form>').attr({method: 'POST', action: action}).hide();
							var doFormSubmit = false;

							if ($container.data('checked') !== 'none' || $this.closest('.must-have-selected').length === 0) {
								var $checkedBoxes = $(".collection-checkbox:checked", $container);
								$form.append($checkedBoxes.clone());
								doFormSubmit = true;
							}

							if (doFormSubmit) {
								$(document.body).append($form);
								$form.submit();
							} else {
								return false;
							}
						}
					});
				},

				onSomeChecked : function() {
					var $markingStage = this.closest('.marking-stage');
					if(this.find('.ready-next-stage input:checked').length){
						$markingStage.find('.must-have-selected').removeClass('disabled');
					} else {
						$markingStage.find('.must-have-selected').addClass('disabled');
					}
				},

				onNoneChecked : function() {
					var $markingStage = this.closest('.marking-stage');
					$markingStage.find('.must-have-selected').addClass('disabled');
				}
			};

			var firstTime = true;
			$(document).on('tabula.filterResultsChanged', function(e) {
				$('a.ajax-modal').ajaxModalLink();

				$('.marking-table')
					.bigList(bigListOptions)
					.on('show.bs.collapse', function (e) {
						var $target = $(e.target);
						var id = $target.attr('id');

						// Use history.pushState here if supported as it stops the page jumping
						if (window.history && window.history.pushState && window.location.hash !== ('#' + id)) {
							window.history.pushState({}, document.title, window.location.pathname + '#' + id);
						} else {
							window.location.hash = id;
						}
					});

				if (firstTime && window.location.hash && $(window.location.hash).length) {
					var $target = $(window.location.hash);
					$target.collapse(); // opens

					var $source = $('[data-target="' + window.location.hash + '"]');
					if ($source.length) {
						// Scroll to the right location
						$('html, body').animate({
							scrollTop: $source.offset().top - 150
						}, 300);
					}
				}

				firstTime = false;
			});

			// prevent rows from expanding when selecting the checkbox column
			$body.on('click', '.check-col', function(e){
				e.stopPropagation()
			});

			// hide / show the feedback form when approve / make changes radio is present
			$body.on('change', 'input[type=radio][name=changesState]', function(e){
				var show = this.value === "make-changes";
				$(this).closest('.online-marking').find('.marking-and-feedback').toggle(show);
			});

		})(jQuery);
	</script>
</#escape>