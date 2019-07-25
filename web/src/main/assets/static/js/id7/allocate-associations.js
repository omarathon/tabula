/* eslint-env browser */
import $ from 'jquery';

$(() => {
  if ($('.allocate-associations').length === 0) {
    return;
  }

  const $fetchForm = $('form.fetch');
  const $studentFilter = $('#command').find('.student-filter');
  function EntityTable($table) {
    const rowMap = {};
    const $removeButton = $table.closest('div').find('button.remove-all');

    $table.find('tr[data-forentity]').each((i, el) => {
      const $this = $(el);
      const entityId = $this.data('forentity');
      if (rowMap[entityId] === undefined) {
        rowMap[entityId] = [];
      }
      rowMap[entityId].push($this);
      $this.detach();
    });

    $table.on('click', '.btn-default', (event) => {
      const $this = $(event.currentTarget);
      const $expandedInput = $this.closest('td').find('input');
      const $row = $this.closest('tr');
      const entityId = $row.data('entity');
      if ($row.hasClass('expanded')) {
        $table.find(`tr[data-forentity="${entityId}"]`).detach();
        $row.removeClass('expanded');
        $expandedInput.val('false');
      } else {
        $row.after(rowMap[entityId]);
        $row.addClass('expanded', true);
        $expandedInput.val('true');
      }
    });
    $table.find('tr.expanded').each((i, el) => {
      const $this = $(el);
      $this.after(rowMap[$this.data('entity')]);
    });

    $table.sortableTable({
      textExtraction: (node) => {
        const $el = $(node);
        if ($el.data('sortby')) {
          return $el.data('sortby');
        }
        return $el.text().trim();
      },
    });
    $table.on('sortStart', () => {
      $table.find('tr[data-forentity]').detach();
    }).on('sortEnd', () => {
      $table.find('tr').each((i, el) => {
        const $row = $(el);
        const entityId = $row.data('entity');
        if ($row.hasClass('expanded')) {
          $row.after(rowMap[entityId]);
        }
      });
    });

    $table.on('click tabula.selectDeselectCheckboxes.toggle', 'input', () => {
      if ($table.find('input:checked').length === 0) {
        $removeButton.prop('disabled', true).attr('title', 'You need to select some personal tutors from which to remove students');
      } else {
        $removeButton.prop('disabled', false).attr('title', 'All students will be removed from selected personal tutors');
      }
    });
    $removeButton.prop('disabled', true);

    this.getTable = () => $table;
  }

  const entityTable = new EntityTable($('.entities table'));

  const $studentTable = $('.students table');
  const $distributeSelectedButton = $studentTable.closest('div').find('button.distribute-selected');
  const $distributeAllButton = $studentTable.closest('div').find('button.distribute-all');

  const checkDistributeButtons = () => {
    const studentsChecked = $studentTable.find('input:checked').length > 0;
    const entitiesChecked = entityTable.getTable().find('input:checked').length > 0;
    if (!entitiesChecked) {
      $distributeSelectedButton.add($distributeAllButton).prop({
        disabled: true,
        title: 'You need to select some students and personal tutors to allocate',
      }).removeClass('btn-primary');
    } else {
      $distributeAllButton.prop('disabled', false).attr('title', 'All students will be equally distributed between selected personal tutors').addClass('btn-primary');
      if (studentsChecked) {
        $distributeSelectedButton.prop('disabled', false).attr('title', 'Selected students will be equally distributed between selected personal tutors').addClass('btn-primary');
      } else {
        $distributeSelectedButton.prop('disabled', true).attr('title', 'You need to select some students and personal tutors to allocate').removeClass('btn-primary');
      }
    }
  };
  $studentTable.sortableTable().on('click tabula.selectDeselectCheckboxes.toggle', 'input', checkDistributeButtons);
  entityTable.getTable().on('click tabula.selectDeselectCheckboxes.toggle', 'input', checkDistributeButtons);
  checkDistributeButtons();

  const $studentQuery = $('input[name=query]').on('keypress', (e) => {
    if (e.which === 13) {
      $(e.currentTarget).closest('form').submit();
    }
  }).attr('autocomplete', 'off');
  const $typeahead = $studentQuery.typeahead({
    source: (query, process) => {
      // Abort any existing search
      if (window.self.currentSearch) {
        window.self.currentSearch.abort();
        window.self.currentSearch = null;
      }
      window.self.currentSearch = $.ajax({
        url: $fetchForm.attr('action'),
        data: $fetchForm.serialize(),
        success: (data) => {
          process(data.unallocated);
        },
      });
    },
    selectOnBlur: false,
    item: '<li class="flexi-picker-result"><a href="#"><span class="name"></span> (<span class="universityId"></span>)</a></li>',
  }).data('typeahead');
  $typeahead.sorter = items => items;
  $typeahead.matcher = () => true;
  $typeahead.render = (items) => {
    const that = this;
    const result = $(items).map((i, item) => {
      if (item !== undefined) {
        const $item = $(that.options.item);
        $item.find('span.name').html(that.highlighter(`${item.firstName} ${item.lastName}`));
        $item.find('span.universityId').html(that.highlighter(item.universityId));
        return $item[0];
      }
      // no idea what's happened here. Return an empty item.
      return $(that.options.item)[0];
    });
    result.first().addClass('active');
    this.$menu.html(result);
    return this;
  };
  $typeahead.show = () => {
    const pos = $.extend({}, this.$element.offset(), {
      height: this.$element[0].offsetHeight,
    });

    this.$menu.appendTo($('body')).show().css({
      top: pos.top + pos.height, left: pos.left,
    });

    this.shown = true;
    return this;
  };
  $typeahead.updater = () => this.$menu.find('.active .universityId').text();
  const oldSelect = $typeahead.select;
  $typeahead.select = () => {
    oldSelect.call($typeahead);
    $fetchForm.submit();
  };

  $('.for-check-all').append(
    $('<input />', {
      type: 'checkbox',
      class: 'check-all use-tooltip',
      title: 'Select all/none',
    }),
  ).find('input').change(function onChange() {
    $(this).closest('table').selectDeselectCheckboxes(this);
  });

  $('.fix-area').fixHeaderFooter();

  const updateClearAllButton = ($el) => {
    const $filterList = $el.closest('.student-filter');

    if ($filterList.find('.empty-filter').length === $filterList.find('.btn-group').length) {
      $filterList.find('.clear-all-filters').attr('disabled', 'disabled');
    } else {
      $filterList.find('.clear-all-filters').removeAttr('disabled');
    }
  };

  const updateFilter = ($el) => {
    // Update the filter content
    const $list = $el.closest('ul');
    const shortValues = $list.find(':checked').map(function map() { return $(this).data('short-value'); }).get();
    const $fsv = $el.closest('.btn-group').find('.filter-short-values');
    if (shortValues.length) {
      $el.closest('.btn-group').removeClass('empty-filter');
      $fsv.html($fsv.data('prefix') + shortValues.join(', '));
    } else {
      $el.closest('.btn-group').addClass('empty-filter');
      $fsv.html($fsv.data('placeholder'));
    }

    $list.closest('.student-filter').find('button.apply').addClass('btn-primary');
    updateClearAllButton($el);
  };

  const prependClearLink = ($list) => {
    if (!$list.find('input:checked').length) {
      $list.find('.clear-this-filter').remove();
    } else if (!$list.find('.clear-this-filter').length) {
      $list.find('> ul').prepend(
        $('<li />').addClass('clear-this-filter')
          .append(
            $('<button />').attr('type', 'button')
              .addClass('btn btn-link')
              .html('<i class="fa fa-ban"></i> Clear selected items')
              .on('click', () => {
                $list.find('input:checked').each((i, el) => {
                  const $checkbox = $(el);
                  $checkbox.prop('checked', false);
                  updateFilter($checkbox);
                });
              }),
          ).append($('<hr />')),
      );
    }
  };

  $studentFilter.on('change', (e) => {
    const $input = $(e.target);
    if ($input.is('.prevent-reload')) return;
    updateFilter($input);
  });

  // Re-order elements inside the dropdown when opened
  $('.filter-list').closest('.btn-group').find('.dropdown-toggle').on('click.dropdown.data-api', function onClick() {
    const $this = $(this);
    if (!$this.closest('.btn-group').hasClass('open')) {
      // Re-order before it's opened!
      const $list = $this.closest('.btn-group').find('.filter-list');
      const items = $list.find('li.check-list-item').get();

      items.sort((a, b) => {
        const aChecked = $(a).find('input').is(':checked');
        const bChecked = $(b).find('input').is(':checked');

        if (aChecked && !bChecked) return -1;
        if (!aChecked && bChecked) return 1;
        return $(a).data('natural-sort') - $(b).data('natural-sort');
      });

      $.each(items, (item, el) => {
        $list.find('> ul').append(el);
      });

      prependClearLink($list);
    }
  });

  $studentFilter.prepend(
    $('<button class="btn btn-xs btn-default clear-all-filters" type="submit" disabled>Clear filter</button>'),
  );

  const $clearAllButtons = $('.clear-all-filters').on('click', function onClick() {
    $(this).closest('.student-filter').find('.filter-list').each(function each() {
      const $list = $(this);

      $list.find('input:checked').each(function each2() {
        const $checkbox = $(this);
        $checkbox.prop('checked', false);
        updateFilter($checkbox);
      });

      prependClearLink($list);
    });
  });

  $clearAllButtons.each(function each() {
    updateClearAllButton($(this));
  });

  const updateFilterFromPicker = ($picker, name, value, shortValue) => {
    if (value === undefined || value.length === 0) return;

    const v = shortValue || value;

    const $ul = $picker.closest('ul');

    const $li = $ul.find(`input[value="${value}"]`).closest('li');
    if ($li.length) {
      $li.find('input').prop('checked', true);
      if ($ul.find('li.check-list-item:first').find('input').val() !== value) {
        $li.insertBefore($ul.find('li.check-list-item:first'));
      }
    } else {
      $('<li/>').addClass('check-list-item').append(
        $('<label/>').addClass('checkbox').append(
          $('<input/>').attr({
            type: 'checkbox',
            name,
            value,
            checked: true,
          }).data('short-value', v),
        ).append(
          $picker.val(),
        ),
      ).insertBefore($ul.find('li.check-list-item:first'));
    }

    updateFilter($picker);
  };

  $('.route-search-query').on('change', function onChange() {
    const $picker = $(this);
    if ($picker.data('routecode') === undefined || $picker.data('routecode').length === 0) return;

    updateFilterFromPicker($picker, 'routes', $picker.data('routecode'), $picker.data('routecode').toUpperCase());

    $picker.data('routecode', '').val('');
  }).routePicker({});

  const $previewForm = $('form.preview');
  const $uploadForm = $('#uploadCommand');
  $previewForm.areYouSure({
    dirtyClass: 'dirty',
  });
  if ($previewForm.find('input[name^=additions]').length > 0 || $previewForm.find('input[name^=removals]').length > 0) {
    $previewForm.addClass('dirty');
  }
  $fetchForm.on('submit', () => {
    $previewForm.removeClass('dirty');
  });
  $uploadForm.on('submit', () => {
    $previewForm.removeClass('dirty');
  });
  $uploadForm.find('.btn.dirty-check-ignore').on('click', () => {
    if ($previewForm.is('.dirty')) {
      $previewForm.removeClass('dirty');
      window.setTimeout(() => { // Wait for the unload event for the file request
        $previewForm.addClass('dirty');
      }, 100);
    }
  });
});
