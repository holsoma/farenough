import { readFile, writeFile, mkdir, copyFile } from 'node:fs/promises';
import { fileURLToPath } from 'node:url';
import path from 'node:path';

const root = path.resolve(path.dirname(fileURLToPath(import.meta.url)), '..');
const repo = process.env.GITHUB_REPOSITORY || 'holsoma/fairenough';
if (!/^[\w.-]+\/[\w.-]+$/.test(repo)) throw new Error('Invalid repository name');
const headers = { Accept: 'application/vnd.github+json' };
if (process.env.GH_TOKEN) headers.Authorization = `Bearer ${process.env.GH_TOKEN}`;
const response = await fetch(`https://api.github.com/repos/${repo}/releases/latest`, { headers });
if (response.status !== 404 && !response.ok) throw new Error(`GitHub release lookup failed: ${response.status}`);
if (response.ok) {
  const release = await response.json();
  if (!/^v\d+\.\d+\.\d+$/.test(release.tag_name)) throw new Error('Expected a stable version tag');
  const apk = release.assets.find(a => a.name === `fairenough-${release.tag_name}.apk`);
  if (!apk) throw new Error('The published release has no expected APK');
  const metadata = { version: release.tag_name.slice(1), date: release.published_at.slice(0, 10), published: true, url: release.html_url, apk: apk.browser_download_url, minimumAndroid: '8.0' };
  const file = path.join(root, 'site/src/data/release.ts');
  const content = await readFile(file, 'utf8');
  await writeFile(file, content.replace(/export const release = [\s\S]*?;\n/, `export const release = ${JSON.stringify(metadata, null, 2)};\n`));
}
await mkdir(path.join(root, 'site/public/schemas'), { recursive: true });
await copyFile(path.join(root, 'schemas/trips-v1.schema.json'), path.join(root, 'site/public/schemas/trips-v1.schema.json'));
