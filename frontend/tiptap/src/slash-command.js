import { Extension } from '@tiptap/core';
import Suggestion from '@tiptap/suggestion';
import tippy from 'tippy.js';

const items = [
  {
    title: 'Text',
    keywords: ['text', 'paragraph', 'body'],
    command: ({ editor, range }) => editor.chain().focus().deleteRange(range).setParagraph().run(),
  },
  {
    title: 'Heading 1',
    keywords: ['h1', 'heading', 'title'],
    command: ({ editor, range }) => editor.chain().focus().deleteRange(range).setNode('heading', { level: 1 }).run(),
  },
  {
    title: 'Heading 2',
    keywords: ['h2', 'heading', 'title'],
    command: ({ editor, range }) => editor.chain().focus().deleteRange(range).setNode('heading', { level: 2 }).run(),
  },
  {
    title: 'Heading 3',
    keywords: ['h3', 'heading', 'title'],
    command: ({ editor, range }) => editor.chain().focus().deleteRange(range).setNode('heading', { level: 3 }).run(),
  },
  {
    title: 'Bullet list',
    keywords: ['bullet', 'unordered', 'list'],
    command: ({ editor, range }) => editor.chain().focus().deleteRange(range).toggleBulletList().run(),
  },
  {
    title: 'Numbered list',
    keywords: ['ordered', 'number', 'list'],
    command: ({ editor, range }) => editor.chain().focus().deleteRange(range).toggleOrderedList().run(),
  },
  {
    title: 'Checklist',
    keywords: ['task', 'todo', 'check'],
    command: ({ editor, range }) => editor.chain().focus().deleteRange(range).toggleTaskList().run(),
  },
  {
    title: 'Quote',
    keywords: ['quote', 'blockquote'],
    command: ({ editor, range }) => editor.chain().focus().deleteRange(range).toggleBlockquote().run(),
  },
  {
    title: 'Code block',
    keywords: ['code', 'codeblock'],
    command: ({ editor, range }) => editor.chain().focus().deleteRange(range).toggleCodeBlock().run(),
  },
  {
    title: 'Divider',
    keywords: ['divider', 'hr', 'rule'],
    command: ({ editor, range }) => editor.chain().focus().deleteRange(range).setHorizontalRule().run(),
  },
  {
    title: 'Table',
    keywords: ['table', 'grid'],
    command: ({ editor, range }) =>
      editor.chain().focus().deleteRange(range).insertTable({ rows: 3, cols: 3, withHeaderRow: true }).run(),
  },
  {
    title: 'Image',
    keywords: ['image', 'picture', 'photo'],
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
            window.alert('Image upload failed.');
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
      empty.textContent = 'No results';
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
