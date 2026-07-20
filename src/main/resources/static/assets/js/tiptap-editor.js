(function () {
  'use strict';

  if (!window.TemporynTiptap) return;

  function uploadImage(file) {
    var headers = {};
    var tokenMeta = document.querySelector('meta[name="_csrf"]');
    var headerMeta = document.querySelector('meta[name="_csrf_header"]');
    if (tokenMeta && headerMeta) {
      headers[headerMeta.getAttribute('content')] = tokenMeta.getAttribute('content');
    }
    var data = new FormData();
    data.append('file', file);
    return fetch('/api/images', { method: 'POST', headers: headers, body: data })
      .then(function (response) {
        if (!response.ok) {
          throw new Error('upload failed');
        }
        return response.json();
      })
      .then(function (json) {
        return json.url;
      });
  }

  var editMount = document.getElementById('tiptap-editor');
  if (editMount) {
    var hidden = document.getElementById('content');
    if (!hidden) return;
    var instance = window.TemporynTiptap.createNotionEditor({
      element: editMount,
      content: hidden.value,
      placeholder: 'Type something…',
      editable: true,
      uploadImage: uploadImage,
    });
    var form = editMount.closest('form');
    if (form) {
      form.addEventListener('submit', function () {
        hidden.value = instance.getMarkdown();
      });

      // Ctrl/Cmd+S saves the document instead of triggering the browser save dialog.
      document.addEventListener('keydown', function (e) {
        if ((e.ctrlKey || e.metaKey) && (e.key === 's' || e.key === 'S')) {
          e.preventDefault();
          if (form.requestSubmit) form.requestSubmit();
          else form.submit();
        }
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
    setupHeadingAnchors(viewMount);
  }

  // GitHub-style slug so index links like [text](#slug) resolve to headings.
  function slugify(text) {
    return text
      .trim()
      .toLowerCase()
      .replace(/[^\w\u00c0-\uffff\s-]/g, '')
      .replace(/\s+/g, '-');
  }

  function setupHeadingAnchors(root) {
    var used = Object.create(null);
    var headings = root.querySelectorAll('h1, h2, h3, h4, h5, h6');
    headings.forEach(function (heading) {
      var base = slugify(heading.textContent || '');
      if (!base) return;
      var slug = base;
      var n = 1;
      while (used[slug]) {
        slug = base + '-' + n;
        n += 1;
      }
      used[slug] = true;
      heading.id = slug;
    });

    root.addEventListener('click', function (event) {
      var anchor = event.target.closest('a[href^="#"]');
      if (!anchor) return;
      var raw = anchor.getAttribute('href').slice(1);
      var id = decodeURIComponent(raw);
      var target = root.querySelector('#' + CSS.escape(id)) || document.getElementById(id);
      if (target) {
        event.preventDefault();
        target.scrollIntoView({ behavior: 'smooth', block: 'start' });
      }
    });
  }
})();

