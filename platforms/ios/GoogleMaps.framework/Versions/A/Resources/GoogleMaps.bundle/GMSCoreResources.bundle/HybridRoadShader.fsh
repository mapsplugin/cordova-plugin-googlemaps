// Because of this #extension this shader doesn't get precompiled by our
// precompiling script.
#extension GL_EXT_shader_framebuffer_fetch : require

uniform sampler2D sTexture0;
varying lowp vec2 vTextureCoord;
varying lowp vec4 vColor;
uniform mediump float uAlphaLodBias;
lowp vec4 gl_LastFragData[gl_MaxDrawBuffers];

void main() {
  // If we've drawn a stroke to the framebuffer the alpha value will be
  // HybridRoadShaderProgram::kDefaultAlpha == 0.6.  Choose 1/2 of this value
  // as a cutoff to delineate the complement rectangular area that surrounds
  // end caps. We do need to draw to these areas or we'll have tombstone shaped
  // stencil gaps around endcaps that haven't been filled.  If we have not
  // drawn a stroke to this fragment it will have the background satellite image
  // alpha.
  const lowp float kSatelliteBackgroundAlpha = 1.0;
  const lowp float kOneHalfOfStrokeAlpha = 0.3;
  const lowp float kStrokeAlpha = 0.6;
  if (gl_LastFragData[0].a == kSatelliteBackgroundAlpha ||
      gl_LastFragData[0].a < kOneHalfOfStrokeAlpha) {
    // Sample from alpha texture.  Add an lod bias to push sampling up closer
    // to the base level.
    lowp vec4 alpha_value = texture2D(sTexture0, vTextureCoord, uAlphaLodBias);
    mediump float alpha_scalar = vColor.a * kStrokeAlpha * alpha_value.a;
    gl_FragColor = vec4(mix(gl_LastFragData[0].rgb, vColor.rgb, alpha_scalar),
                        alpha_scalar);
  } else {
    gl_FragColor = gl_LastFragData[0];
  }
}
