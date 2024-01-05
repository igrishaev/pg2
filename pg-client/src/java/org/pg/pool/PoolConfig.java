package org.pg.pool;

public record PoolConfig (
        int minSize,
        int maxSize,
        int maxLifetime,
        System.Logger.Level logLevel
) {

    public static Builder builder () {
        return new Builder();
    }

    public static PoolConfig standard() {
        return PoolConfig.builder().build();
    }

    public final static class Builder {

        private int minSize = 2;
        private int maxSize = 8;
        private int maxLifetime = 1000 * 60 * 60;
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
        public Builder maxLifetime(final int maxLifetime) {
            this.maxLifetime = maxLifetime;
            return this;
        }

        public PoolConfig build () {
            return new PoolConfig(
                    minSize,
                    maxSize,
                    maxLifetime,
                    logLevel
            );
        }
    }
}


