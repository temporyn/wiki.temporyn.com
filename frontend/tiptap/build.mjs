import esbuild from 'esbuild';
import { readFileSync, mkdirSync } from 'node:fs';
import { fileURLToPath } from 'node:url';
import { dirname, resolve } from 'node:path';

const here = dirname(fileURLToPath(import.meta.url));
const version = JSON.parse(
  readFileSync(resolve(here, 'node_modules/@tiptap/core/package.json'), 'utf8')
).version;

const outDir = resolve(here, '../../src/main/resources/static/lib/tiptap', version);
mkdirSync(outDir, { recursive: true });

await esbuild.build({
  entryPoints: [resolve(here, 'src/index.js')],
  bundle: true,
  format: 'iife',
  target: ['es2019'],
  minify: true,
  sourcemap: false,
  outfile: resolve(outDir, 'tiptap.bundle.js'),
  loader: { '.css': 'css' },
  logLevel: 'info',
});

console.log(`TipTap bundle written to static/lib/tiptap/${version}/`);
