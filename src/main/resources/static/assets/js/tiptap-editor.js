(function () {
  'use strict';

  if (!window.TemporynTiptap) return;

  var editMount = document.getElementById('tiptap-editor');
  if (editMount) {
    var hidden = document.getElementById('content');
    if (!hidden) return;
    var instance = window.TemporynTiptap.createNotionEditor({
      element: editMount,
      content: hidden.value,
      placeholder: '내용을 입력하세요…',
      editable: true,
    });
    var form = editMount.closest('form');
    if (form) {
      form.addEventListener('submit', function () {
        hidden.value = instance.getMarkdown();
      });
    }
    return;
  }

  var viewMount = document.getElementById('tiptap-view');
  if (viewMount) {
    var source = document.getElementById('view-content');
    window.TemporynTiptap.createNotionEditor({
      element: viewMount,
      content: source ? source.value : '',
      editable: false,
    });
  }
})();

