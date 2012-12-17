/**
 * Scripts used only by student profiles. 
 */
(function ($) { "use strict";

var exports = {};

$(function() {
	$('.profile-search').each(function() {
		var container = $(this);
		
		container.find('input.span4').removeClass('span4').addClass('span5');
		container.find('.input-append').removeClass('input-append').find('input.btn').hide();
		
		var target = container.find('form').attr('action') + '.json';
		
		var profilePickerMappings;
		container.find('input[name="query"]').each(function() {
			$(this).typeahead({
				source: function(query, process) {
					$.get(target, { query : query }, function(data) {				
						var labels = []; // labels is the list of Strings representing assignments displayed on the screen
						profilePickerMappings = {};
						
						$.each(data, function(i, member) {
							var mapKey = member.name + ";" + member.id + ";" + member.userId;
							profilePickerMappings[mapKey] = member;
							labels.push(mapKey);
						})
	
						process(labels);
					});
				},
				
				// Disable some typeahead behaviour that we already do in searching
				matcher: function(item) { return true; },
				//sorter: function(items) { return items; },
				//highlighter: function(item) { return item; },
				
				updater: function(mapKey) {
					var member = profilePickerMappings[mapKey];
					window.location = '/profiles/view/' + member.id;
					
					return member.name;
				},
				item: '<li><a href="#"><img class="photo pull-right"><h2 class="name"></h2><span class="description"></a></li>',
				minLength:3
			});
			
			var typeahead = $(this).data('typeahead');
			typeahead.render = function(items) {			
				items = $(items).map(function (i, item) {
					var member = profilePickerMappings[item];
				
					i = $(typeahead.options.item).attr('data-value', member.name + ";" + member.id + ";" + member.userId)
					i.find('.name').html(typeahead.highlighter(member.name))
					i.find('.description').html(member.description)
					i.find('img').attr('src', '/profiles/view/photo/' + member.id + '.jpg');
					return i[0];
				});
				
				items.first().addClass('active');
				this.$menu.html(items);
				return typeahead;
			};
		});	
	});
});

// take anything we've attached to "exports" and add it to the global "Courses"
// we use extend() to add to any existing variable rather than clobber it
window.Profiles = jQuery.extend(window.Profiles, exports);


}(jQuery));