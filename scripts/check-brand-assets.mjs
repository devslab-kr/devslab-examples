import { createHash } from 'node:crypto';
import { execFileSync } from 'node:child_process';
import { existsSync, readFileSync } from 'node:fs';
import { resolve } from 'node:path';

const root = resolve(import.meta.dirname, '..');
const expectedHashes = new Map([
  ['.github/assets/readme-header.png', '47a942ffe3633979b805495dc3f27eef10d243ff278c72f1061152eac8a7f078'],
  ['.github/assets/social-preview.png', 'e9bc6856c557ee4ede2db139b6b40cd754d1e90ebf63b25dc6f02f546cfbd151'],
  ['.github/assets/collection-mark.svg', 'f2370535bb544213ecc0648d82298cd83767388a1c0ac991b907e4a778a03287'],
  ['.github/assets/collection-lockup.svg', 'eaac691c43dd97c4033b966e27272a02d1dc06a5c0b01b95ec8483f6c2ff8bad'],
]);

const read = (file) => readFileSync(resolve(root, file), 'utf8');
const assert = (condition, message) => {
  if (!condition) throw new Error(message);
};
const changedFiles = () => execFileSync('git', ['diff', '--name-only', 'origin/main'], { cwd: root, encoding: 'utf8' })
  .trim()
  .split(/\r?\n/)
  .filter(Boolean);

for (const [file, expected] of expectedHashes) {
  const absolute = resolve(root, file);
  assert(existsSync(absolute), `missing required O12 collection asset: ${file}`);
  const actual = createHash('sha256').update(readFileSync(absolute)).digest('hex');
  assert(actual === expected, `${file} has ${actual}; expected oss-brand v0.1.1 hash ${expected}`);
}

const mark = read('.github/assets/collection-mark.svg');
const lockup = read('.github/assets/collection-lockup.svg');
assert(mark.includes('data-oss-project="O12"'), 'collection mark must identify the O12 asset');
assert(mark.includes('M10 6H6V26H10') && mark.includes('M14 11L21 16L14 21Z'), 'collection mark must retain O12 bracket-and-play geometry');
assert(lockup.includes('data-oss-lockup="O12"') && lockup.includes('Open source by DevsLab'), 'collection lockup must provide the O12 endorsement');

for (const [file, endorsement] of [
  ['README.md', 'Open source by [DevsLab](https://devslab.kr/)'],
  ['README.ko.md', '[DevsLab](https://devslab.kr/) 오픈소스'],
]) {
  const content = read(file);
  assert(content.includes('.github/assets/readme-header.png'), `${file} must use the local O12 README header`);
  assert(content.includes('https://devslab.kr/brand/open-source/'), `${file} must link to the canonical OSS brand guide`);
  assert(content.includes(endorsement), `${file} must include its localized collection endorsement`);
}

const rootOnly = new Set([
  '.github/workflows/ci.yml',
  '.github/assets/readme-header.png',
  '.github/assets/social-preview.png',
  '.github/assets/collection-mark.svg',
  '.github/assets/collection-lockup.svg',
  'README.md',
  'README.ko.md',
  'scripts/check-brand-assets.mjs',
]);
for (const file of changedFiles()) {
  assert(rootOnly.has(file), `O12 is collection-level only; unexpected changed path: ${file}`);
}

const ci = read('.github/workflows/ci.yml');
const brandStart = ci.indexOf('  brand:\n');
assert(brandStart >= 0, 'CI must include a dedicated O12 brand job');
const nextJobPattern = /\n  [A-Za-z][A-Za-z0-9_-]*:\n/g;
nextJobPattern.lastIndex = brandStart + '  brand:\n'.length;
const nextJob = nextJobPattern.exec(ci);
const brandJob = ci.slice(brandStart, nextJob?.index ?? ci.length);
assert(brandJob.includes("node-version: '22'"), 'dedicated O12 brand job must use Node 22');
assert(brandJob.includes('run: node scripts/check-brand-assets.mjs'), 'dedicated O12 brand job must run the collection checker');
assert(!brandJob.includes('needs:'), 'dedicated O12 brand job must not depend on the demo matrix');

console.log(`O12 collection brand contract passed (${expectedHashes.size} exact assets, no per-demo changes).`);
