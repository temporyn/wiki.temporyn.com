import { Editor } from '@tiptap/core';
import StarterKit from '@tiptap/starter-kit';
import Placeholder from '@tiptap/extension-placeholder';
import BubbleMenu from '@tiptap/extension-bubble-menu';
import Link from '@tiptap/extension-link';
import Image from '@tiptap/extension-image';
import { TaskList } from '@tiptap/extension-task-list';
import { TaskItem } from '@tiptap/extension-task-item';
import { Table } from '@tiptap/extension-table';
import { TableRow } from '@tiptap/extension-table-row';
import { TableHeader } from '@tiptap/extension-table-header';
import { TableCell } from '@tiptap/extension-table-cell';
import GlobalDragHandle from 'tiptap-extension-global-drag-handle';
import { Markdown } from 'tiptap-markdown';
import { SlashCommand } from './slash-command.js';
import { buildTableMenu } from './table-menu.js';

import 'tippy.js/dist/tippy.css';
import './bundle.css';

const BUBBLE_BUTTONS = [
  { label: 'B', title: '굵게', className: 'tt-b-bold', isActive: 'bold', run: (c) => c.toggleBold() },
  { label: 'i', title: '기울임', className: 'tt-b-italic', isActive: 'italic', run: (c) => c.toggleItalic() },
  { label: 'S', title: '취소선', className: 'tt-b-strike', isActive: 'strike', run: (c) => c.toggleStrike() },
  { label: '</>', title: '인라인 코드', className: 'tt-b-code', isActive: 'code', run: (c) => c.toggleCode() },
  { label: 'H1', title: '제목 1', className: 'tt-b-h1', isActive: ['heading', { level: 1 }], run: (c) => c.toggleHeading({ level: 1 }) },
  { label: 'H2', title: '제목 2', className: 'tt-b-h2', isActive: ['heading', { level: 2 }], run: (c) => c.toggleHeading({ level: 2 }) },
];

function buildBubbleMenu(getEditor) {
  const element = document.createElement('div');
  element.className = 'tiptap-bubble-menu';

  const buttons = BUBBLE_BUTTONS.map((config) => {
    const button = document.createElement('button');
    button.type = 'button';
    button.className = 'tiptap-bubble-btn ' + config.className;
    button.title = config.title;
    button.textContent = config.label;
    button.addEventListener('mousedown', (event) => {
      event.preventDefault();
      const editor = getEditor();
      if (!editor) return;
      config.run(editor.chain().focus()).run();
    });
    element.appendChild(button);
    return { button, config };
  });

  const linkButton = document.createElement('button');
  linkButton.type = 'button';
  linkButton.className = 'tiptap-bubble-btn tt-b-link';
  linkButton.title = '링크';
  linkButton.textContent = '🔗';
  linkButton.addEventListener('mousedown', (event) => {
    event.preventDefault();
    const editor = getEditor();
    if (!editor) return;
    if (editor.isActive('link')) {
      editor.chain().focus().unsetLink().run();
      return;
    }
    const url = window.prompt('링크 URL을 입력하세요');
    if (url) {
      editor.chain().focus().setLink({ href: url }).run();
    }
  });
  element.appendChild(linkButton);

  const syncState = () => {
    const editor = getEditor();
    if (!editor) return;
    buttons.forEach(({ button, config }) => {
      const active = Array.isArray(config.isActive)
        ? editor.isActive(config.isActive[0], config.isActive[1])
        : editor.isActive(config.isActive);
      button.classList.toggle('is-active', active);
    });
    linkButton.classList.toggle('is-active', editor.isActive('link'));
  };

  return { element, syncState };
}

export function createNotionEditor(options) {
  const { element, content = '', placeholder = '내용을 입력하세요…', editable = true, onUpdate } = options;

  let editorRef = null;
  let bubble = null;

  const extensions = [
    StarterKit.configure({
      codeBlock: { HTMLAttributes: { class: 'tiptap-code-block' } },
    }),
    Link.configure({ openOnClick: false, autolink: true, HTMLAttributes: { rel: 'noopener noreferrer' } }),
    Image.configure({ inline: false, allowBase64: true }),
    TaskList,
    TaskItem.configure({ nested: true }),
    Table.configure({ resizable: editable, allowTableNodeSelection: true }),
    TableRow,
    TableHeader,
    TableCell,
    Markdown.configure({
      html: true,
      transformPastedText: true,
      transformCopiedText: true,
      breaks: true,
    }),
  ];

  if (editable) {
    bubble = buildBubbleMenu(() => editorRef);
    const tableMenu = buildTableMenu(() => editorRef);
    extensions.push(
      Placeholder.configure({
        placeholder: ({ node }) =>
          node.type.name === 'heading' ? '제목' : placeholder,
        showOnlyWhenEditable: true,
      }),
      GlobalDragHandle.configure({ dragHandleWidth: 20, scrollTreshold: 100 }),
      BubbleMenu.configure({
        pluginKey: 'textBubbleMenu',
        element: bubble.element,
        shouldShow: ({ editor: current, from, to }) =>
          from !== to && !current.isActive('table') && !current.isActive('image'),
        tippyOptions: { duration: 120 },
      }),
      BubbleMenu.configure({
        pluginKey: 'tableBubbleMenu',
        element: tableMenu.element,
        shouldShow: ({ editor: current }) => current.isActive('table'),
        tippyOptions: { duration: 120, placement: 'top', maxWidth: 'none' },
      }),
      SlashCommand
    );
  }

  const editor = new Editor({
    element,
    content,
    editable,
    editorProps: {
      attributes: { class: 'doc-body tiptap-content' },
    },
    extensions,
    onCreate: () => {
      if (bubble) bubble.syncState();
    },
    onSelectionUpdate: () => {
      if (bubble) bubble.syncState();
    },
    onTransaction: () => {
      if (bubble) bubble.syncState();
    },
    onUpdate: ({ editor: current }) => {
      if (typeof onUpdate === 'function') {
        onUpdate(current.storage.markdown.getMarkdown());
      }
    },
  });

  editorRef = editor;

  return {
    editor,
    getMarkdown: () => editor.storage.markdown.getMarkdown(),
    focus: () => editor.commands.focus(),
    destroy: () => editor.destroy(),
  };
}

window.TemporynTiptap = { createNotionEditor };
