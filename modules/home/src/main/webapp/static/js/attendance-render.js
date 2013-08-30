/**
* Scripts used only by the attendance monitoring section.
*/
(function ($) { "use strict";

var exports = {};

exports.Manage = {}

$(function(){
	var setupCollapsible = function($this, $target){
		$this.css('cursor', 'pointer').on('click', function(){
			var $chevron = $this.find('i.icon-fixed-width');
			if ($this.hasClass('expanded')) {
				$chevron.removeClass('icon-chevron-down').addClass('icon-chevron-right');
				$this.removeClass('expanded');
				$target.hide();
			} else {
				$chevron.removeClass('icon-chevron-right').addClass('icon-chevron-down');
				$this.addClass('expanded');
				$target.show();
			}
		});
		if ($this.hasClass('expanded')) {
			$target.show();
		} else {
			$target.hide();
		}
	};

	var updateRouteCounter = function(){
		var total = $('.routeAndYearPicker').find('div.scroller tr').filter(function(){
			return $(this).find('input:checked').length > 0;
		}).length;
		if (total === 0) {
			$('.routeAndYearPicker').find('span.routes-count').html('are no routes');
		} else if (total === 1) {
			$('.routeAndYearPicker').find('span.routes-count').html('is 1 route');
		} else {
         	$('.routeAndYearPicker').find('span.routes-count').html('are ' + total + ' routes');
         }
	};

	$('.striped-section.routes .collapsible').each(function(){
		var $this = $(this), $target = $this.closest('.item-info').find('.collapsible-target');
		setupCollapsible($this, $target);
	});

	$('.routeAndYearPicker').find('.collapsible').each(function(){
		var $this = $(this), $target = $this.parent().find('.collapsible-target');
		if ($target.find('input:checked').length > 0) {
			$this.addClass('expanded');
			updateRouteCounter();
		}
		setupCollapsible($this, $target);
	}).end().find('tr.years th:gt(0)').each(function(){
		var $this = $(this), oldHtml = $this.html();
		$this.empty().append(
			$('<input/>').attr({
				'type':'checkbox',
				'title':'Select all/none'
			}).on('click', function(){
				var checked = $(this).prop('checked');
				$this.closest('div').find('div.scroller tr').each(function(){
					$(this).find('td.year_' + $this.data('year') + ' input').not(':disabled').prop('checked', checked);
				});
				updateRouteCounter();
			})
		).append(oldHtml);
	}).end().find('td input:not(:disabled)').on('click', function(){
		var $this = $(this);
		$this.closest('div.collapsible-target').find('th.year_' + $this.data('year') + ' input').prop('checked', false);
		updateRouteCounter();
	});

	var bindPointButtons = function(){
    	$('#addMonitoringPointSet a.new-point, #addMonitoringPointSet a.edit-point, #addMonitoringPointSet a.delete-point').off().on('click', function(e){
    		e.preventDefault();
    		var formLoad = function(data){
    			var $m = $('#modal');
    			$m.html(data);
    			if ($m.find('.monitoring-points').length > 0) {
    				$('#addMonitoringPointSet .monitoring-points').replaceWith($m.find('.monitoring-points'))
    				$m.modal("hide");
    				bindPointButtons();
    				return;
    			}
    			var $f = $m.find('form');
    			$f.on('submit', function(event){
    				event.preventDefault();
    			});
    			$m.find('.modal-footer button[type=submit]').on('click', function(){
    				$(this).button('loading');
    				$.post($f.attr('action'), $f.serialize(), function(data){
    					$(this).button('reset');
    					formLoad(data);
    				})
    			});
    			$m.off('shown').on('shown', function(){
    				$f.find('input[name="name"]').focus();
    			}).modal("show");
    		};
    		$.post($(this).attr('href'), $('form#addMonitoringPointSet').serialize(), formLoad)
    	});
    };
    bindPointButtons();
});

window.Attendance = jQuery.extend(window.Attendance, exports);

}(jQuery));

