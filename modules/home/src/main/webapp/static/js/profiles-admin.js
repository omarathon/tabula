/**
 * Scripts used only by the profiles admin section. 
 */
(function ($) { "use strict";

// TODO this is bulk copied from groups-admin.js, which is unfinished and therefore is going to diverge. Make this into a plugin

function updateFilters(){
    var studentList=$("#studentslist li");
    var controls = $("#filter-controls input[type=checkbox]");
    // get a list of (json-stringed) name/value pairs for attributes to hide
    var hidden = $(controls).map(function(e,control){
        var $control = $(control);
        if ( ! $control.is(":checked")){ // unchecked means we will hide any students with this attribute
           var hideThis = {};
           hideThis[$control.data("filter-attr")]=$control.data("filter-value");
           return JSON.stringify(hideThis);
        }
    });

    // now go through all the students and hide/show each as appropriate
    $(studentList).each(function(i, ele){
        setVisibility(ele, hidden);
     });
}

function setVisibility(element, hiddenAttrs){
    var $element = $(element);
    var data = $element.data();

    // convert any data-f-* attributes into JSON
    // n.b. jQuery data() camel-cases attributes;
    // it converts data-f-Bar="foo" into data()[fBar]=foo
    var stringData = [];
    for (var prop in data){
        if (prop.match("^f[A-Z]")){
            var o = {};
            o[prop] = data[prop];
            stringData.push(JSON.stringify(o));
        }
    }

    // if this element has any attributes on the hidden list, it
    // should not be visible. Otherwise, show it.
    var visible = true;
    $(hiddenAttrs).each(function(i,attr){
        if ($.inArray(attr, stringData) > -1){
            visible = false;
            return false; // break out of the loop early
        }
    });
    $element.toggle(visible);
}


// Drag and drop allocation
$(function() {

    $("#filter-controls").on('click','input[type=checkbox]', function(){
         updateFilters();
    });


	$('#allocateStudentsToTutorsCommand')
		.dragAndDrop({
			itemName: 'student',
			textSelector: '.name h6',
			useHandle: false,
			selectables: '.students .drag-target',
			scroll: true,
			removeTooltip: 'Remove this student from this tutor'
		})
		.each(function(i, container) {
			var $container = $(container);
			$container.find('a.random').on('click', function(e) {			
				$container.dragAndDrop('randomise');
				
				e.preventDefault();
				e.stopPropagation();
				return false;
			});
		});
	
	if ($('#allocateStudentsToTutorsCommand .student-list').length) {
		// Manage button disabled-ness when there are items in/out the source and target lists
		var manageButtons = function() {
			var stillToAllocate = $('#allocateStudentsToTutorsCommand .student-list .drag-list li').length;
			
			if (stillToAllocate > 0) {
				$('#allocateStudentsToTutorsCommand a.random').removeClass('disabled');
			} else {
				$('#allocateStudentsToTutorsCommand a.random').addClass('disabled');
			}
			
			var alreadyAllocated = $('#allocateStudentsToTutorsCommand .tutors .drag-list li').length;
			
			if (alreadyAllocated > 0) {
				$('#allocateStudentsToTutorsCommand a.return-items').removeClass('disabled');
			} else {
				$('#allocateStudentsToTutorsCommand a.return-items').addClass('disabled');
			}
		};
		
		$('#allocateStudentsToTutorsCommand .student-list').on('changed.tabula', manageButtons);
		manageButtons();
	}
});
}(jQuery));