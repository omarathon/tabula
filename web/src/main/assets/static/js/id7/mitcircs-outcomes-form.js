/* eslint-env browser */
import $ from 'jquery';

class MitCircsOutcomesForm {

  constructor(form) {
    this.form = form;
    this.init();
  }

  init() {
    const {form} = this;
    const $form = $(form);

    $form
      .find('.mitcircs-form__fields__section__nested-checkboxes')
      .each((i, container) => {
        const $container = $(container);
        const $checkboxes = $container.find('input[type="checkbox"]');
        const $target = $($container.data('target'));
        const targetValue = $container.data('target-value');
        const matchState = !!$container.data('match-state');

        const onChange = (initial) => {
          const $selected = $target.filter(':checked');
          const isSelected = targetValue ? ($selected.val() === targetValue) : $selected.length > 0;

          $container.collapse(isSelected ? 'show' : 'hide');
          $checkboxes.prop('disabled', !isSelected);

          if (!initial && matchState) {
            $checkboxes.prop('checked', isSelected);
          }
        };

        $target.on('input change', () => onChange(false));
        onChange(true);
      });

    $form
      .find('.mitcircs-form__fields__section__optional-question')
      .each((i, container) => {
        const $container = $(container);
        const $target = $(':input[name=outcomeGrading]');
        $target.on('input change', () => {
          const enabled = $target.filter(':checked').val() !== 'Rejected';
          $container.collapse(enabled ? 'show' : 'hide');
        }).trigger('change');
      });

    const $extensionContainer = $('.grant-extensions');
    const $extensionFields = $extensionContainer.find(':input');

    const extensionFieldDisable = ($fields, disableAll = false) => {
      $fields.each((index, field) => {
        const $field = $(field);
        const isEnabled = !disableAll && $field.closest('.row').find(':checkbox').is(':checked:visible');
        $(field).prop('disabled', !isEnabled)
      });
    };

    const extensionFieldToggle = () => {
      const isExtensions = $form.find(':input[name=acuteOutcome]:checked').val() === "Extension";
      $extensionContainer.collapse(isExtensions ? 'show' : 'hide');
      extensionFieldDisable($extensionFields, !isExtensions);
    };

    $form.on('input change', ':input[name=acuteOutcome]', extensionFieldToggle);
    extensionFieldToggle();

    $('.mitcircs-form__fields__section__affected-assessments input[type=checkbox]').on('input change', (e) => {
      const $checkbox = $(e.target);
      $checkbox.closest('.row').find('.date-time-picker').filter(':visible').prop('disabled', !$checkbox.is(':checked'));
    });
  }

}

function init() {
  $('.mitcircs-outcomes-form').each((i, el) => {
    $(el).data('tabula.mitCircsOutcomesForm', new MitCircsOutcomesForm(el));
  });
}

$(() => init());