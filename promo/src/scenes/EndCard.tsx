import {
  Easing,
  Interactive,
  interpolate,
  useCurrentFrame,
  useVideoConfig,
} from "remotion";
import { Frame } from "../theme";

const EndCardInner = () => {
  const frame = useCurrentFrame();
  const { fps } = useVideoConfig();
  return (
    <Frame dark label="ANDROID · FREE AND OPEN SOURCE">
      <Interactive.Div
        name="Final brand"
        premountFor={fps}
        style={{
          position: "absolute",
          left: 140,
          top: 240,
          fontSize: 184,
          fontWeight: 650,
          letterSpacing: -10,
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
        farenough<span style={{ color: "#e8b339" }}>.</span>
      </Interactive.Div>
      <Interactive.Div
        name="Closing line"
        premountFor={fps}
        style={{
          position: "absolute",
          left: 150,
          top: 485,
          fontSize: 70,
          color: "#b6e5de",
        }}
      >
        Make every journey count.
      </Interactive.Div>
      <Interactive.Div
        name="Download action"
        premountFor={fps}
        style={{
          position: "absolute",
          left: 150,
          top: 655,
          padding: "30px 42px",
          backgroundColor: "#e8b339",
          color: "#18242b",
          borderRadius: 16,
          fontSize: 45,
          fontWeight: 600,
          opacity: interpolate(frame, [0.2 * fps, 0.65 * fps], [0, 1], {
            extrapolateLeft: "clamp",
            extrapolateRight: "clamp",
          }),
        }}
      >
        Download the Android APK <span style={{ marginLeft: 50 }}>↗</span>
      </Interactive.Div>
      <div
        style={{
          position: "absolute",
          left: 150,
          top: 865,
          fontSize: 38,
          color: "#d6eeea",
        }}
      >
        holsoma.github.io/farenough
      </div>
      <svg
        width="390"
        height="430"
        viewBox="0 0 390 430"
        style={{ position: "absolute", right: 160, top: 350 }}
      >
        <path
          d="M 50 360 V 110 Q 50 55 105 55 H 330 M 50 195 H 260"
          fill="none"
          stroke="#78bdb4"
          strokeWidth="32"
          strokeLinecap="round"
          pathLength="1"
          strokeDasharray="1"
          strokeDashoffset={interpolate(
            frame,
            [0.15 * fps, 1.2 * fps],
            [1, 0],
            { extrapolateLeft: "clamp", extrapolateRight: "clamp" },
          )}
        />
        <circle cx="330" cy="55" r="34" fill="#e8b339" />
      </svg>
    </Frame>
  );
};

export const EndCard = Interactive.create({
  Component: EndCardInner,
  schema: {},
  layout: "absolute-fill",
});
