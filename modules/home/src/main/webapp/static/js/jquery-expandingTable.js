(function ($) { "use strict";
/*
	Applied to tables. When a table row is clicked then a some hidden content is made visible and placed beneath the
	row. The following rows in the table are moved down to make space for the content. Clicking on the row again hides
	the content.


	A table cell in each row is designated as the content cell. This cell should contain a the follwing markup
	Note that the class names given here can be overriden in the options.

	<td class="content-cell">
		<dl>
			<dt>
				<!-- cell content goes here - this will appear in the table -->
			</dt>
			<dd class="table-content-container">
				<!-- This is a placeholder for the content that will be displayed when a row is expanded -->
			<dd>
		</dl>
	</td>


	Options:
		contentCellSelector:
			A class added to the cell in the table that will contain the content (see above). Defaults to
			'content-cell'
		tableContainerSelector:
			A class added to the element that will hold the content (see above). Defaults to 'table-content-container'
		iconSelector:
			A class added to the element that will contain the state icon for the row. Defaults to 'toggle-icon'
		toggleCellSelector:
			A class added to each cell in the table that should trigger the toggle behaviour for the parent row.
			Defaults to 'toggle-controller'
		allowTableSort:
			Apply the tablesorter JQuery plugin to this table. Defaults to true
		tableSorterOptions:
			An option map that will be passed to the tableSorter plugin
*/
jQuery.fn.expandingTable = function(options) {

	options = options || {};

	var contentCellSelector = options.contentCellSelector || '.content-cell';
	var tableContainerSelector = options.tableContainerSelector || '.table-content-container';
	var iconSelector = options.iconSelector || '.toggle-icon';
	var toggleCellSelector = options.toggleCellSelector || '.toggle-cell';

	var allowTableSort = options.allowTableSort || true;
	var tableSorterOptions = options.tableSorterOptions || {};

	var iconMarkup = '<i class="icon-chevron-right icon-fixed-width"></i>';

	var $table = this.first();

	function repositionContentBoxes() {
		// These have to be positioned in the right order, so we loop through content-container rather than
		// directly on content
		$(tableContainerSelector).hide().each(function() {
			var contentID = $(this).attr('data-contentid');
			var $content = $('#content-' + contentID);

			if ($content.length) {
				var isOpen = $content.data('open');

				if (isOpen) {
					$(this).show();
					var $contentCell = $(this).closest(contentCellSelector);

					// Add bottom padding equivalent to the height of the content to the content cell
					var cellPosition = $contentCell.position();

					$contentCell.css('padding-bottom', '');
					var cellHeight = $contentCell.outerHeight();
					var contentHeight = $content.outerHeight();
					$contentCell.css('padding-bottom', contentHeight + 10);

					// Position the content div in the correct location
					$content.css({
						top: (cellPosition.top + cellHeight - 2) + 'px',
						left: ($table.position().left + 1) + 'px'
					});
				}
			}
		});
	}

	function hideContent($content, $row, $icon) {
		var $contentCell = $('.content-cell', $row);

		// Hide the content div and move it back to where it was before
		$content.hide();
		$contentCell.find('.table-content').append($content);

		// Remove the bottom padding on the content cell
		$contentCell.css('padding-bottom', '');

		// Remove any position data from the content div and hide
		$content.attr('style', 'display:none;');

		// Change the icon to closed
		$icon.removeClass('icon-chevron-down').addClass('icon-chevron-right');

		// Set the data
		$content.data('open', false);

		repositionContentBoxes();
	}

	function showContent($content, $row, $icon) {
		// Move the workflow div to be at the end of the offset parent and display it
		$('#main-content').append($content);
		$content.show();

		if ($row.is(':nth-child(odd)')) {
			$content.css('background-color', '#ebebeb');
		} else {
			$content.css('background-color', '#f5f5f5');
		}

		$content.css({
			width: ($row.closest('table').width() - 21) + 'px'
		});

		// Change the icon to open
		$icon.removeClass('icon-chevron-right').addClass('icon-chevron-down');

		// Set the data
		$content.data('open', true);

		repositionContentBoxes();
	}

	function toggleRow($content, $row, $icon) {
		if ($content.length) {
			var isOpen = $content.data('open');
			if (isOpen) {
				hideContent($content, $row, $icon);
			} else {
				if(options.contentUrl && !$content.data("loaded")) {
					var contentId = $row.attr("data-contentid");
					var dataUrl = options.contentUrl + '/' + contentId;
					$content.load(dataUrl, function() {
						showContent($content, $row, $icon);
						$content.data("loaded", "true");
					});
				} else {
					showContent($content, $row, $icon);
				}
			}
		}
	}

	// init
	$('tbody tr', $table).each(function() {
		var $row = $(this);
		// get the contentId for this row
		var contentId = $row.attr("data-contentid");
		var $content = $('#content-' + contentId);

		// add the arrow icon to the row
		var $iconCell = $(iconSelector, $row).first();
		var $icon = $(iconMarkup).css('margin-top', '2px');
		$iconCell.prepend(' ').prepend($icon);
		// find the cells in the row that will trigger a toggle when clicked.
		var $toggleCells = $(toggleCellSelector, $row);
		// add some styles to them and register the click event
		$toggleCells.css('cursor', 'pointer');
		$toggleCells.on('click', function(evt){
			toggleRow($content, $row, $icon);
			evt.preventDefault();
			evt.stopPropagation();
		});
	});

	if(allowTableSort) {
		$.tablesorter.addWidget({
			id: 'repositionContentBoxes',
			format: repositionContentBoxes
		});

		var widgetOptions = { widgets: ['repositionContentBoxes'] };
		var sortOptions = $.extend(widgetOptions, tableSorterOptions);
		$table.tablesorter(sortOptions);
	}

}


})(jQuery);