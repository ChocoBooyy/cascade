package dev.chocoboy.cascade.engine.tween;

/**
 * The interpolation curves used by size, alpha and color transitions. LINEAR is the identity; the ease-in and
 * ease-out variants accelerate or decelerate the transition across a particle's life.
 */
public enum Easings implements Easing {

    LINEAR {
        @Override
        public float ease(float t) {
            return t;
        }
    },
    EASE_IN_QUAD {
        @Override
        public float ease(float t) {
            return t * t;
        }
    },
    EASE_OUT_QUAD {
        @Override
        public float ease(float t) {
            return t * (2f - t);
        }
    },
    EASE_IN_OUT_QUAD {
        @Override
        public float ease(float t) {
            return t < 0.5f ? 2f * t * t : -1f + (4f - 2f * t) * t;
        }
    },
    EASE_IN_CUBIC {
        @Override
        public float ease(float t) {
            return t * t * t;
        }
    },
    EASE_OUT_CUBIC {
        @Override
        public float ease(float t) {
            float f = t - 1f;
            return f * f * f + 1f;
        }
    }
}
