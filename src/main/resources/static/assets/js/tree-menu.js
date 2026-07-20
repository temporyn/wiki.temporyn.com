/* Sidebar operations
   - Context menu: article (edit / rename / delete), folder (new doc / subfolder / rename / delete),
     empty space (top-level create, expand/collapse all)
   - Drag & drop: move an article or folder into another folder (drop on empty space = top level) */
(function () {
  'use strict';

  var nav = document.getElementById('nav-tree');
  var menu = document.getElementById('context-menu');
  var scroll = document.querySelector('.sidebar-scroll');
  if (!nav || !menu || !scroll) return;

  var OPEN_KEY = 'temporyn-open';
  var csrfToken = document.querySelector('meta[name="_csrf"]').getAttribute('content');
  var csrfHeader = document.querySelector('meta[name="_csrf_header"]').getAttribute('content');

  function post(url, body) {
    var headers = { 'Content-Type': 'application/json' };
    headers[csrfHeader] = csrfToken;
    return fetch(url, { method: 'POST', headers: headers, body: JSON.stringify(body) })
      .then(function (res) {
        if (res.ok) return res.json();
        return res.json().catch(function () { return {}; }).then(function (j) {
          throw new Error(j.message || j.error || ('Request failed (' + res.status + ')'));
        });
      });
  }

  function fail(e) { alertModal('Error', e.message || 'Something went wrong.'); }
  function reload() { location.reload(); }

  /* ── Modal (replaces native prompt/confirm) ────────────────────────── */
  var modal = {
    overlay: document.getElementById('app-modal'),
    title: document.getElementById('modal-title'),
    message: document.getElementById('modal-message'),
    input: document.getElementById('modal-input'),
    cancel: document.getElementById('modal-cancel'),
    confirm: document.getElementById('modal-confirm')
  };
  var modalClose = null;

  function closeModal(result) {
    if (!modalClose) return;
    var resolve = modalClose;
    modalClose = null;
    modal.overlay.hidden = true;
    document.removeEventListener('keydown', onModalKey, true);
    resolve(result);
  }

  function onModalKey(e) {
    if (e.key === 'Escape') { e.preventDefault(); closeModal(null); }
    else if (e.key === 'Enter' && !modal.input.hidden) { e.preventDefault(); submitModal(); }
  }

  function submitModal() {
    if (modal.input.hidden) { closeModal(true); return; }
    var value = modal.input.value.trim();
    if (!value) { modal.input.focus(); return; }
    closeModal(value);
  }

  function openModal(opts) {
    if (modalClose) closeModal(null);
    modal.title.textContent = opts.title || '';

    if (opts.message) { modal.message.textContent = opts.message; modal.message.hidden = false; }
    else { modal.message.hidden = true; }

    if (opts.input) {
      modal.input.hidden = false;
      modal.input.value = opts.value || '';
      modal.input.placeholder = opts.placeholder || '';
    } else {
      modal.input.hidden = true;
    }

    modal.confirm.textContent = opts.confirmLabel || 'OK';
    modal.cancel.textContent = opts.cancelLabel || 'Cancel';
    modal.cancel.hidden = !!opts.hideCancel;
    modal.confirm.classList.toggle('is-danger', !!opts.danger);
    modal.overlay.hidden = false;

    document.addEventListener('keydown', onModalKey, true);
    setTimeout(function () {
      if (opts.input) { modal.input.focus(); modal.input.select(); }
      else modal.confirm.focus();
    }, 0);

    return new Promise(function (resolve) { modalClose = resolve; });
  }

  modal.confirm.addEventListener('click', submitModal);
  modal.cancel.addEventListener('click', function () { closeModal(null); });
  modal.overlay.addEventListener('mousedown', function (e) {
    if (e.target === modal.overlay) closeModal(null);
  });

  /* Returns entered text, or null when cancelled. */
  function promptModal(title, opts) {
    opts = opts || {};
    return openModal({
      title: title, input: true, value: opts.value || '', placeholder: opts.placeholder || '',
      confirmLabel: opts.confirmLabel || 'OK'
    });
  }

  /* Returns true when confirmed, false/null otherwise. */
  function confirmModal(title, message, opts) {
    opts = opts || {};
    return openModal({
      title: title, message: message, danger: opts.danger,
      confirmLabel: opts.confirmLabel || 'OK'
    }).then(function (r) { return r === true; });
  }

  /* Simple notice with a single confirm button. */
  function alertModal(title, message) {
    return openModal({ title: title, message: message, confirmLabel: 'OK', hideCancel: true });
  }

  /* Path of the article currently being viewed or edited. */
  var currentPath = document.body.dataset.currentPath || '';
  var currentMode = document.body.dataset.currentMode === 'edit' ? 'edit' : 'view';

  /* Encode each path segment so spaces and non-ASCII names stay valid in URLs. */
  function pathUrl(path) {
    return '/' + currentMode + '/' + path.split('/').map(encodeURIComponent).join('/');
  }

  /* When a rename/move changes the current document's path, navigate there; otherwise just refresh the tree. */
  function followArticle(oldPath, newPath) {
    if (currentPath === oldPath) location.href = pathUrl(newPath);
    else reload();
  }

  function followDirectory(oldPath, newPath) {
    if (currentPath === oldPath || currentPath.indexOf(oldPath + '/') === 0) {
      location.href = pathUrl(newPath + currentPath.substring(oldPath.length));
    } else {
      reload();
    }
  }

  /* ── Create ─────────────────────────────────────────────────────── */
  function createArticle(parentPath) {
    promptModal('New document', { placeholder: 'Document name (no extension)', confirmLabel: 'Create' }).then(function (name) {
      if (!name) return;
      post('/api/articles', { parentPath: parentPath, name: name })
        .then(function (j) { location.href = '/view/' + j.path.split('/').map(encodeURIComponent).join('/'); })
        .catch(fail);
    });
  }

  function createDirectory(parentPath) {
    promptModal('New folder', { placeholder: 'Folder name', confirmLabel: 'Create' }).then(function (name) {
      if (!name) return;
      post('/api/directories', { parentPath: parentPath, name: name }).then(reload).catch(fail);
    });
  }

  /* ── Rename ────────────────────────────────────────────────────── */
  function renameArticle(path, current) {
    promptModal('Rename document', { value: current, confirmLabel: 'Rename' }).then(function (name) {
      if (name == null) return;
      post('/api/articles/rename', { path: path, name: name })
        .then(function (j) { followArticle(path, j.path); }).catch(fail);
    });
  }

  function renameDirectory(path, current) {
    promptModal('Rename folder', { value: current, confirmLabel: 'Rename' }).then(function (name) {
      if (name == null) return;
      post('/api/directories/rename', { path: path, name: name })
        .then(function (j) { followDirectory(path, j.path); }).catch(fail);
    });
  }

  /* ── Delete ─────────────────────────────────────────────────────── */
  function afterDelete(path, isDirectory) {
    var affectsCurrent = isDirectory
      ? (currentPath === path || currentPath.indexOf(path + '/') === 0)
      : (currentPath === path);
    if (affectsCurrent) location.href = '/';
    else reload();
  }

  function deleteArticle(path, name) {
    confirmModal('Delete document', 'Delete "' + name + '"? This cannot be undone.', { danger: true, confirmLabel: 'Delete' })
      .then(function (ok) {
        if (!ok) return;
        post('/api/articles/delete', { path: path })
          .then(function () { afterDelete(path, false); }).catch(fail);
      });
  }

  function deleteDirectory(path, name) {
    confirmModal('Delete folder', 'Delete "' + name + '" and all of its contents? This cannot be undone.', { danger: true, confirmLabel: 'Delete' })
      .then(function (ok) {
        if (!ok) return;
        post('/api/directories/delete', { path: path })
          .then(function () { afterDelete(path, true); }).catch(fail);
      });
  }

  /* ── Expand / collapse ──────────────────────────────────────────────── */
  function saveOpen(ids) {
    try { localStorage.setItem(OPEN_KEY, JSON.stringify(ids)); } catch (e) {}
  }
  function expandAll() {
    var ids = [];
    nav.querySelectorAll('.tree-dir').forEach(function (li) {
      li.classList.add('open');
      ids.push(li.dataset.path);
    });
    saveOpen(ids);
  }
  function collapseAll() {
    nav.querySelectorAll('.tree-dir').forEach(function (li) { li.classList.remove('open'); });
    saveOpen([]);
  }

  /* ── Context menu ──────────────────────────────────────────────────── */
  function hideMenu() { menu.hidden = true; }

  function showMenu(x, y, items) {
    menu.innerHTML = '';
    items.forEach(function (item) {
      var li = document.createElement('li');
      li.textContent = item.label;
      if (item.separated) li.className = 'context-menu-separated';
      li.addEventListener('click', function () { hideMenu(); item.action(); });
      menu.appendChild(li);
    });

    // Show first to measure, then keep the menu inside the viewport.
    menu.style.left = '0';
    menu.style.top = '0';
    menu.hidden = false;

    var margin = 8;
    var rect = menu.getBoundingClientRect();
    var left = x;
    var top = y;
    if (left + rect.width > window.innerWidth - margin) {
      left = Math.max(margin, x - rect.width);
    }
    if (top + rect.height > window.innerHeight - margin) {
      top = Math.max(margin, y - rect.height);
    }
    menu.style.left = left + 'px';
    menu.style.top = top + 'px';
  }

  scroll.addEventListener('contextmenu', function (e) {
    e.preventDefault();
    var file = e.target.closest('.tree-file');
    var dir = e.target.closest('.tree-dir');
    var items;

    // Non-openable files (non-Markdown) have no actions and no owning-folder menu.
    if (file && file.classList.contains('is-static')) {
      hideMenu();
      return;
    }

    if (file) {
      var fPath = file.dataset.path, fName = file.dataset.name;
      items = [
        {
          label: 'Edit',
          action: function () {
            location.href = '/edit/' + fPath.split('/').map(encodeURIComponent).join('/');
          }
        },
        { label: 'Rename', action: function () { renameArticle(fPath, fName); } },
        { label: 'Delete', action: function () { deleteArticle(fPath, fName); }, separated: true }
      ];
    } else if (dir) {
      var dPath = dir.dataset.path, dName = dir.dataset.name;
      items = [
        { label: 'New document', action: function () { createArticle(dPath); } },
        { label: 'New subfolder', action: function () { createDirectory(dPath); } },
        { label: 'Rename', action: function () { renameDirectory(dPath, dName); } },
        { label: 'Delete', action: function () { deleteDirectory(dPath, dName); }, separated: true }
      ];
    } else {
      /* Only empty space offers whole-tree actions. */
      items = [
        { label: 'New document', action: function () { createArticle(''); } },
        { label: 'New folder', action: function () { createDirectory(''); } },
        { label: 'Expand all', action: expandAll, separated: true },
        { label: 'Collapse all', action: collapseAll }
      ];
    }

    showMenu(e.clientX, e.clientY, items);
  });

  document.addEventListener('click', hideMenu);
  document.addEventListener('scroll', hideMenu, true);
  window.addEventListener('resize', hideMenu);

  /* ── Orphan image cleanup ──────────────────────────────────────────── */
  var cleanupBtn = document.getElementById('media-cleanup');
  if (cleanupBtn) {
    cleanupBtn.addEventListener('click', function () {
      confirmModal('Clean up unused images', 'This deletes images that no document references. Continue?', { confirmLabel: 'Clean up' })
        .then(function (ok) {
          if (!ok) return;
          cleanupBtn.classList.add('is-busy');
          post('/api/media/cleanup', {})
            .then(function (j) {
              alertModal('Cleanup complete', 'Deleted ' + (j.deleted || 0) + ' unused image(s).');
            })
            .catch(fail)
            .then(function () { cleanupBtn.classList.remove('is-busy'); });
        });
    });
  }

  /* ── Drag & drop move ──────────────────────────────────────────────── */
  var dragged = null;

  /* Upload OS files (dropped onto the sidebar) sequentially into targetPath. */
  function uploadFiles(fileList, targetPath) {
    var files = Array.prototype.slice.call(fileList);
    var uploaded = 0;

    function next(index) {
      if (index >= files.length) {
        if (uploaded > 0) reload();
        return;
      }
      var headers = {};
      headers[csrfHeader] = csrfToken;
      var data = new FormData();
      data.append('file', files[index]);
      if (targetPath) data.append('parentPath', targetPath);

      fetch('/api/files', { method: 'POST', headers: headers, body: data })
        .then(function (res) {
          if (!res.ok) {
            return res.json().catch(function () { return {}; }).then(function (j) {
              throw new Error(j.message || j.error || ('Upload failed (' + res.status + ')'));
            });
          }
          uploaded++;
        })
        .catch(fail)
        .then(function () { next(index + 1); });
    }

    next(0);
  }

  function clearDropTargets() {
    nav.querySelectorAll('.drop-target').forEach(function (el) { el.classList.remove('drop-target'); });
  }

  nav.addEventListener('dragstart', function (e) {
    var file = e.target.closest('.tree-file');
    var toggle = e.target.closest('.tree-toggle');
    if (file && e.target.closest('a')) {
      dragged = { type: 'article', path: file.dataset.path };
    } else if (toggle) {
      dragged = { type: 'directory', path: toggle.closest('.tree-dir').dataset.path };
    } else {
      return;
    }
    e.dataTransfer.effectAllowed = 'move';
    try { e.dataTransfer.setData('text/plain', dragged.path); } catch (x) {}
  });

  scroll.addEventListener('dragover', function (e) {
    var isFileDrag = !dragged && e.dataTransfer && Array.prototype.indexOf.call(e.dataTransfer.types || [], 'Files') >= 0;
    if (!dragged && !isFileDrag) return;
    e.preventDefault();
    if (isFileDrag) e.dataTransfer.dropEffect = 'copy';
    clearDropTargets();
    var dir = e.target.closest('.tree-dir');
    if (dir) dir.classList.add('drop-target');
  });

  scroll.addEventListener('drop', function (e) {
    var files = e.dataTransfer && e.dataTransfer.files;
    var isFileDrop = !dragged && files && files.length > 0;

    if (!dragged && !isFileDrop) return;
    e.preventDefault();
    clearDropTargets();

    var dir = e.target.closest('.tree-dir');
    var targetPath = dir ? dir.dataset.path : '';

    // OS file drop: upload into the folder, or to the vault root on empty space.
    if (isFileDrop) {
      uploadFiles(files, targetPath);
      return;
    }

    var moving = dragged;
    dragged = null;

    if (moving.type === 'directory' && dir && dir.dataset.path === moving.path) return;

    if (moving.type === 'article') {
      post('/api/articles/move', { path: moving.path, targetPath: targetPath })
        .then(function (j) { followArticle(moving.path, j.path); }).catch(fail);
    } else {
      post('/api/directories/move', { path: moving.path, targetPath: targetPath })
        .then(function (j) { followDirectory(moving.path, j.path); }).catch(fail);
    }
  });

  nav.addEventListener('dragend', function () { dragged = null; clearDropTargets(); });
})();
