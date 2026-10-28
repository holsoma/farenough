import test from 'node:test';
import assert from 'node:assert/strict';
import { createHash } from 'node:crypto';
import { spawnSync } from 'node:child_process';
import { mkdtempSync, mkdirSync, writeFileSync, readFileSync, existsSync, realpathSync, rmSync } from 'node:fs';
import os from 'node:os';
import path from 'node:path';
import { fileURLToPath } from 'node:url';

const script = fileURLToPath(new URL('./stage-release.sh', import.meta.url)).replaceAll('\\', '/');
const bash = process.env.FARENOUGH_TEST_BASH || 'bash';
const identity = 'a'.repeat(64);
function fixture(t, state = 'true') {
  const root = mkdtempSync(path.join(os.tmpdir(), 'farenough-release-test-'));
  const bin = path.join(root, 'bin'); mkdirSync(bin); mkdirSync(path.join(root, 'package'));
  const log = path.join(root, 'calls.txt');
  const apk = 'Synthetic fixture bytes, not an APK';
  writeFileSync(path.join(root, 'SIGNING.md'), `${identity}\n`);
  writeFileSync(path.join(root, 'package/farenough-v0.1.0.apk'), apk);
  writeFileSync(path.join(root, 'package/SHA256SUMS.txt'), `${createHash('sha256').update(apk).digest('hex')}  farenough-v0.1.0.apk\n`);
  writeFileSync(path.join(root, 'package/signing-certificate.txt'), `Signer #1 certificate SHA-256 digest: ${identity}\n`);
  writeFileSync(path.join(root, 'package/release-notes.md'), 'Test release notes\n');
  writeFileSync(path.join(bin, 'gh'), `#!/usr/bin/env bash
printf '%s\\n' "$*" >> "$MOCK_LOG"
if [[ "$1" == api ]]; then
  [[ "$MOCK_API_ERROR" != 1 ]] || exit 1
  printf '%s' "$MOCK_STATE"
fi
`, { mode: 0o755 });
  const env = { ...process.env };
  for (const key of Object.keys(env)) if (key.toUpperCase() === 'PATH') delete env[key];
  Object.assign(env, { PATH: [bin, process.env.PATH || process.env.Path].join(path.delimiter), MOCK_LOG: log.replaceAll('\\', '/'), MOCK_STATE: state, MOCK_API_ERROR: '0', GITHUB_REPOSITORY: 'example/farenough', GITHUB_REF_NAME: 'v0.1.0' });
  t.after(() => {
    const resolved = realpathSync(root);
    assert.equal(path.dirname(resolved), realpathSync(os.tmpdir()));
    assert.ok(path.basename(resolved).startsWith('farenough-release-test-'));
    rmSync(resolved, { recursive: true });
  });
  return { root, env, run: mode => spawnSync(bash, [script, 'package', ...(mode ? [mode] : [])], { cwd: root, env, encoding: 'utf8' }), calls: () => existsSync(log) ? readFileSync(log, 'utf8').trim().split('\n') : [] };
}

test('published release assets are never changed', t => {
  const f = fixture(t, 'false'); const r = f.run();
  assert.equal(r.status, 0, r.stderr); assert.equal(f.calls().length, 1); assert.match(r.stdout, /Existing assets are retained/);
});
test('an existing draft is refreshed without creating or publishing a release', t => {
  const f = fixture(t); const r = f.run();
  assert.equal(r.status, 0, r.stderr); const calls = f.calls();
  assert.equal(calls.length, 3); assert.match(calls[1], /^release upload /); assert.match(calls[2], /^release edit /);
  assert.ok(calls.every(c => !c.includes('--draft') && !c.includes('release create')));
});
test('a missing release is created as a draft against an existing tag', t => {
  const f = fixture(t, ''); const r = f.run();
  assert.equal(r.status, 0, r.stderr); assert.equal(f.calls().length, 2); assert.match(f.calls()[1], /^release create .* --verify-tag --draft /);
});
test('an API failure cannot be mistaken for a missing release', t => {
  const f = fixture(t); f.env.MOCK_API_ERROR = '1'; const r = f.run();
  assert.notEqual(r.status, 0); assert.equal(f.calls().length, 1);
});
test('an unrecognised signing certificate prevents every GitHub call', t => {
  const f = fixture(t); writeFileSync(path.join(f.root, 'package/signing-certificate.txt'), `Signer #1 certificate SHA-256 digest: ${'b'.repeat(64)}\n`);
  const r = f.run(); assert.notEqual(r.status, 0); assert.deepEqual(f.calls(), []); assert.match(r.stderr, /certificate does not match/);
});
test('a modified package prevents every GitHub call', t => {
  const f = fixture(t); writeFileSync(path.join(f.root, 'package/farenough-v0.1.0.apk'), 'Changed bytes');
  const r = f.run(); assert.notEqual(r.status, 0); assert.deepEqual(f.calls(), []);
});
test('a checksum for a different file cannot validate this APK', t => {
  const f = fixture(t);
  const sum = readFileSync(path.join(f.root, 'package/SHA256SUMS.txt'), 'utf8').replace('farenough-v0.1.0.apk', 'another.apk');
  writeFileSync(path.join(f.root, 'package/SHA256SUMS.txt'), sum);
  const r = f.run(); assert.notEqual(r.status, 0); assert.deepEqual(f.calls(), []);
});
test('an additional signing certificate prevents every GitHub call', t => {
  const f = fixture(t);
  writeFileSync(path.join(f.root, 'package/signing-certificate.txt'), `Signer #1 certificate SHA-256 digest: ${identity}\nSigner #2 certificate SHA-256 digest: ${'b'.repeat(64)}\n`);
  const r = f.run(); assert.notEqual(r.status, 0); assert.deepEqual(f.calls(), []);
});
test('dry-run verifies and reads the draft without changing it', t => {
  const f = fixture(t); const r = f.run('--dry-run');
  assert.equal(r.status, 0, r.stderr); assert.equal(f.calls().length, 1); assert.match(r.stdout, /Would refresh/);
});
