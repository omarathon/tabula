/**
* Scripts used only by the attendance monitoring section.
*/
(function ($) { "use strict";

var exports = {};

exports.Manage = {}

//$(function(){
//	var target = $('form.manage-points').attr('action'), $select = $('form.manage-points select[name=route]');
//	$select.on('change', function(){
//		$('.academicyears-target').html('<img src="/static/images/icon-spinner.gif"');
//		$('.sets-target').empty();
//		$('.points-target').empty();
//		$.get(target, { route: $(this).val() }, function(data){
//			$('.academicyears-target').html(data);
//		});
//	});
//	if ($select.val().length > 0) {
//		$select.change();
//	}
//});
//
//exports.Manage.bindChooseAcademicYear = function(){
//	$('div.create-buttons a.create-point').hide();
//	$('form.manage-points-academicyear select[name="academicyear"]').on('change', function(){
//		$('.sets-target').empty();
//        $('.points-target').empty();
//		var target = $('form.manage-points-academicyear').attr('action'), sendData = {
//			route: $('form.manage-points select[name=route]').val(),
//			academicYear: $('form.manage-points-academicyear select[name="academicyear"]').val()
//		}
//		$('.sets-target').html('<img src="/static/images/icon-spinner.gif"');
//		$.get(target, sendData, function(data){
//			$('.sets-target').html(data);
//		});
//	});
//};
//
//exports.Manage.onChooseYear = function(){
//	var target = $('form.manage-points-year').attr('action'), sendData = {
//		route: $('form.manage-points select[name=route]').val()
//	}
//	if ($('form.manage-points-year select[name="set-year"]').length > 0) {
//		sendData.year = $('form.manage-points-year select[name="set-year"]').val()
//	}
//	$('.points-target').html('<img src="/static/images/icon-spinner.gif"');
//	$.get(target, sendData, function(data){
//		$('.points-target').html(data);
//	});
//};
//
//exports.Manage.bindChooseYear = function(){
//	$('div.create-buttons a.create-point').hide();
//	$('form.manage-points-year select[name="set-year"]').on('change', exports.Manage.onChooseYear);
//};
//
//exports.Manage.bindNewSetButton = function(){
//	$('div.create-buttons a.create-set').on('click', function(e){
//		e.preventDefault();
//		var formLoad = function(data){
//			var $m = $('#modal');
//			$m.html(data);
//			if ($m.find('.monitoring-point-set').length > 0) {
//				var newYear = $m.find('.monitoring-point-set').data('year');
//				if ($('form.manage-points-year select[name="set-year"]').length === 0) {
//					$('form.manage-points select[name="route"]').change();
//				} else {
//					$('form.manage-points-year select[name="set-year"]').append(
//						$('<option/>').attr('value', newYear).html(newYear)
//					).val(newYear).change();
//				}
//				$m.modal("hide");
//				return;
//			}
//			var $f = $m.find('form');
//			$f.on('submit', function(event){
//				event.preventDefault();
//			});
//			$m.find('.modal-footer button[type=submit]').on('click', function(){
//				$(this).button('loading');
//				$.post($f.attr('action'), $f.serialize(), function(data){
//					$(this).button('reset');
//					formLoad(data);
//				})
//			});
//			$m.modal("show");
//		};
//		$.get($(this).attr('href'), { modal: true, route: $('form.manage-points select[name=route]').val() }, formLoad)
//	});
//};
//
//
//exports.Manage.showAndBindNewPointButton = function(){
//	$('div.create-buttons a.create-point').show().off().on('click', function(e){
//		e.preventDefault();
//		var formLoad = function(data){
//			var $m = $('#modal');
//			$m.html(data);
//			if ($m.find('.monitoring-point').length > 0) {
//				$('form.manage-points-year select[name="set-year"]').change();
//				$m.modal("hide");
//				return;
//			}
//			var $f = $m.find('form');
//			$f.on('submit', function(event){
//				event.preventDefault();
//			});
//			$m.find('.modal-footer button[type=submit]').on('click', function(){
//				$(this).button('loading');
//				$.post($f.attr('action'), $f.serialize(), function(data){
//					$(this).button('reset');
//					formLoad(data);
//				})
//			});
//			$m.modal("show");
//		};
//		$.get($(this).attr('href'), { modal: true, set: $('form.template input[name=set]').val() }, formLoad)
//	});
//};
//
//exports.Manage.bindUpdateSetForm = function(){
//	$('form.template').off().on('submit', function(e){
//		e.preventDefault();
//		var $f = $(this);
//		$f.find('input[type="submit"]').button('loading');
//		$f.find('span.error').empty();
//		$.post($f.attr('action'), $f.serialize(), function(data){
//			var button = $f.find('input[type="submit"]').button('reset');
//			if (button.data('spinContainer')) {
//				button.data('spinContainer').spin(false);
//			}
//			if ($.map(data.errors, function(e){ return e; }).length > 0) {
//				if (data.errors.templateName && data.errors.templateName.length > 0) {
//					$f.find('span.error').html(data.errors.templateName.join(', '));
//				} else {
//					$f.find('span.error').html('An unexpected error occurred');
//				}
//			}
//		});
//	});
//};
//
//exports.Manage.bindEditAndDeletePointButtons = function(){
//	$('div.point a.edit, div.point a.delete').on('click', function(e){
//		e.preventDefault();
//		var formLoad = function(data){
//			var $m = $('#modal');
//			$m.html(data);
//			if ($m.find('.monitoring-point').length > 0) {
//				$('form.manage-points-year select[name="set-year"]').change();
//				$m.modal("hide");
//				return;
//			}
//			var $f = $m.find('form');
//			$f.on('submit', function(event){
//				event.preventDefault();
//			});
//			$m.find('.modal-footer button[type=submit]').on('click', function(){
//				$(this).button('loading');
//				$.post($f.attr('action'), $f.serialize(), function(data){
//					$(this).button('reset');
//					formLoad(data);
//				})
//			});
//			$m.modal("show");
//		};
//		$.get($(this).attr('href'), { modal: true }, formLoad);
//	});
//};

$(function(){
	$('.striped-section.routes .collapsible').each(function(){
		var $this = $(this), $target = $this.closest('.item-info').find('.collapsible-target');
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
	})
});

window.Attendance = jQuery.extend(window.Attendance, exports);

}(jQuery));

