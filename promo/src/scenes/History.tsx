import {
  CanvasImage,
  Easing,
  Interactive,
  interpolate,
  staticFile,
  useCurrentFrame,
  useVideoConfig,
} from "remotion";
import { Frame } from "../theme";

const HistoryInner = () => {
  const frame = useCurrentFrame();
  const { fps } = useVideoConfig();
  return (
    <Frame label="COMPARE MONTHS · FIND BUSIER PERIODS">
      <Interactive.Div
        name="Monthly comparison headline"
        premountFor={fps}
        style={{
          position: "absolute",
          left: 140,
          top: 280,
          fontSize: 150,
          fontWeight: 650,
          lineHeight: 1.04,
          letterSpacing: -7,
          opacity: interpolate(frame, [2.6 * fps, 2.95 * fps], [1, 0], {
            extrapolateLeft: "clamp",
            extrapolateRight: "clamp",
          }),
          translate: interpolate(
            frame,
            [0, 0.65 * fps],
            ["0px 50px", "0px 0px"],
            {
              extrapolateRight: "clamp",
              easing: Easing.bezier(0.16, 1, 0.3, 1),
            },
          ),
        }}
      >
        Your months.
        <br />
        <span style={{ color: "#006c67" }}>Side by side.</span>
      </Interactive.Div>
      <Interactive.Div
        name="Month metrics"
        premountFor={fps}
        style={{
          position: "absolute",
          left: 148,
          top: 660,
          fontSize: 47,
          color: "#52616b",
          opacity: interpolate(
            frame,
            [0.25 * fps, 0.7 * fps, 2.6 * fps, 2.95 * fps],
            [0, 1, 1, 0],
            {
              extrapolateLeft: "clamp",
              extrapolateRight: "clamp",
            },
          ),
        }}
      >
        Fares. Journeys. Travel days.
      </Interactive.Div>
      <Interactive.Div
        name="Busiest months headline"
        premountFor={fps}
        style={{
          position: "absolute",
          left: 140,
          top: 220,
          fontSize: 138,
          fontWeight: 650,
          lineHeight: 1.04,
          letterSpacing: -6,
          opacity: interpolate(frame, [2.95 * fps, 3.25 * fps], [0, 1], {
            extrapolateLeft: "clamp",
            extrapolateRight: "clamp",
          }),
          translate: interpolate(
            frame,
            [2.95 * fps, 3.4 * fps],
            ["0px 30px", "0px 0px"],
            {
              extrapolateLeft: "clamp",
              extrapolateRight: "clamp",
              easing: Easing.bezier(0.16, 1, 0.3, 1),
            },
          ),
        }}
      >
        Spot your
        <br />
        <span style={{ color: "#006c67" }}>busiest months.</span>
      </Interactive.Div>
      <Interactive.Div
        name="Animated monthly journey chart"
        premountFor={fps}
        style={{
          position: "absolute",
          left: 150,
          top: 560,
          width: 1010,
          opacity: interpolate(frame, [3.1 * fps, 3.4 * fps], [0, 1], {
            extrapolateLeft: "clamp",
            extrapolateRight: "clamp",
          }),
        }}
      >
        <div style={{ fontSize: 31, color: "#52616b", marginBottom: 28 }}>
          Journeys per month
        </div>
        {[
          { month: "Aug 2026", trips: 22, peak: false },
          { month: "Sept 2026", trips: 32, peak: true },
          { month: "Oct 2026", trips: 26, peak: false },
        ].map((month) => (
          <div
            key={month.month}
            style={{
              display: "flex",
              alignItems: "center",
              gap: 24,
              marginBottom: 25,
              height: 55,
            }}
          >
            <div
              style={{
                width: 175,
                flexShrink: 0,
                fontSize: 34,
                fontWeight: month.peak ? 650 : 400,
              }}
            >
              {month.month}
            </div>
            <div style={{ width: 650, height: 32, flexShrink: 0 }}>
              <div
                style={{
                  height: 32,
                  borderRadius: 16,
                  backgroundColor: month.peak ? "#e8b339" : "#006c67",
                  width: interpolate(
                    frame,
                    [3.1 * fps, 4.15 * fps],
                    [0, (month.trips / 32) * 650],
                    {
                      extrapolateLeft: "clamp",
                      extrapolateRight: "clamp",
                      easing: Easing.bezier(0.16, 1, 0.3, 1),
                    },
                  ),
                }}
              />
            </div>
            <div
              style={{
                fontSize: 42,
                fontWeight: 650,
                fontVariantNumeric: "tabular-nums",
              }}
            >
              {month.trips}
            </div>
          </div>
        ))}
      </Interactive.Div>
      <Interactive.Div
        name="Phone changing from comparison to graphs"
        premountFor={fps}
        style={{
          position: "absolute",
          left: 1290,
          top: 175,
          width: 350,
          height: 778,
          borderRadius: 30,
          overflow: "hidden",
          border: "3px solid #c7d7d8",
          boxShadow: "0 20px 48px #063d3a20",
          translate: interpolate(
            frame,
            [0, 0.7 * fps],
            ["160px 30px", "0px 0px"],
            {
              extrapolateRight: "clamp",
              easing: Easing.bezier(0.16, 1, 0.3, 1),
            },
          ),
        }}
      >
        <CanvasImage
          name="Month comparison screenshot"
          src={staticFile("screenshots/month-comparison.png")}
          premountFor={fps}
          style={{
            position: "absolute",
            width: "100%",
            height: "100%",
            opacity: interpolate(frame, [2.8 * fps, 3.1 * fps], [1, 0], {
              extrapolateLeft: "clamp",
              extrapolateRight: "clamp",
            }),
          }}
        />
        <CanvasImage
          name="Actual month-history graph screen"
          src={staticFile("screenshots/month-history.png")}
          premountFor={fps}
          style={{
            position: "absolute",
            width: "100%",
            height: "100%",
            opacity: interpolate(frame, [2.8 * fps, 3.1 * fps], [0, 1], {
              extrapolateLeft: "clamp",
              extrapolateRight: "clamp",
            }),
          }}
        />
      </Interactive.Div>
      <div
        style={{
          position: "absolute",
          left: 150,
          top: 920,
          fontSize: 30,
          color: "#52616b",
        }}
      >
        Actual app screens. Figures use demonstration records.
      </div>
    </Frame>
  );
};

export const History = Interactive.create({
  Component: HistoryInner,
  schema: {},
  layout: "absolute-fill",
});
