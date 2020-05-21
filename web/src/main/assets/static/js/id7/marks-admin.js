/* eslint-env browser */
import $ from 'jquery';

$(() => {
  $('.fix-area').fixHeaderFooter();

  // Auto grade generator
  $('.auto-grade[data-mark][data-generate-url]').each((i, el) => {
    const $input = $(el);
    const generateUrl = $input.data('generate-url');
    const $markInput = $input.closest('form').find(`[name="${$input.data('mark')}"]`);
    const $select = $input.next('select');

    if ($input.length && $markInput.length && $select.length) {
      let currentRequest;

      const doRequest = () => {
        if (currentRequest !== undefined) {
          currentRequest.abort();
        }

        if ($markInput.val().length === 0) {
          $input.show().prop('disabled', false);
          $select.prop('disabled', true).hide();

          return;
        }

        const data = {
          mark: $markInput.val(),
        };

        if ($select.is(':visible')) {
          data.existing = $select.val();
        } else if ($input.val().length > 0) {
          data.existing = $input.val();
        }

        currentRequest = $.ajax(generateUrl, {
          type: 'POST',
          data,
          success: (html) => {
            $select.html(html);
            if ($select.find('option').length > 1) {
              $input.hide().prop('disabled', true);
              $select.prop('disabled', false).show();
            } else {
              $input.show().prop('disabled', false);
              $select.prop('disabled', true).hide();
            }
          },
          error: (xhr, errorText) => {
            if (errorText !== 'abort') {
              $input.show().prop('disabled', false);
            }
          },
        });
      };

      $markInput.on('change input', doRequest);
      doRequest();
    }
  });

  // Treat enter as tab in .marks-form
  $('form.marks-form').each((i, form) => {
    const $form = $(form);

    // All visible inputs that:
    // - Aren't buttons (including submit inputs)
    // - Aren't textareas (so we can still add newlines)
    $form.on('keydown', ':input:visible:not(:button):not(input[type="submit"]):not(textarea)', (e) => {
      if (e.key === 'Enter') {
        e.preventDefault();

        // Tabbable inputs are all form inputs, buttons and links that are visible and aren't
        // excluded from tabindex
        const $tabbable = $form.find(':input, :button, a').filter(':visible:not([tabindex="-1"])');
        const currentIndex = $tabbable.index(document.activeElement);

        $tabbable.eq(currentIndex + 1).focus();
      }
    });
  });
});