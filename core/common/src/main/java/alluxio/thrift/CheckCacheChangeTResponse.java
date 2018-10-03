/**
 * Autogenerated by Thrift Compiler (0.9.3)
 *
 * DO NOT EDIT UNLESS YOU ARE SURE THAT YOU KNOW WHAT YOU ARE DOING
 *  @generated
 */
package alluxio.thrift;

import org.apache.thrift.scheme.IScheme;
import org.apache.thrift.scheme.SchemeFactory;
import org.apache.thrift.scheme.StandardScheme;

import org.apache.thrift.scheme.TupleScheme;
import org.apache.thrift.protocol.TTupleProtocol;
import org.apache.thrift.protocol.TProtocolException;
import org.apache.thrift.EncodingUtils;
import org.apache.thrift.TException;
import org.apache.thrift.async.AsyncMethodCallback;
import org.apache.thrift.server.AbstractNonblockingServer.*;
import java.util.List;
import java.util.ArrayList;
import java.util.Map;
import java.util.HashMap;
import java.util.EnumMap;
import java.util.Set;
import java.util.HashSet;
import java.util.EnumSet;
import java.util.Collections;
import java.util.BitSet;
import java.nio.ByteBuffer;
import java.util.Arrays;
import javax.annotation.Generated;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@SuppressWarnings({"cast", "rawtypes", "serial", "unchecked"})
@Generated(value = "Autogenerated by Thrift Compiler (0.9.3)")
public class CheckCacheChangeTResponse implements org.apache.thrift.TBase<CheckCacheChangeTResponse, CheckCacheChangeTResponse._Fields>, java.io.Serializable, Cloneable, Comparable<CheckCacheChangeTResponse> {
  private static final org.apache.thrift.protocol.TStruct STRUCT_DESC = new org.apache.thrift.protocol.TStruct("CheckCacheChangeTResponse");

  private static final org.apache.thrift.protocol.TField CACHING_LIST_FIELD_DESC = new org.apache.thrift.protocol.TField("cachingList", org.apache.thrift.protocol.TType.LIST, (short)1);

  private static final Map<Class<? extends IScheme>, SchemeFactory> schemes = new HashMap<Class<? extends IScheme>, SchemeFactory>();
  static {
    schemes.put(StandardScheme.class, new CheckCacheChangeTResponseStandardSchemeFactory());
    schemes.put(TupleScheme.class, new CheckCacheChangeTResponseTupleSchemeFactory());
  }

  private List<String> cachingList; // required

  /** The set of fields this struct contains, along with convenience methods for finding and manipulating them. */
  public enum _Fields implements org.apache.thrift.TFieldIdEnum {
    CACHING_LIST((short)1, "cachingList");

    private static final Map<String, _Fields> byName = new HashMap<String, _Fields>();

    static {
      for (_Fields field : EnumSet.allOf(_Fields.class)) {
        byName.put(field.getFieldName(), field);
      }
    }

    /**
     * Find the _Fields constant that matches fieldId, or null if its not found.
     */
    public static _Fields findByThriftId(int fieldId) {
      switch(fieldId) {
        case 1: // CACHING_LIST
          return CACHING_LIST;
        default:
          return null;
      }
    }

    /**
     * Find the _Fields constant that matches fieldId, throwing an exception
     * if it is not found.
     */
    public static _Fields findByThriftIdOrThrow(int fieldId) {
      _Fields fields = findByThriftId(fieldId);
      if (fields == null) throw new IllegalArgumentException("Field " + fieldId + " doesn't exist!");
      return fields;
    }

    /**
     * Find the _Fields constant that matches name, or null if its not found.
     */
    public static _Fields findByName(String name) {
      return byName.get(name);
    }

    private final short _thriftId;
    private final String _fieldName;

    _Fields(short thriftId, String fieldName) {
      _thriftId = thriftId;
      _fieldName = fieldName;
    }

    public short getThriftFieldId() {
      return _thriftId;
    }

    public String getFieldName() {
      return _fieldName;
    }
  }

  // isset id assignments
  public static final Map<_Fields, org.apache.thrift.meta_data.FieldMetaData> metaDataMap;
  static {
    Map<_Fields, org.apache.thrift.meta_data.FieldMetaData> tmpMap = new EnumMap<_Fields, org.apache.thrift.meta_data.FieldMetaData>(_Fields.class);
    tmpMap.put(_Fields.CACHING_LIST, new org.apache.thrift.meta_data.FieldMetaData("cachingList", org.apache.thrift.TFieldRequirementType.DEFAULT, 
        new org.apache.thrift.meta_data.ListMetaData(org.apache.thrift.protocol.TType.LIST, 
            new org.apache.thrift.meta_data.FieldValueMetaData(org.apache.thrift.protocol.TType.STRING))));
    metaDataMap = Collections.unmodifiableMap(tmpMap);
    org.apache.thrift.meta_data.FieldMetaData.addStructMetaDataMap(CheckCacheChangeTResponse.class, metaDataMap);
  }

  public CheckCacheChangeTResponse() {
  }

  public CheckCacheChangeTResponse(
    List<String> cachingList)
  {
    this();
    this.cachingList = cachingList;
  }

  /**
   * Performs a deep copy on <i>other</i>.
   */
  public CheckCacheChangeTResponse(CheckCacheChangeTResponse other) {
    if (other.isSetCachingList()) {
      List<String> __this__cachingList = new ArrayList<String>(other.cachingList);
      this.cachingList = __this__cachingList;
    }
  }

  public CheckCacheChangeTResponse deepCopy() {
    return new CheckCacheChangeTResponse(this);
  }

  @Override
  public void clear() {
    this.cachingList = null;
  }

  public int getCachingListSize() {
    return (this.cachingList == null) ? 0 : this.cachingList.size();
  }

  public java.util.Iterator<String> getCachingListIterator() {
    return (this.cachingList == null) ? null : this.cachingList.iterator();
  }

  public void addToCachingList(String elem) {
    if (this.cachingList == null) {
      this.cachingList = new ArrayList<String>();
    }
    this.cachingList.add(elem);
  }

  public List<String> getCachingList() {
    return this.cachingList;
  }

  public CheckCacheChangeTResponse setCachingList(List<String> cachingList) {
    this.cachingList = cachingList;
    return this;
  }

  public void unsetCachingList() {
    this.cachingList = null;
  }

  /** Returns true if field cachingList is set (has been assigned a value) and false otherwise */
  public boolean isSetCachingList() {
    return this.cachingList != null;
  }

  public void setCachingListIsSet(boolean value) {
    if (!value) {
      this.cachingList = null;
    }
  }

  public void setFieldValue(_Fields field, Object value) {
    switch (field) {
    case CACHING_LIST:
      if (value == null) {
        unsetCachingList();
      } else {
        setCachingList((List<String>)value);
      }
      break;

    }
  }

  public Object getFieldValue(_Fields field) {
    switch (field) {
    case CACHING_LIST:
      return getCachingList();

    }
    throw new IllegalStateException();
  }

  /** Returns true if field corresponding to fieldID is set (has been assigned a value) and false otherwise */
  public boolean isSet(_Fields field) {
    if (field == null) {
      throw new IllegalArgumentException();
    }

    switch (field) {
    case CACHING_LIST:
      return isSetCachingList();
    }
    throw new IllegalStateException();
  }

  @Override
  public boolean equals(Object that) {
    if (that == null)
      return false;
    if (that instanceof CheckCacheChangeTResponse)
      return this.equals((CheckCacheChangeTResponse)that);
    return false;
  }

  public boolean equals(CheckCacheChangeTResponse that) {
    if (that == null)
      return false;

    boolean this_present_cachingList = true && this.isSetCachingList();
    boolean that_present_cachingList = true && that.isSetCachingList();
    if (this_present_cachingList || that_present_cachingList) {
      if (!(this_present_cachingList && that_present_cachingList))
        return false;
      if (!this.cachingList.equals(that.cachingList))
        return false;
    }

    return true;
  }

  @Override
  public int hashCode() {
    List<Object> list = new ArrayList<Object>();

    boolean present_cachingList = true && (isSetCachingList());
    list.add(present_cachingList);
    if (present_cachingList)
      list.add(cachingList);

    return list.hashCode();
  }

  @Override
  public int compareTo(CheckCacheChangeTResponse other) {
    if (!getClass().equals(other.getClass())) {
      return getClass().getName().compareTo(other.getClass().getName());
    }

    int lastComparison = 0;

    lastComparison = Boolean.valueOf(isSetCachingList()).compareTo(other.isSetCachingList());
    if (lastComparison != 0) {
      return lastComparison;
    }
    if (isSetCachingList()) {
      lastComparison = org.apache.thrift.TBaseHelper.compareTo(this.cachingList, other.cachingList);
      if (lastComparison != 0) {
        return lastComparison;
      }
    }
    return 0;
  }

  public _Fields fieldForId(int fieldId) {
    return _Fields.findByThriftId(fieldId);
  }

  public void read(org.apache.thrift.protocol.TProtocol iprot) throws org.apache.thrift.TException {
    schemes.get(iprot.getScheme()).getScheme().read(iprot, this);
  }

  public void write(org.apache.thrift.protocol.TProtocol oprot) throws org.apache.thrift.TException {
    schemes.get(oprot.getScheme()).getScheme().write(oprot, this);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder("CheckCacheChangeTResponse(");
    boolean first = true;

    sb.append("cachingList:");
    if (this.cachingList == null) {
      sb.append("null");
    } else {
      sb.append(this.cachingList);
    }
    first = false;
    sb.append(")");
    return sb.toString();
  }

  public void validate() throws org.apache.thrift.TException {
    // check for required fields
    // check for sub-struct validity
  }

  private void writeObject(java.io.ObjectOutputStream out) throws java.io.IOException {
    try {
      write(new org.apache.thrift.protocol.TCompactProtocol(new org.apache.thrift.transport.TIOStreamTransport(out)));
    } catch (org.apache.thrift.TException te) {
      throw new java.io.IOException(te);
    }
  }

  private void readObject(java.io.ObjectInputStream in) throws java.io.IOException, ClassNotFoundException {
    try {
      read(new org.apache.thrift.protocol.TCompactProtocol(new org.apache.thrift.transport.TIOStreamTransport(in)));
    } catch (org.apache.thrift.TException te) {
      throw new java.io.IOException(te);
    }
  }

  private static class CheckCacheChangeTResponseStandardSchemeFactory implements SchemeFactory {
    public CheckCacheChangeTResponseStandardScheme getScheme() {
      return new CheckCacheChangeTResponseStandardScheme();
    }
  }

  private static class CheckCacheChangeTResponseStandardScheme extends StandardScheme<CheckCacheChangeTResponse> {

    public void read(org.apache.thrift.protocol.TProtocol iprot, CheckCacheChangeTResponse struct) throws org.apache.thrift.TException {
      org.apache.thrift.protocol.TField schemeField;
      iprot.readStructBegin();
      while (true)
      {
        schemeField = iprot.readFieldBegin();
        if (schemeField.type == org.apache.thrift.protocol.TType.STOP) { 
          break;
        }
        switch (schemeField.id) {
          case 1: // CACHING_LIST
            if (schemeField.type == org.apache.thrift.protocol.TType.LIST) {
              {
                org.apache.thrift.protocol.TList _list0 = iprot.readListBegin();
                struct.cachingList = new ArrayList<String>(_list0.size);
                String _elem1;
                for (int _i2 = 0; _i2 < _list0.size; ++_i2)
                {
                  _elem1 = iprot.readString();
                  struct.cachingList.add(_elem1);
                }
                iprot.readListEnd();
              }
              struct.setCachingListIsSet(true);
            } else { 
              org.apache.thrift.protocol.TProtocolUtil.skip(iprot, schemeField.type);
            }
            break;
          default:
            org.apache.thrift.protocol.TProtocolUtil.skip(iprot, schemeField.type);
        }
        iprot.readFieldEnd();
      }
      iprot.readStructEnd();

      // check for required fields of primitive type, which can't be checked in the validate method
      struct.validate();
    }

    public void write(org.apache.thrift.protocol.TProtocol oprot, CheckCacheChangeTResponse struct) throws org.apache.thrift.TException {
      struct.validate();

      oprot.writeStructBegin(STRUCT_DESC);
      if (struct.cachingList != null) {
        oprot.writeFieldBegin(CACHING_LIST_FIELD_DESC);
        {
          oprot.writeListBegin(new org.apache.thrift.protocol.TList(org.apache.thrift.protocol.TType.STRING, struct.cachingList.size()));
          for (String _iter3 : struct.cachingList)
          {
            oprot.writeString(_iter3);
          }
          oprot.writeListEnd();
        }
        oprot.writeFieldEnd();
      }
      oprot.writeFieldStop();
      oprot.writeStructEnd();
    }

  }

  private static class CheckCacheChangeTResponseTupleSchemeFactory implements SchemeFactory {
    public CheckCacheChangeTResponseTupleScheme getScheme() {
      return new CheckCacheChangeTResponseTupleScheme();
    }
  }

  private static class CheckCacheChangeTResponseTupleScheme extends TupleScheme<CheckCacheChangeTResponse> {

    @Override
    public void write(org.apache.thrift.protocol.TProtocol prot, CheckCacheChangeTResponse struct) throws org.apache.thrift.TException {
      TTupleProtocol oprot = (TTupleProtocol) prot;
      BitSet optionals = new BitSet();
      if (struct.isSetCachingList()) {
        optionals.set(0);
      }
      oprot.writeBitSet(optionals, 1);
      if (struct.isSetCachingList()) {
        {
          oprot.writeI32(struct.cachingList.size());
          for (String _iter4 : struct.cachingList)
          {
            oprot.writeString(_iter4);
          }
        }
      }
    }

    @Override
    public void read(org.apache.thrift.protocol.TProtocol prot, CheckCacheChangeTResponse struct) throws org.apache.thrift.TException {
      TTupleProtocol iprot = (TTupleProtocol) prot;
      BitSet incoming = iprot.readBitSet(1);
      if (incoming.get(0)) {
        {
          org.apache.thrift.protocol.TList _list5 = new org.apache.thrift.protocol.TList(org.apache.thrift.protocol.TType.STRING, iprot.readI32());
          struct.cachingList = new ArrayList<String>(_list5.size);
          String _elem6;
          for (int _i7 = 0; _i7 < _list5.size; ++_i7)
          {
            _elem6 = iprot.readString();
            struct.cachingList.add(_elem6);
          }
        }
        struct.setCachingListIsSet(true);
      }
    }
  }

}

