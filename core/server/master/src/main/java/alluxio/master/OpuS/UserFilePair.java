package alluxio.master.OpuS;

/**
 * Created by yyuau on 13/7/2017.
 */
public class UserFilePair<K, V> {
  public K key;
  public V value;

  public K getKey() {
    return this.key;
  }

  public V getValue() {
    return this.value;
  }

  public UserFilePair(K var1, V var2) {
    this.key = var1;
    this.value = var2;
  }

  public String toString() {
    return this.key + "=" + this.value;
  }

  public int hashCode() {
    return this.key.hashCode() * 13 + (this.value == null?0:this.value.hashCode());
  }

  public boolean equals(Object var1) {
    if(this == var1) {
      return true;
    } else if(!(var1 instanceof alluxio.master.OpuS.UserFilePair)) {
      return false;
    } else {
      UserFilePair var2 = (UserFilePair)var1;
      if(this.key != null) {
        if(!this.key.equals(var2.key)) {
          return false;
        }
      } else if(var2.key != null) {
        return false;
      }

      if(this.value != null) {
        if(!this.value.equals(var2.value)) {
          return false;
        }
      } else if(var2.value != null) {
        return false;
      }

      return true;
    }
  }
}
