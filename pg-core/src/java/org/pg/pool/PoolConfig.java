package org.pg.pool;

import org.pg.Const;

public record PoolConfig (
        int minSize,
        int maxSize,
        int msLifetime,
        System.Logger.Level logLevel
) {

    public static Builder builder () {
        return new Builder();
    }

    public static PoolConfig standard() {
        return PoolConfig.builder().build();
    }

    public final static class Builder {

        private int minSize = Const.POOL_SIZE_MIN;
        private int maxSize = Const.POOL_SIZE_MAX;
        private int msLifetime = Const.POOL_MAX_LIFETIME;
        private System.Logger.Level logLevel = System.Logger.Level.INFO;

        @SuppressWarnings("unused")
        public Builder logLevel(final System.Logger.Level level) {
            this.logLevel = level;
            return this;
        }

        @SuppressWarnings("unused")
        public Builder minSize(final int minSize) {
            this.minSize = minSize;
            return this;
        }

        @SuppressWarnings("unused")
        public Builder maxSize(final int maxSize) {
            this.maxSize = maxSize;
            return this;
        }

        @SuppressWarnings("unused")
        public Builder msLifetime(final int msLifetime) {
            this.msLifetime = msLifetime;
            return this;
        }

        public PoolConfig build () {
            return new PoolConfig(
                    minSize,
                    maxSize,
                    msLifetime,
                    logLevel
            );
        }
    }
}
