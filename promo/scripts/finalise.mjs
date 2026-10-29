import {spawnSync} from 'node:child_process';
import {fileURLToPath} from 'node:url';
import path from 'node:path';

const project = fileURLToPath(new URL('../', import.meta.url));
const cli = path.join(project, 'node_modules/@remotion/cli/remotion-cli.js');
// Preserve the 600 rendered video frames and trim AAC padding to exactly 20 s.
const result = spawnSync(process.execPath, [cli, 'ffmpeg', '-hide_banner', '-y',
  '-i', 'out/farenough-master.mp4', '-t', '20', '-c:v', 'copy', '-c:a', 'aac',
  '-b:a', '192k', '-movflags', '+faststart', 'out/farenough-promo-20s.mp4'],
  {cwd: project, stdio: 'inherit'});
if (result.error) throw result.error;
process.exitCode = result.status ?? 1;
