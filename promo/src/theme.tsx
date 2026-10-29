import React from "react";
import { loadFont } from "@remotion/fonts";
import {
  AbsoluteFill,
  CanvasImage,
  staticFile,
  useVideoConfig,
} from "remotion";

void loadFont({
  family: "Archivo",
  url: staticFile("fonts/archivo-latin.woff2"),
  weight: "100 900",
});

export const Frame: React.FC<{
  children: React.ReactNode;
  dark?: boolean;
  label?: string;
}> = ({ children, dark = false, label = "SINGAPORE · MRT + LRT" }) => {
  const { fps } = useVideoConfig();
  return (
    <AbsoluteFill
      style={{
        backgroundColor: dark ? "#063d3a" : "#f6f8fa",
        color: dark ? "#f6f8fa" : "#18242b",
        fontFamily: "Archivo, sans-serif",
        overflow: "hidden",
      }}
    >
      <div
        style={{
          position: "absolute",
          left: 140,
          top: 78,
          display: "flex",
          alignItems: "center",
          gap: 20,
        }}
      >
        <CanvasImage
          src={staticFile("farenough.svg")}
          premountFor={fps}
          style={{ width: 62, height: 62 }}
        />
        <div style={{ fontSize: 42, fontWeight: 650, letterSpacing: -1.7 }}>
          farenough
        </div>
      </div>
      <div
        style={{
          position: "absolute",
          right: 140,
          top: 100,
          fontSize: 25,
          letterSpacing: 3,
          color: dark ? "#a9d2ce" : "#52616b",
        }}
      >
        {label}
      </div>
      {children}
    </AbsoluteFill>
  );
};

export const sgd = (cents: number) => `S$${(cents / 100).toFixed(2)}`;
