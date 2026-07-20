/* Temporyn — theme toggle, sidebar tree, and search */
(function () {
  'use strict';

  var BASE = (document.body && document.body.dataset.baseurl) || '';

  /* ── Theme toggle (persisted in localStorage) ──────────────────────── */
  (function themeToggle() {
    var btn = document.getElementById('theme-toggle');
    if (!btn) return;

    // Always default to light; the system preference is intentionally ignored.
    function current() {
      return document.documentElement.getAttribute('data-theme') || 'light';
    }
    btn.addEventListener('click', function () {
      var next = current() === 'dark' ? 'light' : 'dark';
      document.documentElement.setAttribute('data-theme', next);
      try { localStorage.setItem('temporyn-theme', next); } catch (e) {}
    });
  })();

  /* ── Sidebar tree ──────────────────────────────────────────────────── */
  var OPEN_KEY = 'temporyn-open';

  function loadOpen() {
    try { return new Set(JSON.parse(localStorage.getItem(OPEN_KEY) || '[]')); }
    catch (e) { return new Set(); }
  }
  function saveOpen(set) {
    try { localStorage.setItem(OPEN_KEY, JSON.stringify(Array.from(set))); }
    catch (e) {}
  }
  function normalize(p) {
    try { p = decodeURIComponent(p); } catch (e) {}
    if (p.charAt(p.length - 1) !== '/') p += '/';
    return p;
  }

  (function tree() {
    var nav = document.getElementById('nav-tree');
    if (!nav) return;

    var open = loadOpen();

    // 1) Restore previously expanded folders.
    nav.querySelectorAll('.tree-dir').forEach(function (li) {
      if (open.has(li.dataset.path)) li.classList.add('open');
    });

    // 2) Mark the current article active and expand its ancestor folders (persisting state).
    var cur = normalize(location.pathname);
    var changed = false;
    nav.querySelectorAll('.tree-file').forEach(function (li) {
      var full = normalize(BASE + li.dataset.url);
      if (full === cur) {
        li.classList.add('active');
        var p = li.parentElement;
        while (p && p !== nav) {
          if (p.classList && p.classList.contains('tree-dir')) {
            p.classList.add('open');
            if (!open.has(p.dataset.path)) { open.add(p.dataset.path); changed = true; }
          }
          p = p.parentElement;
        }
        li.scrollIntoView({ block: 'center' });
      }
    });
    if (changed) saveOpen(open);

    // 3) Toggle folders on click.
    nav.addEventListener('click', function (e) {
      var btn = e.target.closest('.tree-toggle');
      if (!btn) return;
      var li = btn.parentElement;
      li.classList.toggle('open');
      if (li.classList.contains('open')) open.add(li.dataset.path);
      else open.delete(li.dataset.path);
      saveOpen(open);
    });
  })();

  /* ── Full-text search (title + content, server-side) ───────────────── */
  (function search() {
    var input = document.getElementById('search-input');
    var results = document.getElementById('search-results');
    var navTree = document.getElementById('nav-tree');
    if (!input || !results || !navTree) return;

    var timer = null;
    var seq = 0;

    function showTree() {
      results.hidden = true;
      results.innerHTML = '';
      navTree.hidden = false;
    }

    function escapeHtml(s) {
      return String(s).replace(/[&<>"]/g, function (c) {
        return { '&': '&amp;', '<': '&lt;', '>': '&gt;', '"': '&quot;' }[c];
      });
    }

    function render(hits) {
      navTree.hidden = true;
      results.hidden = false;
      if (!hits.length) {
        results.innerHTML = '<li class="sr-empty">No results</li>';
        return;
      }
      results.innerHTML = hits.map(function (d) {
        return '<li><a href="' + d.url + '">' +
               escapeHtml(d.title) +
               (d.snippet ? '<span class="sr-cat">' + escapeHtml(d.snippet) + '</span>' : '') +
               '</a></li>';
      }).join('');
    }

    function run(q) {
      var mine = ++seq;
      fetch(BASE + '/api/search?q=' + encodeURIComponent(q))
        .then(function (r) { return r.ok ? r.json() : []; })
        .then(function (hits) {
          if (mine !== seq) return; // ignore stale responses
          render(hits);
        })
        .catch(function () {
          if (mine !== seq) return;
          results.hidden = false;
          navTree.hidden = true;
          results.innerHTML = '<li class="sr-empty">Search failed</li>';
        });
    }

    input.addEventListener('input', function () {
      var q = input.value.trim();
      clearTimeout(timer);
      if (!q) { seq++; showTree(); return; }
      timer = setTimeout(function () { run(q); }, 200);
    });
    input.addEventListener('keydown', function (e) {
      if (e.key === 'Escape') { input.value = ''; clearTimeout(timer); seq++; showTree(); input.blur(); }
    });
  })();

  /* ── Mobile sidebar ────────────────────────────────────────────────── */
  (function mobileSidebar() {
    var btn = document.getElementById('sidebar-toggle');
    var sidebar = document.getElementById('sidebar');
    if (!btn || !sidebar) return;
    btn.addEventListener('click', function () { sidebar.classList.toggle('open'); });
    sidebar.addEventListener('click', function (e) {
      if (e.target.closest('a')) sidebar.classList.remove('open');
    });
  })();
})();
