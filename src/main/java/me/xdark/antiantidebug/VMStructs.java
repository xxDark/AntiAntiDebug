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
                gHotSpotVMIntConstantEntryArrayStride = getAddress(jvm.findEntry("gHotSpotVMIntConstantEntryArrayStride"));
                gHotSpotVMIntConstantEntryNameOffset = getAddress(jvm.findEntry("gHotSpotVMIntConstantEntryNameOffset"));
                gHotSpotVMIntConstantEntryValueOffset = getAddress(jvm.findEntry("gHotSpotVMIntConstantEntryValueOffset"));
                gHotSpotVMIntConstants = getAddress(jvm.findEntry("gHotSpotVMIntConstants"));
                gHotSpotVMLongConstantEntryArrayStride = getAddress(jvm.findEntry("gHotSpotVMLongConstantEntryArrayStride"));
                gHotSpotVMLongConstantEntryNameOffset = getAddress(jvm.findEntry("gHotSpotVMLongConstantEntryNameOffset"));
                gHotSpotVMLongConstantEntryValueOffset = getAddress(jvm.findEntry("gHotSpotVMLongConstantEntryValueOffset"));
                gHotSpotVMLongConstants = getAddress(jvm.findEntry("gHotSpotVMLongConstants"));
                gHotSpotVMStructEntryAddressOffset = getAddress(jvm.findEntry("gHotSpotVMStructEntryAddressOffset"));
                gHotSpotVMStructEntryArrayStride = getAddress(jvm.findEntry("gHotSpotVMStructEntryArrayStride"));
                gHotSpotVMStructEntryFieldNameOffset = getAddress(jvm.findEntry("gHotSpotVMStructEntryFieldNameOffset"));
                gHotSpotVMStructEntryIsStaticOffset = getAddress(jvm.findEntry("gHotSpotVMStructEntryIsStaticOffset"));
                gHotSpotVMStructEntryOffsetOffset = getAddress(jvm.findEntry("gHotSpotVMStructEntryOffsetOffset"));
                gHotSpotVMStructEntryTypeNameOffset = getAddress(jvm.findEntry("gHotSpotVMStructEntryTypeNameOffset"));
                gHotSpotVMStructEntryTypeStringOffset = getAddress(jvm.findEntry("gHotSpotVMStructEntryTypeStringOffset"));
                gHotSpotVMStructs = getAddress(jvm.findEntry("gHotSpotVMStructs"));
                gHotSpotVMTypeEntryArrayStride = getAddress(jvm.findEntry("gHotSpotVMTypeEntryArrayStride"));
                gHotSpotVMTypeEntryIsIntegerTypeOffset = getAddress(jvm.findEntry("gHotSpotVMTypeEntryIsIntegerTypeOffset"));
                gHotSpotVMTypeEntryIsOopTypeOffset = getAddress(jvm.findEntry("gHotSpotVMTypeEntryIsOopTypeOffset"));
                gHotSpotVMTypeEntryIsUnsignedOffset = getAddress(jvm.findEntry("gHotSpotVMTypeEntryIsUnsignedOffset"));
                gHotSpotVMTypeEntrySizeOffset = getAddress(jvm.findEntry("gHotSpotVMTypeEntrySizeOffset"));
                gHotSpotVMTypeEntrySuperclassNameOffset = getAddress(jvm.findEntry("gHotSpotVMTypeEntrySuperclassNameOffset"));
                gHotSpotVMTypeEntryTypeNameOffset = getAddress(jvm.findEntry("gHotSpotVMTypeEntryTypeNameOffset"));
                gHotSpotVMTypes = getAddress(jvm.findEntry("gHotSpotVMTypes"));
            } finally {
                jvm.unload();
            }
        }
    }

    private VMStructs() {
    }

    static void init() {
    }
    
    private static long getAddress(long entry) { 
      return entry == 0L ? 0L : InternalsUtil.unsafe().getLong(entry); 
    }
}
