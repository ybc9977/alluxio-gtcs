package alluxio.client.file.options;

import alluxio.thrift.RegisterUserTOptions;
import com.google.common.base.Objects;

public class RegisterUserOptions {
    /**
     * @return the default {@link }
     */
    public static RegisterUserOptions defaults() {
        return new RegisterUserOptions();
    }

    private RegisterUserOptions() {
        // No options currently
    }

    @Override
    public boolean equals(Object o) {
        return this == o || o instanceof RegisterUserOptions;
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
    public RegisterUserTOptions toThrift() {
        RegisterUserTOptions options = new RegisterUserTOptions();
        return options;
    }
}
