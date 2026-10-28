// Extra visual evidence for the awwwards review. Runs in its own test browser.
import { chromium } from 'playwright';
import { mkdir, writeFile } from 'node:fs/promises';
import { fileURLToPath } from 'node:url';
const output = new URL('../../.shots/pass3/transitions/', import.meta.url);
await mkdir(output, { recursive: true });
const browser = await chromium.launch({ headless: true });
try {
  const page = await browser.newPage({ viewport: { width: 1440, height: 1000 } });
  await page.goto('http://127.0.0.1:4403/fairenough/');
  await page.waitForLoadState('networkidle');
  const samples = []; const started = Date.now();
  const click = page.getByRole('link', { name: 'Read the guide', exact: true }).click({ noWaitAfter: true });
  for (let index = 0; index < 10; index++) {
    await page.waitForTimeout(20);
    const style = await page.locator('#main').evaluate(el => ({ opacity: getComputedStyle(el).opacity, transform: getComputedStyle(el).transform, heading: el.querySelector('h1')?.textContent }));
    samples.push({ milliseconds: Date.now() - started, ...style });
    await page.screenshot({ path: fileURLToPath(new URL(`frame-${String(index).padStart(2,'0')}.png`, output)) });
  }
  await click;
  await writeFile(new URL('samples.json',output), JSON.stringify(samples,null,2));
  console.log(JSON.stringify(samples));
} finally { await browser.close(); }
