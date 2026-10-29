import { Composition, Folder } from "remotion";
import { FarenoughPromo } from "./Promo";
import { Opening } from "./scenes/Opening";
import { TripCounter } from "./scenes/TripCounter";
import { FareComparison } from "./scenes/FareComparison";
import { History } from "./scenes/History";
import { EndCard } from "./scenes/EndCard";

export const RemotionRoot: React.FC = () => {
  return (
    <>
      <Composition
        id="FarenoughPromo"
        component={FarenoughPromo}
        width={1920}
        height={1080}
        fps={30}
        durationInFrames={600}
      />
      <Folder name="Scenes">
        <Composition
          id="Opening"
          component={Opening}
          width={1920}
          height={1080}
          fps={30}
          durationInFrames={108}
        />
        <Composition
          id="TripCounter"
          component={TripCounter}
          width={1920}
          height={1080}
          fps={30}
          durationInFrames={108}
        />
        <Composition
          id="FareComparison"
          component={FareComparison}
          width={1920}
          height={1080}
          fps={30}
          durationInFrames={132}
        />
        <Composition
          id="History"
          component={History}
          width={1920}
          height={1080}
          fps={30}
          durationInFrames={204}
        />
        <Composition
          id="EndCard"
          component={EndCard}
          width={1920}
          height={1080}
          fps={30}
          durationInFrames={96}
        />
      </Folder>
    </>
  );
};
