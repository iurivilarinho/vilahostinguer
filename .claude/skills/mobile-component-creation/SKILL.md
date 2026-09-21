---
name: mobile-component-creation
description: create or modify React Native components following project conventions. use when creating a new shared or feature-level component, adding style variants, wrapping a React Native primitive, deciding whether a component should be shared or stay inside a feature, or building camera/barcode scanner components.
---

This project builds components as **named arrow function exports** (or `forwardRef` when a ref is needed) with **typed props** and **theme-based styling** via `useAppTheme()`.

---

## Two scopes

| Scope         | Location                       | Purpose                                                 |
| ------------- | ------------------------------ | ------------------------------------------------------- |
| Shared        | `src/components/`              | Reused across 2+ features, purely presentational        |
| Feature-level | `features/<name>/components/`  | Used only within a single feature, may call data hooks  |

---

## Architecture Overview

### Props

- Define a typed interface: `interface MyComponentProps extends ViewProps { ... }`
- Extend from the base RN primitive when forwarding props (e.g. `PressableProps`, `ViewProps`, `TextProps`)

### Styling

- Access theme via `useAppTheme()` — never hardcode colors or spacing
- Define styles inline inside `useMemo` returning a `{ name: ViewStyle | TextStyle }` map. The hook recomputes when `theme` (or relevant props) change, so theme-token references stay reactive
- Do **not** use `StyleSheet.create` — it captures values at module-eval time, which is incompatible with theme switching. `StyleSheet.absoluteFill`, `StyleSheet.absoluteFillObject`, and `StyleSheet.hairlineWidth` (the static constants) are still fine to reference inline
- No CSS, no Tailwind — this is React Native

### Variants

- Define variant maps as typed `Record<string, ViewStyle | TextStyle>` objects
- Select the correct variant entry based on the prop value
- Follow the pattern in `src/components/button/Button.tsx`

### forwardRef

- Use `forwardRef` when the component needs to expose a ref (e.g. inputs, pressables)
- Type the ref using `ComponentRef<typeof NativePrimitive>`

### Feature components

- May call Realm service hooks or other data hooks
- Props should stay scoped to the feature — avoid leaking feature-specific types into shared components

---

## Camera / barcode scanning

Use **`react-native-vision-camera@^4.7`** as the canonical camera library. Do **not** use `react-native-camera-kit` (legacy, slower — AVFoundation/ZXing) or the v5+ line (Nitro modules, `useObjectOutput` is iOS-only as of this writing).

### Stack

- `react-native-vision-camera@^4.7.3` — on Android uses ML Kit for decoding (very fast); on iOS uses AVFoundation native scanner.
- Built-in hooks: `useCameraDevice`, `useCameraPermission`, `useCodeScanner`. **No external code-scanner plugin needed.**
- `react-native-worklets-core` is only required for frame processors; not for code scanning.

### Native config

- Android: `<uses-permission android:name="android.permission.CAMERA" />` in `AndroidManifest.xml`. `minSdkVersion = 24` or higher.
- iOS: `NSCameraUsageDescription` string in `Info.plist`.
- Migration from another camera lib requires **full native rebuild** (`./gradlew clean && run-android`, `pod install && run-ios`) — Metro reload is not enough.

### Component shape (`src/components/barcodeScanner/`)

A reusable `<BarcodeScanner visible onClose onRead />` modal. Required pieces:

1. **Permission gate** via `useCameraPermission()`. Trigger `requestPermission()` once when `visible` flips to `true`. Render a "permission denied" message if denied.
2. **Device selection** via `useCameraDevice('back')`. Show a loading placeholder if `device` is `undefined`.
3. **Code scanner** via `useCodeScanner({ codeTypes, onCodeScanned })`. Restrict `codeTypes` to the formats your app actually uses — typical WMS/inventory: `['ean-13', 'ean-8', 'code-128', 'code-39', 'qr', 'data-matrix']`. Listing all 14 supported formats degrades latency.
4. **Single-shot read** via `useRef<boolean>(false)` to lock after the first valid decode. Reset the lock in the `useEffect` cleanup when the modal closes. Vision Camera fires `onCodeScanned` continuously while the same code is in frame — without the lock you'll dispatch the callback dozens of times.
5. **Visual overlay** centered on the camera preview: a fixed-size frame (e.g. 260×260) with 4 corner brackets in the primary theme color, surrounded by a dim layer (use `theme.colors.scrim`). Use `pointerEvents="none"` on the overlay so it doesn't intercept touches. No external dependency needed — plain `View` with inline `useMemo` styles.

### Pairing with text input

Expose `<ScannerInput />` in `src/components/scannerInput/` — a numeric `<Input>` with a scan icon button that opens `<BarcodeScanner>`. Accept `enableCameraScanner?: boolean` to disable the camera trigger for screens that only allow keyboard input.

### Performance notes

- Restricting `codeTypes` is the single biggest win (3-10× faster decode in the typical case).
- The `readLockRef` pattern avoids re-renders from duplicate callbacks; combine with closing the modal in the consumer's `onRead`.
- Bind `<Camera isActive>` directly to the modal `visible` state. The camera releases when the modal unmounts.

---

## When to use

- Creating a new **shared UI component** in `src/components/`
- Creating a new **feature component** in `features/<name>/components/`
- Adding **style variants** to an existing component
- Wrapping a React Native primitive with project-specific behavior
- Deciding whether a component should be shared or stay inside a feature

Always inspect existing components (e.g. `Button.tsx`, `src/components/card/`, `src/components/form/`) before introducing custom patterns.
