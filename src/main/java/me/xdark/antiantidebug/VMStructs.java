package me.xdark.antiantidebug;

import me.coley.recaf.util.OSUtil;

/**
 * You can use this class to recover VM structs if someone tries to protect VM by 'nulling'
 * gHotSpotVMStructs.
 */
public final class VMStructs {

  public static final long gHotSpotVMIntConstantEntryArrayStride;
  public static final long gHotSpotVMIntConstantEntryNameOffset;
  public static final long gHotSpotVMIntConstantEntryValueOffset;
  public static final long gHotSpotVMIntConstants;
  public static final long gHotSpotVMLongConstantEntryArrayStride;
  public static final long gHotSpotVMLongConstantEntryNameOffset;
  public static final long gHotSpotVMLongConstantEntryValueOffset;
  public static final long gHotSpotVMLongConstants;
  public static final long gHotSpotVMStructEntryAddressOffset;
  public static final long gHotSpotVMStructEntryArrayStride;
  public static final long gHotSpotVMStructEntryFieldNameOffset;
  public static final long gHotSpotVMStructEntryIsStaticOffset;
  public static final long gHotSpotVMStructEntryOffsetOffset;
  public static final long gHotSpotVMStructEntryTypeNameOffset;
  public static final long gHotSpotVMStructEntryTypeStringOffset;
  public static final long gHotSpotVMStructs;
  public static final long gHotSpotVMTypeEntryArrayStride;
  public static final long gHotSpotVMTypeEntryIsIntegerTypeOffset;
  public static final long gHotSpotVMTypeEntryIsOopTypeOffset;
  public static final long gHotSpotVMTypeEntryIsUnsignedOffset;
  public static final long gHotSpotVMTypeEntrySizeOffset;
  public static final long gHotSpotVMTypeEntrySuperclassNameOffset;
  public static final long gHotSpotVMTypeEntryTypeNameOffset;
  public static final long gHotSpotVMTypes;

  private VMStructs() {
  }

  static void init() {
  }

  static {
    if (OSUtil.getOSType() == OSUtil.MAC) {
      // Welp.
      gHotSpotVMIntConstantEntryArrayStride = 0L;
      gHotSpotVMIntConstantEntryNameOffset = 0L;
      gHotSpotVMIntConstantEntryValueOffset = 0L;
      gHotSpotVMIntConstants = 0L;
      gHotSpotVMLongConstantEntryArrayStride = 0L;
      gHotSpotVMLongConstantEntryNameOffset = 0L;
      gHotSpotVMLongConstantEntryValueOffset = 0L;
      gHotSpotVMLongConstants = 0L;
      gHotSpotVMStructEntryAddressOffset = 0L;
      gHotSpotVMStructEntryArrayStride = 0L;
      gHotSpotVMStructEntryFieldNameOffset = 0L;
      gHotSpotVMStructEntryIsStaticOffset = 0L;
      gHotSpotVMStructEntryOffsetOffset = 0L;
      gHotSpotVMStructEntryTypeNameOffset = 0L;
      gHotSpotVMStructEntryTypeStringOffset = 0L;
      gHotSpotVMStructs = 0L;
      gHotSpotVMTypeEntryArrayStride = 0L;
      gHotSpotVMTypeEntryIsIntegerTypeOffset = 0L;
      gHotSpotVMTypeEntryIsOopTypeOffset = 0L;
      gHotSpotVMTypeEntryIsUnsignedOffset = 0L;
      gHotSpotVMTypeEntrySizeOffset = 0L;
      gHotSpotVMTypeEntrySuperclassNameOffset = 0L;
      gHotSpotVMTypeEntryTypeNameOffset = 0L;
      gHotSpotVMTypes = 0L;
    } else {
      NativeLibrary jvm = NativeLibraryLoader.loadJvmLibrary();
      try {
        gHotSpotVMIntConstantEntryArrayStride = jvm.findEntry("gHotSpotVMIntConstantEntryArrayStride");
        gHotSpotVMIntConstantEntryNameOffset = jvm.findEntry("gHotSpotVMIntConstantEntryNameOffset");
        gHotSpotVMIntConstantEntryValueOffset = jvm.findEntry("gHotSpotVMIntConstantEntryValueOffset");
        gHotSpotVMIntConstants = jvm.findEntry("gHotSpotVMIntConstants");
        gHotSpotVMLongConstantEntryArrayStride = jvm.findEntry("gHotSpotVMLongConstantEntryArrayStride");
        gHotSpotVMLongConstantEntryNameOffset = jvm.findEntry("gHotSpotVMLongConstantEntryNameOffset");
        gHotSpotVMLongConstantEntryValueOffset = jvm.findEntry("gHotSpotVMLongConstantEntryValueOffset");
        gHotSpotVMLongConstants = jvm.findEntry("gHotSpotVMLongConstants");
        gHotSpotVMStructEntryAddressOffset = jvm.findEntry("gHotSpotVMStructEntryAddressOffset");
        gHotSpotVMStructEntryArrayStride = jvm.findEntry("gHotSpotVMStructEntryArrayStride");
        gHotSpotVMStructEntryFieldNameOffset = jvm.findEntry("gHotSpotVMStructEntryFieldNameOffset");
        gHotSpotVMStructEntryIsStaticOffset = jvm.findEntry("gHotSpotVMStructEntryIsStaticOffset");
        gHotSpotVMStructEntryOffsetOffset = jvm.findEntry("gHotSpotVMStructEntryOffsetOffset");
        gHotSpotVMStructEntryTypeNameOffset = jvm.findEntry("gHotSpotVMStructEntryTypeNameOffset");
        gHotSpotVMStructEntryTypeStringOffset = jvm.findEntry("gHotSpotVMStructEntryTypeStringOffset");
        gHotSpotVMStructs = jvm.findEntry("gHotSpotVMStructs");
        gHotSpotVMTypeEntryArrayStride = jvm.findEntry("gHotSpotVMTypeEntryArrayStride");
        gHotSpotVMTypeEntryIsIntegerTypeOffset = jvm.findEntry("gHotSpotVMTypeEntryIsIntegerTypeOffset");
        gHotSpotVMTypeEntryIsOopTypeOffset = jvm.findEntry("gHotSpotVMTypeEntryIsOopTypeOffset");
        gHotSpotVMTypeEntryIsUnsignedOffset = jvm.findEntry("gHotSpotVMTypeEntryIsUnsignedOffset");
        gHotSpotVMTypeEntrySizeOffset = jvm.findEntry("gHotSpotVMTypeEntrySizeOffset");
        gHotSpotVMTypeEntrySuperclassNameOffset = jvm.findEntry("gHotSpotVMTypeEntrySuperclassNameOffset");
        gHotSpotVMTypeEntryTypeNameOffset = jvm.findEntry("gHotSpotVMTypeEntryTypeNameOffset");
        gHotSpotVMTypes = jvm.findEntry("gHotSpotVMTypes");
      } finally {
        jvm.unload();
      }
    }
  }
}
