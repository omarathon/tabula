/**
* Scripts used when recording attendance in Attendance Monitoring or Small Group Teaching.
*/
(function ($) { "use strict";

var exports = {};

exports.createButtonGroup = function(id){
    var $this = $(id), selectedValue = $this.find('option:selected').val();
    var activeButton = $('.recordCheckpointForm div.forCloning div.btn-group')
        .clone(true)
        .insertAfter($this)
        .find('button').filter(function(){
            return $(this).data('state') == selectedValue;
        }).addClass('active');
    if ($this.attr('title') && $this.attr('title').length > 0) {
        activeButton.attr('title', '<p>' + activeButton.attr('title') +'</p>' + $this.attr('title'));
    }
    $this.hide();
};

var setArgOnUrl = function(url, argName, argValue){
	if(url.indexOf('?') === 0) {
		return url + '?' + argName + '=' + argValue;
	} else {
		var args = url.substring(url.indexOf('?') + 1, url.length).split('&'),
			found = false,
			newArgs = $.map(args, function(pair){
				var arg = pair.split('=');
				if (arg[0] === argName) {
					found = true;
					return argName + '=' + argValue;
				} else {
					return pair;
				}
			});
		if (!found) {
			newArgs.push(argName + '=' + argValue);
		}
		return url.substring(0, url.indexOf('?')) + '?' + newArgs.join('&');
	}
};

exports.bindButtonGroupHandler = function(enableCheckForCheckpoints) {
	var checkForCheckpoints = function(){
		var attendanceData = {};
		$('form#recordAttendance').find('select').each(function(){
			attendanceData[$(this).data('universityid')] = $(this).val();
		});
		$.post('/attendance/check/smallgroup', {
			'occurrence' : $('form#recordAttendance').data('occurrence'),
			'attendances' : attendanceData
		}, function(data){
			$('form#recordAttendance .checkpoints-message .students').popover('destroy').removeClass('tabulaPopover-init');
			if (data.length === 0) {
				$('form#recordAttendance .checkpoints-message').hide();
			} else {
				var popoverContent = $('<ul/>');
				$.each(data, function(i, student){
					popoverContent.append($('<li/>').html(student.name));
				});
				$('form#recordAttendance .checkpoints-message').show()
					.find('.students').html(data.length + ' student' + ((data.length > 1)?'s':''))
					.data('content', popoverContent.wrap('<div></div>').parent().html())
					.tabulaPopover({
						trigger: 'click'
					});
			}
		});
	};

    $('#recordAttendance').on('click', 'div.btn-group button', function(e){
        var $this = $(this);
        if ($this.is('.disabled')) {
        	e.stopPropagation();
        	e.preventDefault();
        	return false;
        } else {
	        $this.closest('div.pull-right').find('option').filter(function(){
	            return $(this).val() == $this.data('state');
	        }).prop('selected', true);
			var noteButton = $this.closest('div.pull-right').find('a.attendance-note');
			noteButton.attr('href', setArgOnUrl(noteButton.attr('href'), 'state', $this.data('state')));
	    }

		if (enableCheckForCheckpoints) {
			checkForCheckpoints();
		}
    });

	if (enableCheckForCheckpoints) {
		$(function(){
			checkForCheckpoints();
		});
	}
};

exports.enableCheckForCheckpoints = function() {
	$('#recordAttendance').on('click', 'div.btn-group button', function(e){
		$('#recordAttendance')
	});
};

$(function(){
	// SCRIPTS FOR RECORDING MONITORING POINTS

	$('.recordCheckpointForm').find('.fix-header')
        .find('div.pull-right').show()
        .end().each(function(){
		$(this).find('.btn-group button').each(function(i){
			$(this).on('click', function(){
				$('.attendees .item-info').each(function(){
					$(this).find('button').eq(i).trigger('click');
				})
			});
		});
	}).end().find('a.meetings').on('click', function(e){
        e.preventDefault();
        $.get($(this).attr('href'), function(data){
            $('#modal .modal-body').html(data);
            $('#modal').modal("show");
            $('.use-popover').tabulaPopover({
                trigger: 'click',
                container: '#container'
            });
        });
    });

    $('.agent-search').find('input').on('keyup', function(){
        var rows = $('table.agents tbody tr'), query = $(this).val().toLowerCase();
        if (query.length === 0) {
            rows.show();
            rows.find('p.student-list').hide();
            $('.agent-search span.muted').hide();
        } else if (query.length < 3) {
            $('.agent-search span.muted').show();
        } else {
            $('.agent-search span.muted').hide();
            rows.each(function(){
                var $row = $(this)
                    , $agentCell = $row.find('td.agent')
                    , $agentName = $agentCell.find('h6')
                    , $students = $agentCell.find('p.student-list')
                    , showRow = false;
                if ($agentName.text().toLowerCase().indexOf(query) >= 0) {
                    showRow = true;
                }
                if ($students.text().toLowerCase().indexOf(query) >= 0) {
                    showRow = true;
                    $students.find('span').show().filter('.name').filter(function(){
                        return $(this).text().toLowerCase().indexOf(query) === -1
                    }).hide();
                    $students.show();
                    $students.find('span.name:visible').last().find('span.comma').hide();
                } else {
                    $students.hide();
                }
                if (showRow) {
                    $row.show();
                } else {
                    $row.hide();
                }
            });
        }
    }).end().show();

	// END SCRIPTS FOR RECORDING MONITORING POINTS
});

window.AttendanceRecording = jQuery.extend(window.AttendanceRecording, exports);

}(jQuery));