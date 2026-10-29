import { Audio } from "@remotion/media";
import { TransitionSeries, linearTiming } from "@remotion/transitions";
import { fade } from "@remotion/transitions/fade";
import { wipe } from "@remotion/transitions/wipe";
import { staticFile, useVideoConfig } from "remotion";
import { Opening } from "./scenes/Opening";
import { TripCounter } from "./scenes/TripCounter";
import { FareComparison } from "./scenes/FareComparison";
import { History } from "./scenes/History";
import { EndCard } from "./scenes/EndCard";

export const FarenoughPromo = () => {
  const { fps } = useVideoConfig();
  return (
    <>
      <TransitionSeries>
        <TransitionSeries.Sequence
          name="Has your pass paid for itself?"
          durationInFrames={3.6 * fps}
          premountFor={fps}
        >
          <Opening premountFor={fps} />
        </TransitionSeries.Sequence>
        <TransitionSeries.Transition
          presentation={wipe({ direction: "from-right" })}
          timing={linearTiming({ durationInFrames: 0.4 * fps })}
        />
        <TransitionSeries.Sequence
          name="26 trips"
          durationInFrames={3.6 * fps}
          premountFor={fps}
        >
          <TripCounter premountFor={fps} />
        </TransitionSeries.Sequence>
        <TransitionSeries.Transition
          presentation={wipe({ direction: "from-right" })}
          timing={linearTiming({ durationInFrames: 0.4 * fps })}
        />
        <TransitionSeries.Sequence
          name="Fares versus pass price"
          durationInFrames={4.4 * fps}
          premountFor={fps}
        >
          <FareComparison premountFor={fps} />
        </TransitionSeries.Sequence>
        <TransitionSeries.Transition
          presentation={fade()}
          timing={linearTiming({ durationInFrames: 0.4 * fps })}
        />
        <TransitionSeries.Sequence
          name="Month comparison and busiest months"
          durationInFrames={6.8 * fps}
          premountFor={fps}
        >
          <History premountFor={fps} />
        </TransitionSeries.Sequence>
        <TransitionSeries.Transition
          presentation={wipe({ direction: "from-right" })}
          timing={linearTiming({ durationInFrames: 0.4 * fps })}
        />
        <TransitionSeries.Sequence
          name="Download farenough"
          durationInFrames={3.2 * fps}
          premountFor={fps}
        >
          <EndCard premountFor={fps} />
        </TransitionSeries.Sequence>
      </TransitionSeries>
      <Audio
        name="Original sound cues"
        src={staticFile("promo-sound.wav")}
        premountFor={fps}
        volume={0.75}
      />
    </>
  );
};
