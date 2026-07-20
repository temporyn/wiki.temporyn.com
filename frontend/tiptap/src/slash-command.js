import { Extension } from '@tiptap/core';
import Suggestion from '@tiptap/suggestion';
import tippy from 'tippy.js';

const items = [
  {
    title: '텍스트',
    keywords: ['text', 'paragraph', '텍스트', '본문'],
    command: ({ editor, range }) => editor.chain().focus().deleteRange(range).setParagraph().run(),
  },
  {
    title: '제목 1',
    keywords: ['h1', 'heading', '제목'],
    command: ({ editor, range }) => editor.chain().focus().deleteRange(range).setNode('heading', { level: 1 }).run(),
  },
  {
    title: '제목 2',
    keywords: ['h2', 'heading', '제목'],
    command: ({ editor, range }) => editor.chain().focus().deleteRange(range).setNode('heading', { level: 2 }).run(),
  },
  {
    title: '제목 3',
    keywords: ['h3', 'heading', '제목'],
    command: ({ editor, range }) => editor.chain().focus().deleteRange(range).setNode('heading', { level: 3 }).run(),
  },
  {
    title: '글머리 목록',
    keywords: ['bullet', 'unordered', 'list', '목록'],
    command: ({ editor, range }) => editor.chain().focus().deleteRange(range).toggleBulletList().run(),
  },
  {
    title: '번호 목록',
    keywords: ['ordered', 'number', 'list', '번호'],
    command: ({ editor, range }) => editor.chain().focus().deleteRange(range).toggleOrderedList().run(),
  },
  {
    title: '체크리스트',
    keywords: ['task', 'todo', 'check', '체크', '할일'],
    command: ({ editor, range }) => editor.chain().focus().deleteRange(range).toggleTaskList().run(),
  },
  {
    title: '인용',
    keywords: ['quote', 'blockquote', '인용'],
    command: ({ editor, range }) => editor.chain().focus().deleteRange(range).toggleBlockquote().run(),
  },
  {
    title: '코드 블록',
    keywords: ['code', 'codeblock', '코드'],
    command: ({ editor, range }) => editor.chain().focus().deleteRange(range).toggleCodeBlock().run(),
  },
  {
    title: '구분선',
    keywords: ['divider', 'hr', 'rule', '구분'],
    command: ({ editor, range }) => editor.chain().focus().deleteRange(range).setHorizontalRule().run(),
  },
  {
    title: '표',
    keywords: ['table', '표'],
    command: ({ editor, range }) =>
      editor.chain().focus().deleteRange(range).insertTable({ rows: 3, cols: 3, withHeaderRow: true }).run(),
  },
  {
    title: '이미지',
    keywords: ['image', 'picture', '이미지', '그림'],
    command: ({ editor, range }) => {
      editor.chain().focus().deleteRange(range).run();
      const upload = editor.storage.imageUpload && editor.storage.imageUpload.handler;
      const input = document.createElement('input');
      input.type = 'file';
      input.accept = 'image/png,image/jpeg,image/gif,image/webp';
      input.addEventListener('change', () => {
        const file = input.files && input.files[0];
        if (!file) return;
        Promise.resolve(upload ? upload(file) : null)
          .then((url) => {
            if (url) {
              editor.chain().focus().setImage({ src: url }).run();
            }
          })
          .catch(() => {
            window.alert('이미지 업로드에 실패했습니다.');
          });
      });
      input.click();
    },
  },
];

class SlashMenuView {
  constructor({ items, command, editor }) {
    this.items = items;
    this.command = command;
    this.editor = editor;
    this.selectedIndex = 0;

    this.root = document.createElement('div');
    this.root.className = 'tiptap-slash-menu';
    this.render();
  }

  render() {
    this.root.innerHTML = '';
    if (this.items.length === 0) {
      const empty = document.createElement('div');
      empty.className = 'tiptap-slash-empty';
      empty.textContent = '결과 없음';
      this.root.appendChild(empty);
      return;
    }
    this.items.forEach((item, index) => {
      const button = document.createElement('button');
      button.type = 'button';
      button.className = 'tiptap-slash-item' + (index === this.selectedIndex ? ' is-active' : '');
      button.textContent = item.title;
      button.addEventListener('mousedown', (event) => {
        event.preventDefault();
        this.select(index);
      });
      this.root.appendChild(button);
    });
  }

  updateItems(items) {
    this.items = items;
    this.selectedIndex = 0;
    this.render();
  }

  select(index) {
    const item = this.items[index];
    if (item) {
      this.command(item);
    }
  }

  onKeyDown(event) {
    if (this.items.length === 0) {
      return false;
    }
    if (event.key === 'ArrowDown') {
      this.selectedIndex = (this.selectedIndex + 1) % this.items.length;
      this.render();
      return true;
    }
    if (event.key === 'ArrowUp') {
      this.selectedIndex = (this.selectedIndex + this.items.length - 1) % this.items.length;
      this.render();
      return true;
    }
    if (event.key === 'Enter') {
      this.select(this.selectedIndex);
      return true;
    }
    return false;
  }

  destroy() {
    this.root.remove();
  }
}

export const SlashCommand = Extension.create({
  name: 'slashCommand',

  addOptions() {
    return {
      suggestion: {
        char: '/',
        startOfLine: false,
        command: ({ editor, range, props }) => {
          props.command({ editor, range });
        },
      },
    };
  },

  addProseMirrorPlugins() {
    return [
      Suggestion({
        editor: this.editor,
        ...this.options.suggestion,
        items: ({ query }) => {
          const normalized = query.toLowerCase().trim();
          if (!normalized) {
            return items;
          }
          return items.filter((item) =>
            item.title.toLowerCase().includes(normalized) ||
            item.keywords.some((keyword) => keyword.toLowerCase().includes(normalized))
          );
        },
        render: () => {
          let view;
          let popup;

          return {
            onStart: (props) => {
              view = new SlashMenuView(props);
              popup = tippy('body', {
                getReferenceClientRect: props.clientRect,
                appendTo: () => document.body,
                content: view.root,
                showOnCreate: true,
                interactive: true,
                trigger: 'manual',
                placement: 'bottom-start',
                theme: 'temporyn-slash',
              })[0];
            },
            onUpdate: (props) => {
              view.updateItems(props.items);
              if (props.clientRect && popup) {
                popup.setProps({ getReferenceClientRect: props.clientRect });
              }
            },
            onKeyDown: (props) => {
              if (props.event.key === 'Escape') {
                popup.hide();
                return true;
              }
              return view.onKeyDown(props.event);
            },
            onExit: () => {
              if (popup) {
                popup.destroy();
              }
              if (view) {
                view.destroy();
              }
            },
          };
        },
      }),
    ];
  },
});
