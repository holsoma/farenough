"""Generate the original 20-second score and UI cues, without external audio."""
from array import array
from pathlib import Path
import math
import wave

RATE = 48000
DURATION = 20
samples = [0.0] * (RATE * DURATION)

def tone(start, duration, frequency, amplitude, decay=2.5):
    first = round(start * RATE)
    length = round(duration * RATE)
    for i in range(min(length, len(samples) - first)):
        t = i / RATE
        attack = min(1.0, t / .012)
        tail = min(1.0, (duration - t) / .08)
        envelope = attack * tail * math.exp(-decay * t / duration)
        signal = math.sin(2 * math.pi * frequency * t) + .18 * math.sin(4 * math.pi * frequency * t)
        samples[first + i] += amplitude * envelope * signal

# Four quiet chords and a closing resolution; no copyrighted recording is used.
for start, notes in [(0, (130.81, 196, 329.63)), (4, (146.83, 220, 349.23)),
                     (8, (164.81, 246.94, 392)), (12, (130.81, 196, 329.63)),
                     (16, (130.81, 196, 261.63))]:
    for note in notes:
        tone(start, min(4, DURATION - start), note, .035, decay=1.5)

# Counter clicks follow the linear count-up exactly.
for trip in range(1, 27):
    tone(3.2 + .4 + trip * 2 / 26, .075, 700 + trip * 12, .075, decay=5)

for start in (3.2, 6.4, 10.4, 13.2, 16.8):
    tone(start, .28, 196, .07, decay=4)
    tone(start + .06, .2, 392, .035, decay=4)

for index, note in enumerate((523.25, 659.25, 783.99)):
    tone(8.5 + index * .1, .7, note, .095, decay=3)

peak = max(abs(value) for value in samples)
gain = .65 / peak
pcm = array('h')
for i, value in enumerate(samples):
    fade = min(1.0, i / (RATE * .15), (len(samples) - 1 - i) / (RATE * .55))
    pcm.append(round(value * gain * fade * 32767))

destination = Path(__file__).resolve().parents[1] / 'public' / 'promo-sound.wav'
with wave.open(str(destination), 'wb') as output:
    output.setnchannels(1)
    output.setsampwidth(2)
    output.setframerate(RATE)
    output.writeframes(pcm.tobytes())
print(f'Generated {destination.name}: {DURATION}s, {RATE}Hz, peak {peak * gain:.3f}')
