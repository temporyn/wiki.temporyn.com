/* 아티클 첨부파일 drag & drop 업로드 / 제거 */
(function () {
  'use strict';

  var dropzone = document.getElementById('dropzone');
  var input = document.getElementById('file-input');
  var pick = document.getElementById('file-pick');
  var list = document.getElementById('attachment-list');
  var tokenField = document.getElementById('uploadToken');
  if (!dropzone || !input || !list || !tokenField) return;

  var csrfToken = document.querySelector('meta[name="_csrf"]').getAttribute('content');
  var csrfHeader = document.querySelector('meta[name="_csrf_header"]').getAttribute('content');
  var token = tokenField.value;

  function humanSize(bytes) {
    if (bytes < 1024) return bytes + ' B';
    var units = ['KB', 'MB', 'GB', 'TB'];
    var size = bytes, i = -1;
    do { size /= 1024; i++; } while (size >= 1024 && i < units.length - 1);
    return size.toFixed(1) + ' ' + units[i];
  }

  function addRow(att) {
    var li = document.createElement('li');
    li.className = 'attach-item';
    li.setAttribute('data-id', att.id);
    li.innerHTML =
      '<a class="attach-name" href="' + att.downloadUrl + '"></a>' +
      '<span class="attach-size"></span>' +
      '<button type="button" class="attach-remove" data-id="' + att.id + '">제거</button>';
    li.querySelector('.attach-name').textContent = att.filename;
    li.querySelector('.attach-size').textContent = att.humanSize || humanSize(att.size);
    list.appendChild(li);
  }

  function upload(file) {
    var li = document.createElement('li');
    li.className = 'attach-item attach-uploading';
    li.innerHTML = '<span class="attach-name"></span><span class="attach-size">0%</span>';
    li.querySelector('.attach-name').textContent = file.name;
    list.appendChild(li);
    var progress = li.querySelector('.attach-size');

    var data = new FormData();
    data.append('file', file);
    data.append('token', token);

    var xhr = new XMLHttpRequest();
    xhr.open('POST', '/admin/attachments');
    xhr.setRequestHeader(csrfHeader, csrfToken);
    xhr.upload.addEventListener('progress', function (e) {
      if (e.lengthComputable) progress.textContent = Math.round((e.loaded / e.total) * 100) + '%';
    });
    xhr.addEventListener('load', function () {
      list.removeChild(li);
      if (xhr.status >= 200 && xhr.status < 300) {
        addRow(JSON.parse(xhr.responseText));
      } else {
        alert('업로드 실패: ' + file.name);
      }
    });
    xhr.addEventListener('error', function () {
      list.removeChild(li);
      alert('업로드 실패: ' + file.name);
    });
    xhr.send(data);
  }

  function uploadAll(files) {
    for (var i = 0; i < files.length; i++) upload(files[i]);
  }

  function remove(id, li) {
    var xhr = new XMLHttpRequest();
    xhr.open('POST', '/admin/attachments/' + id + '/delete');
    xhr.setRequestHeader(csrfHeader, csrfToken);
    xhr.addEventListener('load', function () {
      if (xhr.status >= 200 && xhr.status < 300) list.removeChild(li);
      else alert('제거 실패');
    });
    xhr.send();
  }

  pick && pick.addEventListener('click', function () { input.click(); });
  input.addEventListener('change', function () { uploadAll(input.files); input.value = ''; });

  ['dragenter', 'dragover'].forEach(function (ev) {
    dropzone.addEventListener(ev, function (e) { e.preventDefault(); dropzone.classList.add('dragover'); });
  });
  ['dragleave', 'drop'].forEach(function (ev) {
    dropzone.addEventListener(ev, function (e) { e.preventDefault(); dropzone.classList.remove('dragover'); });
  });
  dropzone.addEventListener('drop', function (e) {
    if (e.dataTransfer && e.dataTransfer.files.length) uploadAll(e.dataTransfer.files);
  });

  list.addEventListener('click', function (e) {
    var btn = e.target.closest('.attach-remove');
    if (!btn) return;
    var li = btn.closest('.attach-item');
    if (confirm('이 첨부파일을 제거할까요?')) remove(btn.getAttribute('data-id'), li);
  });
})();
