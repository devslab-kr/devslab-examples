import { createHash } from 'node:crypto';
import { execFileSync } from 'node:child_process';
import { existsSync, readFileSync } from 'node:fs';
import { resolve } from 'node:path';

const root = resolve(import.meta.dirname, '..');
const expectedHashes = new Map([
  ['.github/assets/readme-header.png', '36e94ca0b5553c07e67ded933fbcb17e2a04f64223026c065b69bc2c72a60c10'],
  ['.github/assets/social-preview.png', 'c4cc40d3261368a76a5b80a794007037805baa887792fc99bf1418fcb3f401db'],
  ['.github/assets/collection-mark.svg', 'a0374b6e5b987dfdf34779459ff412d8ddb9d4b3ada1f672aabce4db0a71d166'],
  ['.github/assets/collection-lockup.svg', '71497e089061d07251c5f086ec1fc2c6fdfa75cdec38d0822f9e349d6b372121'],
]);

const read = (file) => readFileSync(resolve(root, file), 'utf8').replace(/\r\n/g, '\n');
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
  assert(actual === expected, `${file} has ${actual}; expected oss-brand v0.2.0 hash ${expected}`);
}

const mark = read('.github/assets/collection-mark.svg');
const lockup = read('.github/assets/collection-lockup.svg');
assert(mark.includes('data-oss-project="O12"'), 'collection mark must identify the O12 asset');
assert(mark.includes('data-layer="q-frame"') && mark.includes('<rect x="5" y="5" width="16" height="16" rx="2"') && mark.includes('<rect x="11" y="11" width="16" height="16" rx="2"'), 'collection mark must use the shared Q frame');
assert(mark.includes('M15 13V23') && mark.includes('M23 13V23') && mark.includes('M18 15L22 18L18 21Z'), 'collection mark must retain the O12 examples route');
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
assert(brandJob.includes('fetch-depth: 0'), 'dedicated O12 brand job must fetch origin/main for collection-only diff validation');
assert(brandJob.includes('run: node scripts/check-brand-assets.mjs'), 'dedicated O12 brand job must run the collection checker');
assert(!brandJob.includes('needs:'), 'dedicated O12 brand job must not depend on the demo matrix');

console.log(`O12 collection brand contract passed (${expectedHashes.size} exact assets, no per-demo changes).`);
