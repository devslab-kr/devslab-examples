import assert from 'node:assert/strict';
import { spawnSync } from 'node:child_process';
import { cpSync, mkdirSync, mkdtempSync, readFileSync, rmSync, writeFileSync } from 'node:fs';
import { tmpdir } from 'node:os';
import { dirname, join, resolve } from 'node:path';
import { test } from 'node:test';

const root = resolve(import.meta.dirname, '..');
const assets = ['readme-header.png', 'social-preview.png', 'collection-mark.svg', 'collection-lockup.svg'];

function check(t, mutate = () => {}) {
  const fixture = mkdtempSync(join(tmpdir(), 'brand-contract-'));
  t.after(() => rmSync(fixture, { recursive: true, force: true }));
  for (const file of [
    'scripts/check-brand-assets.mjs', '.github/workflows/ci.yml', 'README.md', 'README.ko.md',
    ...assets.map(asset => `.github/assets/${asset}`),
  ]) {
    const target = join(fixture, file);
    mkdirSync(dirname(target), { recursive: true });
    cpSync(join(root, file), target);
  }
  mutate(fixture);
  return spawnSync(process.execPath, [join(fixture, 'scripts/check-brand-assets.mjs')], { encoding: 'utf8' });
}

test('ordinary demo maintenance passes without Git or an origin/main diff', t => {
  const result = check(t, fixture => {
    mkdirSync(join(fixture, 'example-demo'));
    writeFileSync(join(fixture, 'example-demo/build.gradle.kts'), '// ordinary maintenance\n');
  });
  assert.equal(result.status, 0, result.stderr);
  assert.match(result.stdout, /4 exact assets/);
});

for (const asset of assets) {
  for (const mode of ['missing', 'modified']) {
    test(`${mode} ${asset} fails`, t => {
      const result = check(t, fixture => {
        const file = join(fixture, '.github/assets', asset);
        if (mode === 'missing') rmSync(file);
        else writeFileSync(file, Buffer.concat([readFileSync(file), Buffer.from('tampered')]));
      });
      assert.notEqual(result.status, 0);
      assert.match(result.stderr, mode === 'missing' ? /missing required O12/ : /expected oss-brand/);
    });
  }
}

for (const file of ['README.md', 'README.ko.md']) {
  for (const removed of ['.github/assets/readme-header.png', 'https://devslab.kr/brand/open-source/', 'Open source by']) {
    test(`${file} rejects removal of ${removed}`, t => {
      const result = check(t, fixture => {
        const path = join(fixture, file);
        writeFileSync(path, readFileSync(path, 'utf8').replaceAll(removed, 'removed'));
      });
      assert.notEqual(result.status, 0);
      assert.match(result.stderr, /must (use|link|include)/);
    });
  }
}

for (const [before, after, expected] of [
  ['  brand:\n', '  removed:\n', /dedicated O12 brand job/],
  ["node-version: '22'", "node-version: '20'", /must use Node 22/],
  ['fetch-depth: 0', 'fetch-depth: 1', /must fetch origin\/main/],
  ['run: node scripts/check-brand-assets.mjs', 'run: echo removed', /must run the collection checker/],
  ['  brand:\n', '  brand:\n    needs: detect\n', /must not depend/],
]) {
  test(`CI rejects ${after.trim()}`, t => {
    const result = check(t, fixture => {
      const path = join(fixture, '.github/workflows/ci.yml');
      const content = readFileSync(path, 'utf8').replace(/\r\n/g, '\n');
      writeFileSync(path, content.replace(before, after));
    });
    assert.notEqual(result.status, 0);
    assert.match(result.stderr, expected);
  });
}
