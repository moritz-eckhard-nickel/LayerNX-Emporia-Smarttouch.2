# moritz-flipOS – Device & Development Reference

> **Target:** Emporia TOUCHsmart.2 (V188.2) · Android 11 · 32-bit ARM  
> **Project:** `moritz-flipOS` – custom, lightweight, keypad-first Android launcher.  
> **Strategy:** Keep the existing Android 11 firmware, phone stack and vendor drivers. Replace the everyday user experience at application level, not the operating system.

## Confirmed hardware / firmware

These facts come from ADB diagnostics on the actual device (November 2026), not from a retail product description.

| Field | Value | Notes |
|---|---|---|
| Model | Emporia TOUCHsmart.2 / `V188.2` | Manufacturer/model product family |
| Android | **11**, API **30** | Verified with `ro.build.version.release` and `ro.build.version.sdk` |
| Stock build | `EMP.V188.2_GE.MP.OM.V2.15` | Build string additionally includes `svn:rev4279 build:20250715-1912` |
| Hardware platform | `mt6739` | MediaTek |
| Primary ABI | `armeabi-v7a` | A 32-bit app ABI; do not assume arm64 until separately confirmed |
| Kernel | Linux `4.14.186` | Stock kernel |
| Project Treble | `true` | Vendor separation exists; not needed to make a launcher |
| A/B updates | `true` | `boot_a/b`, `vendor_boot_a/b`, `dtbo_a/b` and corresponding partitions exist |
| Dynamic partitions | `super` present | Logical partitions mounted at `/`, `/vendor`, `/product` |
| Bootloader | `ro.boot.flash.locked=1` | Locked. Leave locked for this project |
| ADB shell | UID `2000` (`shell`) | USB debugging works; **not root** |
| System partition | ~1.2 GB, full/read-only | Normal for Android stock images |
| Vendor partition | ~190 MB, full/read-only | Contains OEM/platform components |
| Product partition | ~73 MB, full/read-only | Normal for Android stock images |
| Userdata | ~4.1 GB, 3.3 GB free during scan | Changes as apps/data change |

No serial number, IMEI, phone number, accounts, MAC address or IP address are intentionally included in this document.

## Product-specific integration goals

1. **Custom HOME launcher:** Implement as a regular Android app with HOME / DEFAULT intent filters, and allow the user to choose it as the default launcher if the OEM firmware permits.
2. **Keypad-first navigation:** Handle D-pad, confirm/select, back, number keys and dedicated call/end keys. Test actual key codes; **do not hard-code guesses**.
3. **Touchscreen support:** Touch and keypad navigation should both work. Account for small display size and font scaling.
4. **Flip/lid behavior:** Identify whether opening and closing is exposed as an input switch, Hall sensor, display event or private OEM broadcast/service.
5. **Outer display:** Inspect display enumeration and OEM services before promising third-party control; public Android multi-display APIs may not expose all vendor functionality.
6. **Phone & SMS:** Initially **launch existing system apps** and keep VoLTE, emergency calling and other telephony functionality untouched. Do not replace privileged applications merely to change appearance.
7. **Performance:** Keep RAM usage and app startup time low, avoid heavy web-based UI frameworks and background polling.
8. **Recovery:** Document how to switch back to the stock launcher before changing defaults, hiding apps or experimenting with special permissions.

## Engineering constraints for Codex / Claude Code

- Target **Android 11 / API 30** initially; the app can compile with newer SDK versions if runtime behavior on API 30 is tested.
- Support **32-bit ARM (`armeabi-v7a`)** for any native libraries; pure Kotlin/Java apps do not require a separate ABI-specific build.
- Prefer Kotlin with Android Views or another lightweight UI; make all core screens usable with only hardware buttons.
- Keep system privileges, root, bootloader unlocking, partition changes, `pm disable`, and preinstalled-app removal **out of scope** for the first release.
- Make hardware integrations modular: `KeypadController`, `FlipStateMonitor`, `ExternalDisplayAdapter`, `TelephonyIntents` (names are suggestions, not existing APIs).
- Implement graceful fallbacks where OEM services, secondary display, or special keys cannot be accessed by a third-party app.
- Do not assume normal APIs can intercept `POWER`, `HOME`, emergency call behavior or vendor-owned buttons.
- Log only event types and key codes during debugging; never collect typed content, messages, phone numbers or contacts into logs intended for publication.
- Use `moritz-` or `moritz_` prefix for script/tool names. Author attribution, if needed: **Moritz und Zen**.

## Development acceptance checklist

- [ ] Launcher installs and starts on stock V188.2 without root.
- [ ] HOME app selection and stock-launcher rollback tested.
- [ ] Navigation works with keypad only (no touch required for normal flows).
- [ ] Touchscreen gestures and scrolling remain functional.
- [ ] Calls, SMS, Bluetooth, Wi-Fi, volume, camera and lock screen still work via existing system functionality.
- [ ] Flip opening/closing is detected if a supported interface exists.
- [ ] Outer-screen behavior is documented (supported, unsupported, or requires privileged integration).
- [ ] App runs reliably on limited RAM and the phone's small screen.

## Field-testing commands (run from Windows PowerShell in `C:\platform-tools`)

Read-only inspection, with USB debugging already authorized:

```powershell
.\adb.exe devices
.\adb.exe shell wm size
.\adb.exe shell wm density
.\adb.exe shell dumpsys display
.\adb.exe shell cat /proc/bus/input/devices
.\adb.exe shell getevent -lp
.\adb.exe shell dumpsys input
.\adb.exe shell dumpsys sensorservice
.\adb.exe shell pm list packages -s
.\adb.exe shell cmd role get-role-holders android.app.role.HOME
```

For **interactive hardware-button mapping only**, while no passwords or personal content are being entered:

```powershell
.\adb.exe shell getevent -lt
```

Press exactly one hardware key at a time, note its event code, then use **Ctrl+C** to stop. `getevent -lt` is a continuous live stream, unlike `getevent -lp`. It may require additional permissions and should **never** be left running to log private typing. Compare flip-open and flip-closed behavior separately. Android-level `KeyEvent.keyCode` may differ from Linux event codes; verify in an actual test app.

**Next inspection:** Run `moritz-flipos-scan.ps1` beside `adb.exe`. It refreshes the automatically collected portion of this document without modifying the manually maintained project requirements above.

<!-- BEGIN MORITZ-FLIPOS-SCAN -->
## Automatically collected device information

Generated: 2026-11-09 14:33:03

All commands in this section are read-only. Empty output may mean unsupported or restricted access.

### System properties (allowlisted)

| Property | Value |
|---|---|
| `ro.product.manufacturer` | emporia_V188.2 |
| `ro.product.model` | emporia_V188.2 |
| `ro.product.device` | emporia_V188.2 |
| `ro.build.version.release` | 11 |
| `ro.build.version.sdk` | 30 |
| `ro.build.display.id` | EMP.V188.2_GE.MP.OM.V2.15 svn:rev4279 build:20250715-1912 |
| `ro.build.fingerprint` | emporia/emporia_V188.2/emporia_V188.2:11/EMP.V188.2_GE.MP.OM.V2.15/mp1V846:user/release-keys |
| `ro.product.cpu.abi` | armeabi-v7a |
| `ro.product.cpu.abilist` | armeabi-v7a,armeabi |
| `ro.product.cpu.abilist64` | (empty or unsupported) |
| `ro.hardware` | mt6739 |
| `ro.board.platform` | mt6739 |
| `ro.treble.enabled` | true |
| `ro.vndk.version` | 30 |
| `ro.vendor.build.version.sdk` | (empty or unsupported) |
| `ro.product.first_api_level` | 30 |
| `ro.build.ab_update` | true |
| `ro.boot.slot_suffix` | _a |
| `ro.boot.flash.locked` | 1 |
| `ro.boot.verifiedbootstate` | green |
| `ro.oem_unlock_supported` | 1 |

### Kernel

Command: `uname -a`

```text
Linux localhost 4.14.186 #1 SMP PREEMPT Tue Jul 15 17:50:24 CST 2025 armv7l
```

### RAM and swap

Command: `cat /proc/meminfo (selected lines)`

```text
MemTotal:         939672 kB
MemFree:           44704 kB
MemAvailable:     346704 kB
SwapTotal:        704748 kB
SwapFree:         427692 kB
```

### Filesystem space

Command: `df -h`

```text
Filesystem      Size  Used Avail Use% Mounted on
tmpfs           459M  1.9M  457M   1% /dev
tmpfs           459M     0  459M   0% /mnt
/dev/block/dm-4 1.2G  1.2G     0 100% /
/dev/block/dm-5 190M  189M     0 100% /vendor
/dev/block/dm-6  73M   73M     0 100% /product
tmpfs           459M     0  459M   0% /apex
/dev/block/dm-8 4.1G  775M  3.3G  19% /data
/dev/fuse       4.1G  775M  3.3G  19% /storage/emulated
```

### Primary display size

Command: `wm size`

```text
Physical size: 480x854
```

### Primary display density

Command: `wm density`

```text
Physical density: 240
```

### Display subsystem (filtered)

Command: `dumpsys display`

```text
  mViewports=[DisplayViewport{type=INTERNAL, valid=true, isActive=true, displayId=0, uniqueId='local:0', physicalPort=0, orientation=0, logicalFrame=Rect(0, 0 - 480, 854), physicalFrame=Rect(0, 0 - 480, 854), deviceWidth=480, deviceHeight=854}]
  DisplayDeviceInfo{"Integrierter Bildschirm": uniqueId="local:0", 480 x 854, modeId 1, defaultModeId 1, supportedModes [{id=1, width=480, height=854, fps=60.0}], colorMode 0, supportedColorModes [0], HdrCapabilities HdrCapabilities{mSupportedHdrTypes=[], mMaxLuminance=500.0, mMaxAverageLuminance=500.0, mMinLuminance=0.0}, allmSupported false, gameContentTypeSupported false, density 240, 160.0 x 160.0 dpi, appVsyncOff 8300000, presDeadline 9366667, touch INTERNAL, rotation 0, type INTERNAL, address {port=0}, deviceProductInfo null, state ON, FLAG_DEFAULT_DISPLAY, FLAG_ROTATES_WITH_CONTENT, FLAG_SECURE, FLAG_SUPPORTS_PROTECTED_BUFFERS}
    mUniqueId=local:0
    mDisplayModeSpecs={baseModeId=1 primaryRefreshRateRange=[0 60] appRequestRefreshRateRange=[0 Infinity]}
    mDisplayInfo=DisplayInfo{isInternal=true, density=1.5, secure=true, deviceProductInfo=null}
      DisplayConfig{width=480, height=854, xDpi=160.0, yDpi=160.0, refreshRate=60.0, appVsyncOffsetNanos=8300000, presentationDeadlineNanos=9366667, configGroup=-1}
mDisplayId=0
    mDesiredDisplayModeSpecs={baseModeId=1 primaryRefreshRateRange=[0 60] appRequestRefreshRateRange=[0 Infinity]}
    mBaseDisplayInfo=DisplayInfo{"Integrierter Bildschirm", displayId 0, FLAG_SECURE, FLAG_SUPPORTS_PROTECTED_BUFFERS, FLAG_TRUSTED, real 480 x 854, largest app 480 x 854, smallest app 480 x 854, appVsyncOff 8300000, presDeadline 9366667, mode 1, defaultMode 1, modes [{id=1, width=480, height=854, fps=60.0}], hdrCapabilities HdrCapabilities{mSupportedHdrTypes=[], mMaxLuminance=500.0, mMaxAverageLuminance=500.0, mMinLuminance=0.0}, minimalPostProcessingSupported false, rotation 0, state ON, type INTERNAL, uniqueId "local:0", app 480 x 854, density 240 (160.0 x 160.0) dpi, layerStack 0, colorMode 0, supportedColorModes [0], address {port=0}, deviceProductInfo null, removeMode 0}
    mOverrideDisplayInfo=DisplayInfo{"Integrierter Bildschirm", displayId 0, FLAG_SECURE, FLAG_SUPPORTS_PROTECTED_BUFFERS, FLAG_TRUSTED, real 480 x 854, largest app 776 x 779, smallest app 480 x 480, appVsyncOff 8300000, presDeadline 9366667, mode 1, defaultMode 1, modes [{id=1, width=480, height=854, fps=60.0}], hdrCapabilities HdrCapabilities{mSupportedHdrTypes=[], mMaxLuminance=500.0, mMaxAverageLuminance=500.0, mMinLuminance=0.0}, minimalPostProcessingSupported false, rotation 0, state ON, type INTERNAL, uniqueId "local:0", app 480 x 779, density 240 (160.0 x 160.0) dpi, layerStack 0, colorMode 0, supportedColorModes [0], address {port=0}, deviceProductInfo null, removeMode 0}
      PRIORITY_USER_SETTING_MIN_REFRESH_RATE -> Vote{width=-1, height=-1, minRefreshRate=0.0, maxRefreshRate=Infinity}
      PRIORITY_DEFAULT_REFRESH_RATE -> Vote{width=-1, height=-1, minRefreshRate=0.0, maxRefreshRate=60.0}
    mDefaultRefreshRate: 60.0
    mDefaultPeakRefreshRate: 0.0
    mRefreshRateInZone: 0
```

### Linux input devices

Command: `cat /proc/bus/input/devices`

```text
(not available on this firmware / permission denied)
```

### Input device capabilities (not live keystrokes)

Command: `getevent -lp`

```text
add device 1: /dev/input/event2
  name:     "mtk-tpd"
  events:
    KEY (0001): BTN_TOUCH            
    ABS (0003): ABS_X                 : value 0, min 0, max 480, fuzz 0, flat 0, resolution 480
                ABS_Y                 : value 0, min 0, max 854, fuzz 0, flat 0, resolution 854
                ABS_PRESSURE          : value 0, min 0, max 255, fuzz 0, flat 0, resolution 0
                ABS_MT_SLOT           : value 0, min 0, max 4, fuzz 0, flat 0, resolution 0
                ABS_MT_TOUCH_MAJOR    : value 0, min 0, max 100, fuzz 0, flat 0, resolution 0
                ABS_MT_TOUCH_MINOR    : value 0, min 0, max 100, fuzz 0, flat 0, resolution 0
                ABS_MT_POSITION_X     : value 0, min 0, max 480, fuzz 0, flat 0, resolution 0
                ABS_MT_POSITION_Y     : value 0, min 0, max 854, fuzz 0, flat 0, resolution 0
                ABS_MT_TRACKING_ID    : value 0, min 0, max 10, fuzz 0, flat 0, resolution 0
  input props:
    INPUT_PROP_DIRECT
add device 2: /dev/input/event0
  name:     "ACCDET"
  events:
    KEY (0001): KEY_VOLUMEDOWN        KEY_VOLUMEUP          KEY_PLAYPAUSE         KEY_VOICECOMMAND     
    SW  (0005): SW_HEADPHONE_INSERT   SW_MICROPHONE_INSERT  SW_LINEOUT_INSERT     SW_JACK_PHYSICAL_INS    
  input props:
    <none>
add device 3: /dev/input/event3
  name:     "HALL"
  events:
    KEY (0001): KEY_LEFTALT           KEY_RIGHTALT          KEY_POWER            
  input props:
    <none>
add device 4: /dev/input/event1
  name:     "mtk-kpd"
  events:
    KEY (0001): KEY_1                 KEY_2                 KEY_3                 KEY_4                
                KEY_5                 KEY_6                 KEY_7                 KEY_8                
                KEY_9                 KEY_0                 KEY_BACKSPACE         KEY_ENTER            
                KEY_UP                KEY_LEFT              KEY_RIGHT             KEY_DOWN             
                KEY_VOLUMEDOWN        KEY_VOLUMEUP          KEY_POWER             KEY_MENU             
                KEY_MAIL              KEY_BACK              KEY_CAMERA            KEY_SWITCHVIDEOMODE  
                KEY_SEND              BTN_MISC              BTN_1                 KEY_NUMERIC_POUND    
  input props:
    <none>
```

### Android input mapping (filtered)

Command: `dumpsys input`

```text
Event Hub State:
  BuiltInKeyboardId: -2
      Classes: 0x40000023
      Path: <virtual>
      KeyLayoutFile: /system/usr/keylayout/Generic.kl
      KeyCharacterMapFile: /system/usr/keychars/Virtual.kcm
      Classes: 0x00000014
      Path: /dev/input/event2
      KeyLayoutFile: 
      KeyCharacterMapFile: 
      Classes: 0x00000081
      Path: /dev/input/event0
      KeyLayoutFile: /system/usr/keylayout/Generic.kl
      KeyCharacterMapFile: /system/usr/keychars/Generic.kcm
      Classes: 0x00000001
      Path: /dev/input/event3
      KeyLayoutFile: /system/usr/keylayout/Generic.kl
      KeyCharacterMapFile: /system/usr/keychars/Generic.kcm
      Classes: 0x00000021
      Path: /dev/input/event1
      KeyLayoutFile: /system/usr/keylayout/mtk-kpd.kl
      KeyCharacterMapFile: /system/usr/keychars/Generic.kcm
Input Reader State:
  Device 5: mtk-tpd
    Sources: 0x00001002
    KeyboardType: 0
    Touch Input Mapper (mode - direct):
  Device 4: ACCDET
    Sources: 0x80000101
    KeyboardType: 1
    Keyboard Input Mapper:
      KeyboardType: 1
  Device 3: HALL
    Sources: 0x00000101
    KeyboardType: 1
    Keyboard Input Mapper:
      KeyboardType: 1
  Device 2: mtk-kpd
    Sources: 0x00000301
    KeyboardType: 1
    Keyboard Input Mapper:
      KeyboardType: 1
    Sources: 0x00000301
    KeyboardType: 2
    Keyboard Input Mapper:
      KeyboardType: 2
      0: name='Window{bcaa53e u0 NavigationBar0}', displayId=0, portalToDisplayId=-1, paused=false, hasFocus=false, hasWallpaper=false, visible=true, canReceiveKeys=false, flags=0x21840068, type=0x000007e3, frame=[0,779][480,854], globalScale=1.000000, windowScale=(1.000000,1.000000), touchableRegion=[0,779][480,854], inputFeatures=0x00000000, ownerPid=1090, ownerUid=10113, dispatchingTimeout=5000ms
      1: name='Window{59ba1ac u0 StatusBar}', displayId=0, portalToDisplayId=-1, paused=false, hasFocus=false, hasWallpaper=false, visible=false, canReceiveKeys=false, flags=0x81800008, type=0x000007d0, frame=[-1,-1][0,0], globalScale=1.000000, windowScale=(1.000000,1.000000), touchableRegion=<empty>, inputFeatures=0x00000000, ownerPid=1090, ownerUid=10113, dispatchingTimeout=5000ms
      2: name='Window{4d513b9 u0 com.android.settings/com.android.settings.Settings$DevelopmentSettingsDashboardActivity}', displayId=0, portalToDisplayId=-1, paused=false, hasFocus=true, hasWallpaper=false, visible=true, canReceiveKeys=true, flags=0x81810120, type=0x00000001, frame=[0,0][480,854], globalScale=1.000000, windowScale=(1.000000,1.000000), touchableRegion=[0,0][480,854], inputFeatures=0x00000000, ownerPid=4127, ownerUid=1000, dispatchingTimeout=5000ms
      3: name='Window{aa8c9d0 u0 com.android.systemui.ImageWallpaper}', displayId=0, portalToDisplayId=-1, paused=false, hasFocus=false, hasWallpaper=false, visible=false, canReceiveKeys=false, flags=0x00014318, type=0x000007dd, frame=[-24,-43][504,897], globalScale=1.000000, windowScale=(0.909091,0.909091), touchableRegion=[-24,-43][504,896], inputFeatures=0x00000000, ownerPid=1090, ownerUid=10113, dispatchingTimeout=5000ms
    331: channelName='ecf97d InputMethod (server)', windowName='ecf97d InputMethod (server)', status=NORMAL, monitor=false, responsive=true
```

### Keylayout file names (system)

Command: `ls /system/usr/keylayout`

```text
AVRCP.kl
Generic.kl
Vendor_0079_Product_0011.kl
Vendor_0079_Product_18d4.kl
Vendor_044f_Product_b326.kl
Vendor_045e_Product_028e.kl
Vendor_045e_Product_028f.kl
Vendor_045e_Product_02a1.kl
Vendor_045e_Product_02d1.kl
Vendor_045e_Product_02dd.kl
Vendor_045e_Product_02e0.kl
Vendor_045e_Product_02e3.kl
Vendor_045e_Product_02ea.kl
Vendor_045e_Product_02fd.kl
Vendor_045e_Product_0b12.kl
Vendor_046d_Product_b501.kl
Vendor_046d_Product_c216.kl
Vendor_046d_Product_c219.kl
Vendor_046d_Product_c21d.kl
Vendor_046d_Product_c21e.kl
Vendor_046d_Product_c21f.kl
Vendor_046d_Product_c242.kl
Vendor_046d_Product_c294.kl
Vendor_046d_Product_c299.kl
Vendor_046d_Product_c532.kl
Vendor_054c_Product_0268.kl
Vendor_054c_Product_0268_Version_8000.kl
Vendor_054c_Product_0268_Version_8100.kl
Vendor_054c_Product_0268_Version_8111.kl
Vendor_054c_Product_05c4.kl
Vendor_054c_Product_05c4_Version_8000.kl
Vendor_054c_Product_05c4_Version_8100.kl
Vendor_054c_Product_05c4_Version_8111.kl
Vendor_054c_Product_09cc.kl
Vendor_054c_Product_09cc_Version_8000.kl
Vendor_054c_Product_09cc_Version_8100.kl
Vendor_054c_Product_09cc_Version_8111.kl
Vendor_054c_Product_0ba0.kl
Vendor_054c_Product_0ba0_Version_8111.kl
Vendor_056e_Product_2004.kl
Vendor_057e_Product_2009.kl
Vendor_0583_Product_2060.kl
Vendor_05ac_Product_0239.kl
Vendor_06a3_Product_f51a.kl
Vendor_0738_Product_4716.kl
Vendor_0738_Product_4718.kl
Vendor_0738_Product_4726.kl
Vendor_0738_Product_4736.kl
Vendor_0738_Product_4740.kl
Vendor_0738_Product_9871.kl
Vendor_0738_Product_b726.kl
Vendor_0738_Product_beef.kl
Vendor_0738_Product_cb02.kl
Vendor_0738_Product_cb03.kl
Vendor_0738_Product_cb29.kl
Vendor_0738_Product_f738.kl
Vendor_07ff_Product_ffff.kl
Vendor_0b05_Product_4500.kl
Vendor_0e6f_Product_0113.kl
Vendor_0e6f_Product_011f.kl
Vendor_0e6f_Product_0131.kl
Vendor_0e6f_Product_0133.kl
Vendor_0e6f_Product_0139.kl
Vendor_0e6f_Product_013a.kl
Vendor_0e6f_Product_0146.kl
Vendor_0e6f_Product_0147.kl
Vendor_0e6f_Product_0161.kl
Vendor_0e6f_Product_0162.kl
Vendor_0e6f_Product_0163.kl
Vendor_0e6f_Product_0164.kl
Vendor_0e6f_Product_0165.kl
Vendor_0e6f_Product_0201.kl
Vendor_0e6f_Product_0213.kl
Vendor_0e6f_Product_021f.kl
Vendor_0e6f_Product_0246.kl
Vendor_0e6f_Product_02a4.kl
Vendor_0e6f_Product_02a6.kl
Vendor_0e6f_Product_02ab.kl
Vendor_0e6f_Product_0301.kl
Vendor_0e6f_Product_0346.kl
Vendor_0e6f_Product_0401.kl
Vendor_0e6f_Product_0413.kl
Vendor_0e6f_Product_0501.kl
Vendor_0e6f_Product_f900.kl
Vendor_0f0d_Product_000a.kl
Vendor_0f0d_Product_000c.kl
Vendor_0f0d_Product_0067.kl
Vendor_1038_Product_1412.kl
Vendor_1038_Product_1430.kl
Vendor_1038_Product_1431.kl
Vendor_11c9_Product_55f0.kl
Vendor_12ab_Product_0301.kl
Vendor_12bd_Product_d015.kl
Vendor_1430_Product_4748.kl
Vendor_1430_Product_f801.kl
Vendor_146b_Product_0601.kl
Vendor_1532_Product_0037.kl
Vendor_1532_Product_0900.kl
Vendor_1532_Product_0a03.kl
Vendor_15e4_Product_3f00.kl
[truncated: 152 total lines]
```

### Keylayout file names (vendor)

Command: `ls /vendor/usr/keylayout`

```text
(not available on this firmware / permission denied)
```

### Keylayout file names (odm)

Command: `ls /odm/usr/keylayout`

```text
(not available on this firmware / permission denied)
```

### Active keylayout: /system/usr/keylayout/Generic.kl

Command: `cat /system/usr/keylayout/Generic.kl`

```text
# Copyright (C) 2010 The Android Open Source Project
#
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
#      http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.

#
# Generic key layout file for full alphabetic US English PC style external keyboards.
#
# This file is intentionally very generic and is intended to support a broad range of keyboards.
# Do not edit the generic key layout to support a specific keyboard; instead, create
# a new key layout file with the required keyboard configuration.
#

key 1     ESCAPE
key 2     1
key 3     2
key 4     3
key 5     4
key 6     5
key 7     6
key 8     7
key 9     8
key 10    9
key 11    0
key 12    MINUS
key 13    EQUALS
key 14    DEL
key 15    TAB
key 16    Q
key 17    W
key 18    E
key 19    R
key 20    T
key 21    Y
key 22    U
key 23    I
key 24    O
key 25    P
key 26    LEFT_BRACKET
key 27    RIGHT_BRACKET
key 28    ENTER
key 29    CTRL_LEFT
key 30    A
key 31    S
key 32    D
key 33    F
key 34    G
key 35    H
key 36    J
key 37    K
key 38    L
key 39    SEMICOLON
key 40    APOSTROPHE
key 41    GRAVE
key 42    SHIFT_LEFT
key 43    BACKSLASH
key 44    Z
key 45    X
key 46    C
key 47    V
key 48    B
key 49    N
key 50    M
key 51    COMMA
key 52    PERIOD
key 53    SLASH
key 54    SHIFT_RIGHT
key 55    NUMPAD_MULTIPLY
key 56    ALT_LEFT
key 57    SPACE
key 58    CAPS_LOCK
key 59    F1
key 60    F2
key 61    F3
key 62    F4
key 63    F5
key 64    F6
key 65    F7
key 66    F8
key 67    F9
key 68    F10
key 69    NUM_LOCK
key 70    SCROLL_LOCK
key 71    NUMPAD_7
key 72    NUMPAD_8
key 73    NUMPAD_9
key 74    NUMPAD_SUBTRACT
key 75    NUMPAD_4
key 76    NUMPAD_5
key 77    NUMPAD_6
key 78    NUMPAD_ADD
key 79    NUMPAD_1
key 80    NUMPAD_2
key 81    NUMPAD_3
key 82    NUMPAD_0
key 83    NUMPAD_DOT
# key 84 (undefined)
key 85    ZENKAKU_HANKAKU
key 86    BACKSLASH
key 87    F11
key 88    F12
key 89    RO
# key 90 "KEY_KATAKANA"
# key 91 "KEY_HIRAGANA"
key 92    HENKAN
key 93    KATAKANA_HIRAGANA
key 94    MUHENKAN
key 95    NUMPAD_COMMA
key 96    NUMPAD_ENTER
key 97    CTRL_RIGHT
key 98    NUMPAD_DIVIDE
key 99    SYSRQ
key 100   ALT_RIGHT
# key 101 "KEY_LINEFEED"
key 102   MOVE_HOME
key 103   DPAD_UP
key 104   PAGE_UP
key 105   DPAD_LEFT
key 106   DPAD_RIGHT
key 107   MOVE_END
key 108   DPAD_DOWN
key 109   PAGE_DOWN
key 110   INSERT
key 111   FORWARD_DEL
# key 112 "KEY_MACRO"
key 113   VOLUME_MUTE
key 114   VOLUME_DOWN
key 115   VOLUME_UP
key 116   POWER
key 117   NUMPAD_EQUALS
# key 118 "KEY_KPPLUSMINUS"
key 119   BREAK
# key 120 (undefined)
key 121   NUMPAD_COMMA
key 122   KANA
key 123   EISU
key 124   YEN
key 125   META_LEFT
key 126   META_RIGHT
key 127   MENU
key 128   MEDIA_STOP
# key 129 "KEY_AGAIN"
# key 130 "KEY_PROPS"
# key 131 "KEY_UNDO"
# key 132 "KEY_FRONT"
key 133   COPY
# key 134 "KEY_OPEN"
key 135   PASTE
# key 136 "KEY_FIND"
key 137   CUT
# key 138 "KEY_HELP"
key 139   MENU
key 140   CALCULATOR
# key 141 "KEY_SETUP"
key 142   SLEEP
key 143   WAKEUP
# key 144 "KEY_FILE"
# key 145 "KEY_SENDFILE"
# key 146 "KEY_DELETEFILE"
# key 147 "KEY_XFER"
# key 148 "KEY_PROG1"
# key 149 "KEY_PROG2"
key 150   EXPLORER
# key 151 "KEY_MSDOS"
key 152   POWER
# key 153 "KEY_DIRECTION"
# key 154 "KEY_CYCLEWINDOWS"
key 155   ENVELOPE
key 156   BOOKMARK
# key 157 "KEY_COMPUTER"
key 158   BACK
key 159   FORWARD
key 160   MEDIA_CLOSE
key 161   MEDIA_EJECT
key 162   MEDIA_EJECT
key 163   MEDIA_NEXT
key 164   MEDIA_PLAY_PAUSE
key 165   MEDIA_PREVIOUS
key 166   MEDIA_STOP
key 167   MEDIA_RECORD
key 168   MEDIA_REWIND
[truncated: 445 total lines]
```

### Active keylayout: /system/usr/keylayout/mtk-kpd.kl

Command: `cat /system/usr/keylayout/mtk-kpd.kl`

```text
key 399   GRAVE
key 2     1
key 3     2
key 4     3
key 5     4
key 6     5
key 7     6
key 8     7
key 9     8
key 10    9
key 11    0
key 158   BACK
key 230   SOFT_RIGHT
key 60    SOFT_RIGHT
key 107   ENDCALL
key 62    ENDCALL
key 229   MENU
key 139   MENU
key 59    MENU
key 127   SYM
key 217   SEARCH
key 228   POUND
key 523   POUND
key 227   STAR
key 522   STAR
key 231   CALL
key 61    CALL
key 232   DPAD_CENTER
key 108   DPAD_DOWN
key 103   DPAD_UP
key 102   HOME
key 105   DPAD_LEFT
key 106   DPAD_RIGHT
key 115   VOLUME_UP
key 114   VOLUME_DOWN
key 113   MUTE
key 116   POWER
key 212   CAMERA
key 211   FOCUS
key 256   BUTTON_1
key 257   BUTTON_2

key 16    Q
key 17    W
key 18    E
key 19    R
key 20    T
key 21    Y
key 22    U
key 23    I
key 24    O
key 25    P
key 26    LEFT_BRACKET
key 27    RIGHT_BRACKET
key 43    BACKSLASH

key 30    A
key 31    S
key 32    D
key 33    F
key 34    G
key 35    H
key 36    J
key 37    K
key 38    L
key 39    SEMICOLON
key 40    APOSTROPHE
key 14    F1

key 44    Z
key 45    X
key 46    C
key 47    V
key 48    B
key 49    N
key 50    M
key 51    COMMA
key 52    PERIOD
key 53    SLASH
key 28    DPAD_CENTER

key 56    ALT_LEFT
key 100   ALT_RIGHT
key 42    SHIFT_LEFT
key 54    SHIFT_RIGHT
key 15    TAB
key 57    SPACE
key 150   EXPLORER
key 155   ENVELOPE

key 12    MINUS
key 13    EQUALS
key 215   AT

key 164   MEDIA_PLAY_PAUSE
key 128   MEDIA_STOP
key 163   MEDIA_NEXT
key 165   MEDIA_PREVIOUS
key 168   MEDIA_REWIND
key 159   MEDIA_FAST_FORWARD
```

### Potential flip and orientation sensors (filtered)

Command: `dumpsys sensorservice`

```text
Sensor List:
0x00000001) ACCELEROMETER             | MTK             | ver: 1 | type: android.sensor.accelerometer(1) | perm: n/a | flags: 0x00000000
0x00000008) PROXIMITY                 | MTK             | ver: 1 | type: android.sensor.proximity(8) | perm: n/a | flags: 0x00000003
ACCELEROMETER: last 50 events
12:03:36 - 0x00000001 pid= 3044 uid=10116 package=android.view.OrientationEventListener
12:03:15 + 0x00000001 pid= 3044 uid=10116 package=android.view.OrientationEventListener samplingPeriod=200000us batchingPeriod=0us
```

### Possible flip/cover policy integration

Command: `dumpsys window policy`

```text
    mCameraLensCoverState=CAMERA_LENS_COVER_ABSENT
    mLidKeyboardAccessibility=0 mLidNavigationAccessibility=0 getLidBehavior=LID_BEHAVIOR_NONE
```

### Relevant system packages (filtered)

Command: `pm list packages -s`

```text
package:com.mediatek.gba
package:com.mediatek.ims
package:com.android.internal.display.cutout.emulation.corner
package:com.android.internal.display.cutout.emulation.double
package:com.mediatek.camera
package:com.mediatek.telephony
package:com.emporia.mms
package:com.android.internal.systemui.navbar.gestural_wide_back
package:com.mediatek.location.lppe.main
package:com.emporia.weather
package:com.mediatek.SettingsProviderResOverlay
package:com.mediatek.gnss.nonframeworklbs
package:com.mediatek.systemuiresoverlay
package:com.android.theme.icon_pack.rounded.systemui
package:com.mediatek.oobe
package:com.mediatek.simprocessor
package:com.android.mms.service
package:com.mediatek.engineermode
package:com.emporia.quicksetting
package:com.android.internal.display.cutout.emulation.hole
package:com.android.internal.display.cutout.emulation.tall
package:com.android.internal.systemui.navbar.threebutton
package:com.emporia.launcher
package:com.android.theme.icon_pack.rounded.launcher
package:com.emporia.inputmethod.latin
package:com.android.settings.intelligence
package:com.mediatek.frameworkresoverlay
package:com.android.internal.systemui.navbar.gestural_extra_wide_back
package:com.android.providers.settings
package:com.android.theme.icon_pack.filled.settings
package:com.mediatek
package:com.android.theme.icon_pack.circular.settings
package:com.emporia.contacts
package:com.emporia.update
package:com.mediatek.nlpservice
package:com.android.server.telecom
package:com.android.dialer
package:com.android.theme.icon_pack.filled.systemui
package:com.emporia.calendar
package:com.android.internal.display.cutout.emulation.waterfall
package:com.android.theme.icon_pack.circular.systemui
package:com.mediatek.capctrl.service
package:com.mediatek.callrecorder
package:com.emporia.gallery
package:com.android.settings
package:com.android.theme.icon_pack.filled.launcher
package:com.emporia.setup
package:com.mediatek.lbs.em2.ui
package:com.emporia.browser
package:com.android.theme.icon_pack.circular.launcher
package:com.emporia.emergency
package:com.android.internal.systemui.navbar.gestural
package:com.emporia.deskclock
package:com.android.systemui
package:com.android.bluetoothmidiservice
package:com.mediatek.location.mtknlp
package:com.emporia.calculator
package:com.emporia.update.overlay
package:com.android.bluetooth
package:com.android.providers.contacts
package:com.android.internal.systemui.navbar.gestural_narrow_back
package:com.android.theme.icon_pack.rounded.settings
```

### Android system features

Command: `pm list features`

```text
feature:reqGlEsVersion=0x30002
feature:android.hardware.audio.low_latency
feature:android.hardware.audio.output
feature:android.hardware.bluetooth
feature:android.hardware.bluetooth_le
feature:android.hardware.camera
feature:android.hardware.camera.any
feature:android.hardware.camera.autofocus
feature:android.hardware.camera.capability.manual_post_processing
feature:android.hardware.camera.capability.manual_sensor
feature:android.hardware.camera.flash
feature:android.hardware.camera.front
feature:android.hardware.faketouch
feature:android.hardware.location
feature:android.hardware.location.gps
feature:android.hardware.location.network
feature:android.hardware.microphone
feature:android.hardware.opengles.aep
feature:android.hardware.ram.low
feature:android.hardware.screen.landscape
feature:android.hardware.screen.portrait
feature:android.hardware.sensor.accelerometer
feature:android.hardware.sensor.compass
feature:android.hardware.sensor.light
feature:android.hardware.sensor.proximity
feature:android.hardware.telephony
feature:android.hardware.telephony.gsm
feature:android.hardware.telephony.ims
feature:android.hardware.touchscreen
feature:android.hardware.touchscreen.multitouch
feature:android.hardware.touchscreen.multitouch.distinct
feature:android.hardware.touchscreen.multitouch.jazzhand
feature:android.hardware.usb.accessory
feature:android.hardware.usb.host
feature:android.hardware.wifi
feature:android.hardware.wifi.direct
feature:android.hardware.wifi.passpoint
feature:android.software.adoptable_storage
feature:android.software.app_enumeration
feature:android.software.autofill
feature:android.software.backup
feature:android.software.cant_save_state
feature:android.software.companion_device_setup
feature:android.software.connectionservice
feature:android.software.cts
feature:android.software.device_admin
feature:android.software.file_based_encryption
feature:android.software.home_screen
feature:android.software.incremental_delivery
feature:android.software.input_methods
feature:android.software.ipsec_tunnels
feature:android.software.midi
feature:android.software.print
feature:android.software.secure_lock_screen
feature:android.software.securely_removes_users
feature:android.software.verified_boot
feature:android.software.webview
```

### Current HOME role holder

Command: `cmd role get-role-holders android.app.role.HOME`

```text
Unknown command: get-role-holders
```

### Current DIALER role holder

Command: `cmd role get-role-holders android.app.role.DIALER`

```text
Unknown command: get-role-holders
```

### Current SMS role holder

Command: `cmd role get-role-holders android.app.role.SMS`

```text
Unknown command: get-role-holders
```

### HOME activity resolution

Command: `cmd package resolve-activity --brief -a android.intent.action.MAIN -c android.intent.category.HOME`

```text
priority=0 preferredOrder=0 match=0x108000 specificIndex=-1 isDefault=true
com.emporia.launcher/.activity.MainActivity
```

### Partition names (no partition data)

Command: `ls -l /dev/block/by-name`

```text
total 0
lrwxrwxrwx 1 root root 21 2010-01-01 01:00 boot_a -> /dev/block/mmcblk0p26
lrwxrwxrwx 1 root root 21 2010-01-01 01:00 boot_b -> /dev/block/mmcblk0p40
lrwxrwxrwx 1 root root 20 2010-01-01 01:00 boot_para -> /dev/block/mmcblk0p1
lrwxrwxrwx 1 root root 21 2010-01-01 01:00 dtbo_a -> /dev/block/mmcblk0p28
lrwxrwxrwx 1 root root 21 2010-01-01 01:00 dtbo_b -> /dev/block/mmcblk0p42
lrwxrwxrwx 1 root root 20 2010-01-01 01:00 expdb -> /dev/block/mmcblk0p3
lrwxrwxrwx 1 root root 21 2010-01-01 01:00 flashinfo -> /dev/block/mmcblk0p46
lrwxrwxrwx 1 root root 20 2010-01-01 01:00 frp -> /dev/block/mmcblk0p4
lrwxrwxrwx 1 root root 21 2010-01-01 01:00 gz_a -> /dev/block/mmcblk0p23
lrwxrwxrwx 1 root root 21 2010-01-01 01:00 gz_b -> /dev/block/mmcblk0p37
lrwxrwxrwx 1 root root 21 2010-01-01 01:00 lk_a -> /dev/block/mmcblk0p24
lrwxrwxrwx 1 root root 21 2010-01-01 01:00 lk_b -> /dev/block/mmcblk0p38
lrwxrwxrwx 1 root root 21 2010-01-01 01:00 loader_ext_a -> /dev/block/mmcblk0p25
lrwxrwxrwx 1 root root 21 2010-01-01 01:00 loader_ext_b -> /dev/block/mmcblk0p39
lrwxrwxrwx 1 root root 21 2010-01-01 01:00 logo -> /dev/block/mmcblk0p15
lrwxrwxrwx 1 root root 21 2010-01-01 01:00 mcupmfw_a -> /dev/block/mmcblk0p22
lrwxrwxrwx 1 root root 21 2010-01-01 01:00 mcupmfw_b -> /dev/block/mmcblk0p36
lrwxrwxrwx 1 root root 21 2010-01-01 01:00 md1dsp_a -> /dev/block/mmcblk0p20
lrwxrwxrwx 1 root root 21 2010-01-01 01:00 md1dsp_b -> /dev/block/mmcblk0p34
lrwxrwxrwx 1 root root 21 2010-01-01 01:00 md1img_a -> /dev/block/mmcblk0p19
lrwxrwxrwx 1 root root 21 2010-01-01 01:00 md1img_b -> /dev/block/mmcblk0p33
lrwxrwxrwx 1 root root 20 2010-01-01 01:00 md_udc -> /dev/block/mmcblk0p7
lrwxrwxrwx 1 root root 20 2010-01-01 01:00 metadata -> /dev/block/mmcblk0p8
lrwxrwxrwx 1 root root 18 2010-01-01 01:00 mmcblk0 -> /dev/block/mmcblk0
lrwxrwxrwx 1 root root 23 2010-01-01 01:00 mmcblk0boot0 -> /dev/block/mmcblk0boot0
lrwxrwxrwx 1 root root 23 2010-01-01 01:00 mmcblk0boot1 -> /dev/block/mmcblk0boot1
lrwxrwxrwx 1 root root 22 2010-01-01 01:00 mmcblk0rpmb -> /dev/block/mmcblk0rpmb
lrwxrwxrwx 1 root root 20 2010-01-01 01:00 nvcfg -> /dev/block/mmcblk0p5
lrwxrwxrwx 1 root root 20 2010-01-01 01:00 nvdata -> /dev/block/mmcblk0p6
lrwxrwxrwx 1 root root 21 2010-01-01 01:00 nvram -> /dev/block/mmcblk0p14
lrwxrwxrwx 1 root root 20 2010-01-01 01:00 para -> /dev/block/mmcblk0p2
lrwxrwxrwx 1 root root 15 2010-01-01 01:00 preloader_raw_a -> /dev/block/dm-7
lrwxrwxrwx 1 root root 15 2010-01-01 01:00 preloader_raw_b -> /dev/block/dm-9
lrwxrwxrwx 1 root root 21 2010-01-01 01:00 proinfo -> /dev/block/mmcblk0p13
lrwxrwxrwx 1 root root 20 2010-01-01 01:00 protect1 -> /dev/block/mmcblk0p9
lrwxrwxrwx 1 root root 21 2010-01-01 01:00 protect2 -> /dev/block/mmcblk0p10
lrwxrwxrwx 1 root root 21 2010-01-01 01:00 sec1 -> /dev/block/mmcblk0p12
lrwxrwxrwx 1 root root 21 2010-01-01 01:00 seccfg -> /dev/block/mmcblk0p11
lrwxrwxrwx 1 root root 21 2010-01-01 01:00 spmfw_a -> /dev/block/mmcblk0p21
lrwxrwxrwx 1 root root 21 2010-01-01 01:00 spmfw_b -> /dev/block/mmcblk0p35
lrwxrwxrwx 1 root root 21 2010-01-01 01:00 super -> /dev/block/mmcblk0p44
lrwxrwxrwx 1 root root 21 2010-01-01 01:00 tee_a -> /dev/block/mmcblk0p29
lrwxrwxrwx 1 root root 21 2010-01-01 01:00 tee_b -> /dev/block/mmcblk0p43
lrwxrwxrwx 1 root root 21 2010-01-01 01:00 userdata -> /dev/block/mmcblk0p45
lrwxrwxrwx 1 root root 21 2010-01-01 01:00 vbmeta_a -> /dev/block/mmcblk0p16
lrwxrwxrwx 1 root root 21 2010-01-01 01:00 vbmeta_b -> /dev/block/mmcblk0p30
lrwxrwxrwx 1 root root 21 2010-01-01 01:00 vbmeta_system_a -> /dev/block/mmcblk0p17
lrwxrwxrwx 1 root root 21 2010-01-01 01:00 vbmeta_system_b -> /dev/block/mmcblk0p31
lrwxrwxrwx 1 root root 21 2010-01-01 01:00 vbmeta_vendor_a -> /dev/block/mmcblk0p18
lrwxrwxrwx 1 root root 21 2010-01-01 01:00 vbmeta_vendor_b -> /dev/block/mmcblk0p32
lrwxrwxrwx 1 root root 21 2010-01-01 01:00 vendor_boot_a -> /dev/block/mmcblk0p27
lrwxrwxrwx 1 root root 21 2010-01-01 01:00 vendor_boot_b -> /dev/block/mmcblk0p41
```

### Manual tests still needed

- [ ] Identify keypad Android KeyEvents and Linux event codes (see instructions below).
- [ ] Compare `dumpsys display` when flipped open and closed; confirm secondary display behavior.
- [ ] Identify whether lid state is delivered by switch, Hall sensor, or vendor service.
- [ ] Test HOME selection with a separately installed launcher and reversible rollback.
- [ ] Test focus/navigation with D-pad, call/end keys, back key and T9 keyboard.
- [ ] Determine how phone, SMS, camera and emergency calling work without replacing system apps.

<!-- END MORITZ-FLIPOS-SCAN -->

## Before publishing to GitHub

Review the regenerated file for personal information, unusual device identifiers, app/package names that identify accounts or installed services, and any accidental paths. Do **not** publish a complete `getprop`, `bugreport`, `logcat`, `/data` extraction, NVRAM/NVDATA partition dump or full stock image. Share only sanitized diagnostic data and documentation.

## Technical sources

- [Android Debug Bridge](https://developer.android.com/tools/adb)
- [Android dumpsys](https://developer.android.com/tools/dumpsys)
- [AOSP getevent](https://source.android.com/docs/core/interaction/input/getevent)
- [AOSP keylayout files](https://source.android.com/docs/core/interaction/input/key-layout-files)
