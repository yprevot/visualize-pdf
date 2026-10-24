import AppKit

func renderSVGToPNG(svgString: String, width: Int, height: Int, outputPath: String) {
    guard let data = svgString.data(using: .utf8),
          let image = NSImage(data: data) else {
        print("Error: Could not parse SVG")
        return
    }
    
    guard let rep = NSBitmapImageRep(
        bitmapDataPlanes: nil,
        pixelsWide: width,
        pixelsHigh: height,
        bitsPerSample: 8,
        samplesPerPixel: 4,
        hasAlpha: true,
        isPlanar: false,
        colorSpaceName: .deviceRGB,
        bytesPerRow: 0,
        bitsPerPixel: 0
    ) else {
        print("Error: Could not allocate NSBitmapImageRep")
        return
    }
    
    rep.size = NSSize(width: width, height: height)
    NSGraphicsContext.saveGraphicsState()
    NSGraphicsContext.current = NSGraphicsContext(bitmapImageRep: rep)
    
    // Draw SVG
    image.draw(in: NSRect(x: 0, y: 0, width: width, height: height))
    
    NSGraphicsContext.restoreGraphicsState()
    
    guard let pngData = rep.representation(using: .png, properties: [:]) else {
        print("Error: Could not encode PNG")
        return
    }
    
    do {
        try pngData.write(to: URL(fileURLWithPath: outputPath))
        print("Generated: \(outputPath) (\(width)x\(height), \(pngData.count) bytes)")
    } catch {
        print("Error saving PNG: \(error)")
    }
}

// 1. Google Play App Icon (512x512)
let iconSVG = """
<svg xmlns="http://www.w3.org/2000/svg" width="512" height="512" viewBox="0 0 512 512">
  <defs>
    <linearGradient id="bgGrad" x1="0%" y1="0%" x2="100%" y2="100%">
      <stop offset="0%" stop-color="#1E88E5"/>
      <stop offset="50%" stop-color="#1565C0"/>
      <stop offset="100%" stop-color="#0D47A1"/>
    </linearGradient>
    <linearGradient id="cornerGrad" x1="0%" y1="0%" x2="100%" y2="100%">
      <stop offset="0%" stop-color="#4DD0E1"/>
      <stop offset="100%" stop-color="#00ACC1"/>
    </linearGradient>
    <filter id="cardShadow" x="-10%" y="-10%" width="125%" height="125%">
      <feDropShadow dx="0" dy="12" stdDeviation="16" flood-color="#000000" flood-opacity="0.32"/>
    </filter>
    <filter id="badgeShadow" x="-10%" y="-10%" width="125%" height="125%">
      <feDropShadow dx="0" dy="4" stdDeviation="6" flood-color="#B71C1C" flood-opacity="0.4"/>
    </filter>
  </defs>

  <!-- Full bleed background for Google Play -->
  <rect width="512" height="512" fill="url(#bgGrad)"/>

  <!-- Subtle background glow -->
  <circle cx="256" cy="160" r="220" fill="#FFFFFF" opacity="0.04"/>

  <!-- Document Container with drop shadow (Scale 108 -> ~340) -->
  <g transform="translate(86, 60)" filter="url(#cardShadow)">
    <!-- White Document Body -->
    <path fill="#FFFFFF" d="M30,0 h180 l100,100 v270 c0,18 -14,32 -32,32 H30 c-18,0 -32,-14 -32,-32 V32 c0,-18 14,-32 32,-32 z"/>

    <!-- Folded Corner Accent -->
    <path fill="url(#cornerGrad)" d="M210,0 v100 h100 z"/>
    <path fill="#000000" opacity="0.12" d="M210,100 l-15,15 h115 z"/>

    <!-- Red Accent Header Badge inside Document -->
    <g filter="url(#badgeShadow)">
      <rect x="24" y="130" width="232" height="74" rx="10" fill="#E53935"/>
      <!-- 'PDF' Bold Text -->
      <text x="140" y="184" font-family="-apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, Helvetica, Arial, sans-serif" font-size="52" font-weight="900" fill="#FFFFFF" text-anchor="middle" letter-spacing="4">PDF</text>
    </g>

    <!-- Document Content Lines -->
    <rect x="24" y="240" width="232" height="16" rx="8" fill="#CFD8DC"/>
    <rect x="24" y="274" width="180" height="16" rx="8" fill="#CFD8DC"/>
    <rect x="24" y="308" width="210" height="16" rx="8" fill="#CFD8DC"/>
    <rect x="24" y="342" width="130" height="16" rx="8" fill="#90A4AE"/>
  </g>
</svg>
"""

renderSVGToPNG(svgString: iconSVG, width: 512, height: 512, outputPath: "docs/assets/playstore/app_icon_512.png")
renderSVGToPNG(svgString: iconSVG, width: 512, height: 512, outputPath: "docs/assets/app_icon_512.png")

// 2. Google Play Feature Graphic (1024x500)
let bannerSVG = """
<svg xmlns="http://www.w3.org/2000/svg" width="1024" height="500" viewBox="0 0 1024 500">
  <defs>
    <linearGradient id="bannerBg" x1="0%" y1="0%" x2="100%" y2="100%">
      <stop offset="0%" stop-color="#0A2540"/>
      <stop offset="45%" stop-color="#0D47A1"/>
      <stop offset="85%" stop-color="#1565C0"/>
      <stop offset="100%" stop-color="#1976D2"/>
    </linearGradient>
    <linearGradient id="iconDocCorner" x1="0%" y1="0%" x2="100%" y2="100%">
      <stop offset="0%" stop-color="#4DD0E1"/>
      <stop offset="100%" stop-color="#00ACC1"/>
    </linearGradient>
    <filter id="bannerShadow" x="-20%" y="-20%" width="140%" height="140%">
      <feDropShadow dx="0" dy="16" stdDeviation="24" flood-color="#000000" flood-opacity="0.45"/>
    </filter>
  </defs>

  <!-- Background -->
  <rect width="1024" height="500" fill="url(#bannerBg)"/>

  <!-- Decorative circles / grid glow -->
  <circle cx="850" cy="100" r="300" fill="#00B0FF" opacity="0.08"/>
  <circle cx="200" cy="450" r="250" fill="#2979FF" opacity="0.12"/>
  <circle cx="950" cy="400" r="180" fill="#FFFFFF" opacity="0.04"/>

  <!-- Left Side: App Icon Graphic (280x360) -->
  <g transform="translate(90, 70)" filter="url(#bannerShadow)">
    <rect width="250" height="330" rx="20" fill="#FFFFFF"/>
    <!-- Folded Corner -->
    <path fill="url(#iconDocCorner)" d="M175,0 v75 h75 z"/>
    <path fill="#000000" opacity="0.12" d="M175,75 l-12,12 h87 z"/>

    <!-- Red PDF Header Badge -->
    <rect x="22" y="105" width="206" height="66" rx="8" fill="#E53935"/>
    <text x="125" y="154" font-family="-apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, Helvetica, Arial, sans-serif" font-size="46" font-weight="900" fill="#FFFFFF" text-anchor="middle" letter-spacing="3">PDF</text>

    <!-- Document Lines -->
    <rect x="22" y="200" width="206" height="14" rx="7" fill="#CFD8DC"/>
    <rect x="22" y="230" width="160" height="14" rx="7" fill="#CFD8DC"/>
    <rect x="22" y="260" width="186" height="14" rx="7" fill="#CFD8DC"/>
    <rect x="22" y="290" width="110" height="14" rx="7" fill="#90A4AE"/>
  </g>

  <!-- Right Side: Clean Typography & Highlights -->
  <g transform="translate(400, 135)">
    <!-- Brand / Title -->
    <text x="0" y="45" font-family="-apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, Helvetica, Arial, sans-serif" font-size="54" font-weight="900" fill="#FFFFFF" letter-spacing="-1">Visualize PDF</text>

    <!-- Subtitle / Tagline -->
    <text x="0" y="95" font-family="-apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, Helvetica, Arial, sans-serif" font-size="24" font-weight="500" fill="#E3F2FD">Visor de PDF Rápido, Ligero y 100% Privado</text>

    <!-- Feature Pills -->
    <g transform="translate(0, 140)">
      <!-- Pill 1: Motor Híbrido -->
      <rect x="0" y="0" width="160" height="38" rx="19" fill="#FFFFFF" opacity="0.15"/>
      <text x="80" y="24" font-family="-apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, Helvetica, Arial, sans-serif" font-size="14" font-weight="600" fill="#FFFFFF" text-anchor="middle">⚡ Motor Híbrido</text>

      <!-- Pill 2: 100% Local -->
      <rect x="175" y="0" width="150" height="38" rx="19" fill="#FFFFFF" opacity="0.15"/>
      <text x="250" y="24" font-family="-apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, Helvetica, Arial, sans-serif" font-size="14" font-weight="600" fill="#FFFFFF" text-anchor="middle">🔒 100% Offline</text>

      <!-- Pill 3: Sin Anuncios -->
      <rect x="340" y="0" width="155" height="38" rx="19" fill="#FFFFFF" opacity="0.15"/>
      <text x="417" y="24" font-family="-apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, Helvetica, Arial, sans-serif" font-size="14" font-weight="600" fill="#FFFFFF" text-anchor="middle">🚫 Sin Anuncios</text>
    </g>

    <!-- Tech spec footer note -->
    <text x="0" y="225" font-family="-apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, Helvetica, Arial, sans-serif" font-size="14" font-weight="400" fill="#90CAF9">Optimizado para Android 16 (API 36) &#8226; Código Abierto</text>
  </g>
</svg>
"""

renderSVGToPNG(svgString: bannerSVG, width: 1024, height: 500, outputPath: "docs/assets/playstore/feature_graphic_1024x500.png")
renderSVGToPNG(svgString: bannerSVG, width: 1024, height: 500, outputPath: "docs/assets/feature_graphic_1024x500.png")

print("Done rendering all store assets!")
