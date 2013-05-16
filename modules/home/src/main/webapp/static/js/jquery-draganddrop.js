/**

jQuery.draganddrop

A configurable plugin to define multiple sets of items, which can
be dragged between sets, either one at a time or in batch by making
a drag selection.

Run the plugin on an element that contains all the sets:

    $('#tutee-widget').dragAndDrop();

Each set must be at least a .drag-target containing a ul.drag-list.

The list must have a data-bindpath attribute relating to the collection
that this list will be bound to on the server. Each list item must then
contain a hidden field relating to its value. The script will use the
bindpath value to rename fields as they are moved about.

Example (showing some optional extras as below)

  <div id=tutee-widget>
    <a class="btn return-items">Unallocate students</a>
    <div class=drag-target>
      <h3>Students</h3>
      <ul class="drag-list return-list" data-bindpath=command.unsorted>
        <li>0001 <input type=hidden name="command.unsorted[0]" value=0001>
        <li>0002 <input type=hidden name="command.unsorted[1]" value=0002>
      </ul>
    </div>
    <div class=drag-target>
      <h3>Students</h3>
      <span class=drag-count></span>
      <a href=# class="btn show-list">List</a>
      <ul class="drag-list hide" data-bindpath=command.tutor></ul>
    </div>
  </div>

Optional extras:
 - Counter: add a .drag-count element and it will be kept up to date
       with the number of items inside that .drag-target.
 - Popup list: Add a .show-list button and it will trigger a popout
       listing all the items. Use this in conjunction with hiding the
       list itself (by adding .hide to .drag-list)
 - Return items: Add .return-list to ONE .drag-list then add a 
       .return-items button; it will be wired to move all items into
       that list.

Method calls (after initialising):

 - $('#tutee-widget').dragAndDrop('return')
        Returns items, same as .return-items button.

TODO: More options; Random allocation function.

*/
(function($){ "use strict";

    var DataName = "tabula-dnd";

    var DragAndDrop = function(element, options) {
        var sortables = '.drag-list';
        var $el = $(element);
        var self = this;
        var first_rows = {};

        // Returns all items to the .return-list.drag-list
        // assuming there is one.
        this.returnItems = function() {
            var $returnList = $el.find('.return-list');
            $el.find('li').appendTo($returnList[0]);
            $el.find(sortables).each(function(i, list) {
                listChanged($(list));
            });
        };

        this.randomise = function() {
            throw "Not implemented";
        };

        // call on a $(ul) when its content changes.
        var listChanged = function($list) {
            renameFields($list);
            var $target = $list.closest('.drag-target');
            if ($target.length) {
                updateCount($target);
            }
        };

        var returnItem = function($listItem) {
            var $returnList = $el.find('.return-list');
            $listItem.appendTo($returnList[0]);
            listChanged($listItem.closest('ul'));
            listChanged($returnList);
        };

        // Wire button to trigger returnItems
        $el.find('.return-items').click(function() {
            self.returnItems();
        });

        $el.find('.show-list').popover({
            content: function() {
                var lis = $(this)
                    .closest('.drag-target')
                    .find('li')
                    .map(function(i, li){
                        return '<li>'+$(li).text()+'</li>';
                    })
                    .toArray();
                return '<ul>'+lis.join('')+'</ul>';
            }
        });

        var $sortables = $el.find(sortables);

        var draggableOptions = {
            scroll: false,
            revert: 'invalid',
            handle: '.handle',
            containment: $el,

            start: function(event, ui) {
                var $li = $(this);
                var $dragTarget = $li.closest('.drag-target');
                $li.data('source-target', $dragTarget);

                var $selectedItems = $dragTarget.find('.ui-selected');

                if ($li.hasClass('ui-selected') && $selectedItems.length > 1) {
                    first_rows = $selectedItems.map(function(i, e) {
                        var $tr = $(e);
                        return {
                            tr : $tr.clone(true),
                            id : $tr.attr('id')
                        };
                    }).get();
                    $selectedItems.addClass('cloned');
                }
            },

            // helper returns the HTML item that follows the mouse
            helper: function(event) {
                var $element = $(event.currentTarget);
                var multidrag = $element.hasClass('ui-selected');
                var msg = $element.text();
                if (multidrag) msg = $element.closest('ul').find('.ui-selected').length + " items";
                return $('<div>')
                    .addClass('label')
                    .addClass('multiple-items-drag-placeholder')
                    .html(msg);
            },

            stop : function(event, ui) {
                // Unhighlight stuff else it gets messy-looking
                $el.find('.ui-selected').removeClass('.ui-selected');
            }

        };

        // Drag any list item by its handle
        $sortables.find('li')
            .draggable(draggableOptions)
            .prepend('<i class="icon-th icon-white handle"></i> ');

        // Drag-select
        $sortables.selectable({
                filter: 'li',
                cancel: '.handle'
            });

        var updateAllCounts = function() {
            $el.find('.drag-target').each(function(i, dragTarget){
                updateCount($(dragTarget));
            });
        };

        // Dropping onto any .drag-target
        $el.find('.drag-target').droppable({
            activate: function(event, ui) {
                //$(event.target).addClass('droponme-highlight');
            },
            deactivate: function(event, ui) {
                //$(event.target).removeClass('droponme-highlight');
            },
            drop: function(event, ui) {
                var $target = $(this);
                var $source = $(ui.draggable).data('source-target');
                var $sourceDragList = $source.find(sortables);
                var $dragList = $target.find(sortables);

                if (first_rows.length > 1) {
                    // multi-ball!
                    // have to re-draggable() these as they
                    // lost their senses during cloning.
                    $.each(first_rows, function(i, item) {
                        $(item.tr)
                        .removeAttr('style')
                        .removeClass('ui-draggable')
                        .data('draggable', null)
                        .data('ui-draggable', null)
                        .draggable(draggableOptions)
                        .appendTo($dragList);
                    });
                    $el.find('.cloned').remove();
                    first_rows = {};
                } else {
                    $dragList.append(ui.draggable);
                }

                $el.find('.ui-selected').removeClass('ui-selected');

                // update counts, lists, popups
                listChanged($dragList);
                listChanged($sourceDragList);
            }
        });

        // Initialise all count badges
        updateAllCounts();

    };

    // The jQ plugin itself is a basic adapter around DragAndDrop
    $.fn.dragAndDrop = function(options) {
        var dnd = $(this).data(DataName);
        if (options === 'return') {
            dnd.returnItems();
        } else if (options === 'randomise') {
            dnd.randomise();
        } else {
            $(this).each(function(i, element) {
                dnd = new DragAndDrop(element, options);
                $(element).data(DataName, dnd);
            });
        }
    };

    var updateCount = function($dragTarget) {
        // setTimeout is a silly hack to work around the fact that an object
        // just added or removed won't be reflected in the list straight away.
        // This is neater than the mathemagical alternative.
        setTimeout(function() {
            var $dragList = $dragTarget.find('.drag-list');
            $dragTarget.find('.drag-count').html($dragList.find('li').length);
        }, 10);
    };

    // Rename all form input for this department to represent the ordered list
    // NOTE only works if exactly 1 input in each li
    var renameFields = function($list) {
        var bindpath = $list.data('bindpath');
        if (!bindpath) throw "No data-bindpath on ul";
        $list.find('li input').each(function(i, field) {
            field.name = bindpath + '[' + i + ']';
        });
    };

})(jQuery);
