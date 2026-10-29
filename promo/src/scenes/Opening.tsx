import {
  Easing,
  Interactive,
  interpolate,
  useCurrentFrame,
  useVideoConfig,
} from "remotion";
import { Frame } from "../theme";

const OpeningInner = () => {
  const frame = useCurrentFrame();
  const { fps } = useVideoConfig();
  return (
    <Frame>
      <Interactive.Div
        name="The pass question"
        premountFor={fps}
        style={{
          position: "absolute",
          left: 140,
          top: 265,
          width: 1050,
          fontSize: 146,
          fontWeight: 650,
          letterSpacing: -8,
          lineHeight: 1.03,
          opacity: interpolate(frame, [0, 0.55 * fps], [0, 1], {
            extrapolateRight: "clamp",
          }),
          translate: interpolate(
            frame,
            [0, 0.7 * fps],
            ["0px 45px", "0px 0px"],
            {
              extrapolateRight: "clamp",
              easing: Easing.bezier(0.16, 1, 0.3, 1),
            },
          ),
        }}
      >
        Has your pass
        <br />
        <span style={{ color: "#006c67" }}>paid for itself?</span>
      </Interactive.Div>
      <Interactive.Div
        name="Product description"
        premountFor={fps}
        style={{
          position: "absolute",
          left: 148,
          top: 645,
          fontSize: 45,
          color: "#52616b",
          opacity: interpolate(frame, [0.4 * fps, 0.9 * fps], [0, 1], {
            extrapolateLeft: "clamp",
            extrapolateRight: "clamp",
          }),
        }}
      >
        Your Singapore concession-pass tracker.
      </Interactive.Div>
      <Interactive.Div
        name="Example train pass"
        premountFor={fps}
        style={{
          position: "absolute",
          left: 1290,
          top: 265,
          width: 470,
          height: 475,
          backgroundColor: "#006c67",
          borderRadius: 30,
          color: "#f6f8fa",
          padding: 48,
          rotate: interpolate(frame, [0, 0.8 * fps], ["8deg", "3deg"], {
            extrapolateRight: "clamp",
            easing: Easing.bezier(0.16, 1, 0.3, 1),
          }),
          translate: interpolate(
            frame,
            [0, 0.8 * fps],
            ["200px 50px", "0px 0px"],
            {
              extrapolateRight: "clamp",
              easing: Easing.bezier(0.16, 1, 0.3, 1),
            },
          ),
          opacity: interpolate(frame, [0.15 * fps, 0.6 * fps], [0, 1], {
            extrapolateLeft: "clamp",
            extrapolateRight: "clamp",
          }),
        }}
      >
        <div style={{ fontSize: 29, letterSpacing: 3 }}>MONTHLY TRAIN PASS</div>
        <div
          style={{
            fontSize: 122,
            fontWeight: 650,
            letterSpacing: -6,
            marginTop: 58,
          }}
        >
          S$48
        </div>
        <div
          style={{
            borderTop: "2px dashed #78b6af",
            marginTop: 44,
            paddingTop: 30,
            fontSize: 31,
          }}
        >
          Example pass price
        </div>
        <div
          style={{
            position: "absolute",
            width: 38,
            height: 38,
            borderRadius: "50%",
            backgroundColor: "#e8b339",
            top: -19,
            right: 40,
          }}
        />
      </Interactive.Div>
      <svg
        width="1640"
        height="130"
        viewBox="0 0 1640 130"
        style={{
          position: "absolute",
          left: 140,
          top: 815,
          overflow: "visible",
        }}
      >
        <path
          d="M 18 70 H 1150 Q 1200 70 1245 40 H 1620"
          fill="none"
          stroke="#d4dce1"
          strokeWidth="12"
          strokeLinecap="round"
        />
        <path
          d="M 18 70 H 1150 Q 1200 70 1245 40 H 1620"
          fill="none"
          stroke="#006c67"
          strokeWidth="12"
          strokeLinecap="round"
          pathLength="1"
          strokeDasharray="1"
          strokeDashoffset={interpolate(frame, [0.2 * fps, 2.9 * fps], [1, 0], {
            extrapolateLeft: "clamp",
            extrapolateRight: "clamp",
            easing: Easing.bezier(0.33, 0, 0.2, 1),
          })}
        />
        <circle
          cx="18"
          cy="70"
          r="18"
          fill="#f6f8fa"
          stroke="#006c67"
          strokeWidth="7"
        />
        <circle cx="1620" cy="40" r="23" fill="#e8b339" />
      </svg>
    </Frame>
  );
};

export const Opening = Interactive.create({
  Component: OpeningInner,
  schema: {},
  layout: "absolute-fill",
});
