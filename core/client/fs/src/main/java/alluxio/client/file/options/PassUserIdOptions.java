package alluxio.client.file.options;

import alluxio.thrift.PassUserIdTOptions;
import com.google.common.base.Objects;

public class PassUserIdOptions {
    /**
     * @return the default {@link PassUserIdOptions}
     */
    public static PassUserIdOptions defaults() {
        return new PassUserIdOptions();
    }

    private PassUserIdOptions() {
        // No options currently
    }

    @Override
    public boolean equals(Object o) {
        return this == o || o instanceof PassUserIdOptions;
    }

    @Override
    public int hashCode() {
        return 0;
    }

    @Override
    public String toString() {
        return Objects.toStringHelper(this).toString();
    }

    /**
     * @return Thrift representation of the options
     */
    public PassUserIdTOptions toThrift() {
        PassUserIdTOptions options = new PassUserIdTOptions();
        return options;
    }
}
