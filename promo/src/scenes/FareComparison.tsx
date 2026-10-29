import {
  Easing,
  Interactive,
  interpolate,
  useCurrentFrame,
  useVideoConfig,
} from "remotion";
import { Frame, sgd } from "../theme";

const FareComparisonInner = () => {
  const frame = useCurrentFrame();
  const { fps } = useVideoConfig();
  const fare = Math.round(
    interpolate(frame, [0.35 * fps, 2.1 * fps], [0, 5720], {
      extrapolateLeft: "clamp",
      extrapolateRight: "clamp",
    }),
  );
  const savings = Math.max(0, fare - 4800);
  return (
    <Frame label="26 JOURNEYS · ONE EXAMPLE MONTH">
      <Interactive.Div
        name="Comparison headline"
        premountFor={fps}
        style={{
          position: "absolute",
          left: 140,
          top: 230,
          fontSize: 122,
          fontWeight: 650,
          letterSpacing: -6,
          translate: interpolate(
            frame,
            [0, 0.6 * fps],
            ["0px 40px", "0px 0px"],
            {
              extrapolateRight: "clamp",
              easing: Easing.bezier(0.16, 1, 0.3, 1),
            },
          ),
        }}
      >
        See when it pays off.
      </Interactive.Div>
      <div style={{ position: "absolute", left: 150, top: 455, width: 990 }}>
        <div
          style={{
            display: "flex",
            justifyContent: "space-between",
            alignItems: "baseline",
          }}
        >
          <span style={{ fontSize: 39 }}>Recorded fares</span>
          <span
            style={{
              fontSize: 71,
              fontWeight: 650,
              letterSpacing: -3,
              fontVariantNumeric: "tabular-nums",
              color: "#006c67",
            }}
          >
            {sgd(fare)}
          </span>
        </div>
        <div
          style={{
            height: 55,
            backgroundColor: "#e3eaed",
            borderRadius: 30,
            marginTop: 22,
          }}
        >
          <div
            style={{
              width: `${fare / 60}%`,
              height: 55,
              backgroundColor: "#006c67",
              borderRadius: 30,
            }}
          />
        </div>
        <div
          style={{
            display: "flex",
            justifyContent: "space-between",
            alignItems: "baseline",
            marginTop: 65,
          }}
        >
          <span style={{ fontSize: 39 }}>Pass price</span>
          <span style={{ fontSize: 71, fontWeight: 650, letterSpacing: -3 }}>
            S$48.00
          </span>
        </div>
        <div
          style={{
            height: 55,
            backgroundColor: "#e3eaed",
            borderRadius: 30,
            marginTop: 22,
          }}
        >
          <div
            style={{
              width: "80%",
              height: 55,
              backgroundColor: "#e8b339",
              borderRadius: 30,
            }}
          />
        </div>
      </div>
      <div
        style={{
          position: "absolute",
          left: 1240,
          top: 450,
          height: 350,
          borderLeft: "2px solid #cbd7db",
        }}
      />
      <Interactive.Div
        name="Savings result"
        premountFor={fps}
        style={{
          position: "absolute",
          left: 1310,
          top: 455,
          width: 470,
          opacity: interpolate(frame, [1.85 * fps, 2.15 * fps], [0, 1], {
            extrapolateLeft: "clamp",
            extrapolateRight: "clamp",
          }),
          translate: interpolate(
            frame,
            [1.85 * fps, 2.3 * fps],
            ["0px 35px", "0px 0px"],
            {
              extrapolateLeft: "clamp",
              extrapolateRight: "clamp",
              easing: Easing.bezier(0.16, 1, 0.3, 1),
            },
          ),
        }}
      >
        <div style={{ fontSize: 36, color: "#52616b" }}>Estimated savings</div>
        <div
          style={{
            fontSize: 118,
            fontWeight: 650,
            color: "#006c67",
            letterSpacing: -6,
            marginTop: 30,
            fontVariantNumeric: "tabular-nums",
          }}
        >
          {sgd(savings)}
        </div>
        <div
          style={{
            fontSize: 34,
            marginTop: 35,
            padding: "16px 23px",
            borderRadius: 12,
            backgroundColor: "#ffe6a4",
            display: "inline-block",
          }}
        >
          Pass cost covered ✓
        </div>
      </Interactive.Div>
      <div
        style={{
          position: "absolute",
          left: 150,
          top: 925,
          fontSize: 30,
          color: "#52616b",
        }}
      >
        Example figures. Actual fares depend on your journeys and discounts.
      </div>
    </Frame>
  );
};

export const FareComparison = Interactive.create({
  Component: FareComparisonInner,
  schema: {},
  layout: "absolute-fill",
});
