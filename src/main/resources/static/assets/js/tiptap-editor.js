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
    var raw = source ? source.value : '';
    window.TemporynTiptap.createNotionEditor({
      element: viewMount,
      content: buildIndexMarkdown(raw) + raw,
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

  // Scans raw Markdown (skipping fenced code blocks) for ATX headings (# .. ######).
  function extractHeadings(markdown) {
    var headings = [];
    var inFence = false;
    var fenceMarker = null;
    (markdown || '').split(/\r?\n/).forEach(function (line) {
      var fence = line.match(/^\s*(`{3,}|~{3,})/);
      if (fence) {
        if (!inFence) { inFence = true; fenceMarker = fence[1].charAt(0); }
        else if (line.trim().charAt(0) === fenceMarker) { inFence = false; }
        return;
      }
      if (inFence) return;
      var heading = line.match(/^(#{1,6})\s+(.+?)\s*#*\s*$/);
      if (heading) headings.push({ depth: heading[1].length, text: heading[2].trim() });
    });
    return headings;
  }

  // "---" hr, a bold-italic "index" label, then one link per heading; depth 2+
  // gets a dash prefix repeated (depth - 1) times so nesting reads without an indented list.
  function buildIndexMarkdown(markdown) {
    var headings = extractHeadings(markdown);
    if (!headings.length) return '';
    var used = Object.create(null);
    var lines = headings.map(function (h) {
      var base = slugify(h.text);
      var slug = base;
      var n = 1;
      while (used[slug]) { slug = base + '-' + n; n += 1; }
      used[slug] = true;
      var prefix = h.depth > 1 ? new Array(h.depth).join('\u2500') : '';
      return '[' + prefix + h.text + '](#' + slug + ')';
    });
    return '---\n\n***index***\n\n' + lines.join('\n') + '\n\n---\n\n';
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

