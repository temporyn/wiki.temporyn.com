(function () {
  'use strict';

  var mount = document.getElementById('tiptap-editor');
  var hidden = document.getElementById('content');
  if (!mount || !hidden || !window.TemporynTiptap) return;

  var instance = window.TemporynTiptap.createNotionEditor({
    element: mount,
    content: hidden.value,
    placeholder: '내용을 입력하세요…',
  });

  var form = mount.closest('form');
  if (form) {
    form.addEventListener('submit', function () {
      hidden.value = instance.getMarkdown();
    });
  }
})();
