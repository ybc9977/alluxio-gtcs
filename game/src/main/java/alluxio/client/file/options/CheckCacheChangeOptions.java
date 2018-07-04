package alluxio.client.file.options;

import alluxio.security.User;
import alluxio.thrift.CheckCacheChangeTOptions;
import com.google.common.base.Objects;

public class CheckCacheChangeOptions {

    private boolean mImplement;

    private Long mUserId;

    public static CheckCacheChangeOptions defaults(Long user){
        return new CheckCacheChangeOptions(user);
    }

    private CheckCacheChangeOptions(Long user){
        super();
        mImplement=false;
        mUserId = user;
    }

    public CheckCacheChangeOptions(CheckCacheChangeTOptions options) {
        this(options.userId);
        mImplement = options.isImplement();
        mUserId =  options.getUserId();
    }

    public boolean ismImplement() {
        return mImplement;
    }

    public CheckCacheChangeOptions setmImplement(boolean Implement) {
        mImplement = Implement;
        mUserId = 0L;
        return this;
    }

    public long getmUserId() {
        return mUserId;
    }

    public CheckCacheChangeOptions setUserId(long UserId){
        mImplement = false;
        mUserId = UserId;
        return this;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof CheckCacheChangeOptions)) {
            return false;
        }
        CheckCacheChangeOptions that = (CheckCacheChangeOptions) o;
        return Objects.equal(mImplement, that.mImplement);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(mImplement);
    }

    @Override
    public String toString() {
        return Objects.toStringHelper(this).add("implement",mImplement).toString();
    }

    /**
     * @return Thrift representation of the options
     */
    public CheckCacheChangeTOptions toThrift() {
        CheckCacheChangeTOptions options = new CheckCacheChangeTOptions();
        options.setImplement(mImplement);
        options.setUserId(mUserId);
        return options;
    }

}
