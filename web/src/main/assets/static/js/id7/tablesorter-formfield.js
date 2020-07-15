// Add a custom sorter that uses the value of the first visible form field
(($) => {
  $.tablesorter.addParser({
    // use a unique id
    id: 'formfield',

    // return false if you don't want this parser to be auto detected
    is: () => false,

    format: (text, table, cell) => $(':input:visible', cell).val(),

    // flag for filter widget (true = ALWAYS search parsed values; false = search cell text)
    parsed: true,

    // set the type to either numeric or text (text uses a natural sort function
    // so it will work for everything, but numeric is faster for numbers
    type: 'text',
  });
})(window.jQuery);
