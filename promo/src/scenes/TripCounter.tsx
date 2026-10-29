import {
  Easing,
  Interactive,
  interpolate,
  useCurrentFrame,
  useVideoConfig,
} from "remotion";
import { Frame } from "../theme";

const TripCounterInner = () => {
  const frame = useCurrentFrame();
  const { fps } = useVideoConfig();
  const trips = Math.floor(
    interpolate(frame, [0.4 * fps, 2.4 * fps], [0, 26], {
      extrapolateLeft: "clamp",
      extrapolateRight: "clamp",
    }),
  );
  return (
    <Frame dark label="YOUR MONTH, ONE JOURNEY AT A TIME">
      <Interactive.Div
        name="Trip headline"
        premountFor={fps}
        style={{
          position: "absolute",
          left: 140,
          top: 265,
          fontSize: 153,
          fontWeight: 600,
          lineHeight: 1.04,
          letterSpacing: -7,
          translate: interpolate(
            frame,
            [0, 0.65 * fps],
            ["0px 55px", "0px 0px"],
            {
              extrapolateRight: "clamp",
              easing: Easing.bezier(0.16, 1, 0.3, 1),
            },
          ),
        }}
      >
        Every trip
        <br />
        <span style={{ color: "#94ded3" }}>adds up.</span>
      </Interactive.Div>
      <Interactive.Div
        name="Animated trip total"
        premountFor={fps}
        style={{
          position: "absolute",
          right: 150,
          top: 215,
          width: 670,
          textAlign: "center",
          fontVariantNumeric: "tabular-nums",
        }}
      >
        <div
          style={{
            fontSize: 360,
            lineHeight: 1,
            letterSpacing: -18,
            fontWeight: 650,
            color: "#e8b339",
          }}
        >
          {trips.toString().padStart(2, "0")}
        </div>
        <div style={{ fontSize: 43, marginTop: 22 }}>trips recorded</div>
      </Interactive.Div>
      <div
        style={{
          position: "absolute",
          left: 150,
          top: 775,
          width: 1620,
          display: "grid",
          gridTemplateColumns: "repeat(26, 1fr)",
          gap: 15,
        }}
      >
        {Array.from({ length: 26 }, (_, index) => (
          <div
            key={index}
            style={{
              height: 52,
              borderRadius: 26,
              backgroundColor: index < trips ? "#94ded3" : "#235450",
              border: index < trips ? "2px solid #94ded3" : "2px solid #47716d",
            }}
          />
        ))}
      </div>
      <div
        style={{
          position: "absolute",
          left: 150,
          right: 150,
          top: 890,
          display: "flex",
          justifyContent: "space-between",
          fontSize: 31,
          color: "#b5d4d0",
        }}
      >
        <span>Record your MRT and LRT journeys.</span>
        <span>Illustrative month</span>
      </div>
    </Frame>
  );
};

export const TripCounter = Interactive.create({
  Component: TripCounterInner,
  schema: {},
  layout: "absolute-fill",
});
