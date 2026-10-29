import assert from 'node:assert/strict';
import {spawnSync} from 'node:child_process';
import {readFileSync, writeFileSync} from 'node:fs';
import {createHash} from 'node:crypto';
import {fileURLToPath} from 'node:url';
import path from 'node:path';

const project = fileURLToPath(new URL('../', import.meta.url));
const cli = path.join(project, 'node_modules/@remotion/cli/remotion-cli.js');
const file = 'out/farenough-promo-20s.mp4';
const run = (args) => {
  const result = spawnSync(process.execPath, [cli, ...args], {cwd: project, maxBuffer: 16 * 1024 * 1024});
  if (result.error) throw result.error;
  assert.equal(result.status, 0, result.stderr.toString());
  return result.stdout;
};

const metadata = JSON.parse(run(['ffprobe', '-v', 'error', '-count_frames',
  '-show_entries', 'format=duration,size:stream=codec_name,width,height,r_frame_rate,nb_read_frames,duration,channels,sample_rate',
  '-of', 'json', file]).toString());
const video = metadata.streams.find((stream) => stream.codec_name === 'h264');
const audio = metadata.streams.find((stream) => stream.codec_name === 'aac');
assert.ok(video && audio);
assert.equal(Number(metadata.format.duration), 20);
assert.equal(Number(video.duration), 20);
assert.equal(video.width, 1920);
assert.equal(video.height, 1080);
assert.equal(video.r_frame_rate, '30/1');
assert.equal(Number(video.nb_read_frames), 600);
assert.equal(Number(audio.sample_rate), 48000);

const wav = run(['ffmpeg', '-v', 'error', '-xerror', '-i', file, '-vn', '-ac', '1',
  '-ar', '48000', '-c:a', 'pcm_s16le', '-f', 'wav', 'pipe:1']);
assert.equal(wav.toString('ascii', 0, 4), 'RIFF');
let pcm;
for (let offset = 12; offset + 8 <= wav.length;) {
  const length = wav.readUInt32LE(offset + 4);
  if (wav.toString('ascii', offset, offset + 4) === 'data') {
    pcm = wav.subarray(offset + 8, Math.min(wav.length, offset + 8 + length));
    break;
  }
  offset += 8 + length + (length % 2);
}
assert.ok(pcm && pcm.length > 0, 'Decoded audio samples are missing');
let peak = 0;
let squareSum = 0;
for (let i = 0; i < pcm.length; i += 2) {
  const value = pcm.readInt16LE(i) / 32768;
  peak = Math.max(peak, Math.abs(value));
  squareSum += value * value;
}
const rms = Math.sqrt(squareSum / (pcm.length / 2));
assert.ok(peak > .05 && peak < .99, `Unexpected audio peak: ${peak}`);
assert.ok(rms > .005, `Unexpected audio RMS: ${rms}`);
const report = {metadata, decodedVideoFrames: 600, audioPeakDb: 20 * Math.log10(peak),
  audioRmsDb: 20 * Math.log10(rms), sha256: createHash('sha256').update(readFileSync(path.join(project, file))).digest('hex')};
writeFileSync(path.join(project, 'out/verification.json'), JSON.stringify(report, null, 2) + '\n');
console.log(`PASS: 20.000 seconds, 1920x1080, 30 fps, 600 decoded frames, H.264/AAC; audio peak ${report.audioPeakDb.toFixed(1)} dBFS.`);
