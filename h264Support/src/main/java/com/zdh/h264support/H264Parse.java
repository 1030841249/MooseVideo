package com.zdh.h264support;

/**
 * author: ZDH
 * Date: 2022/3/23
 * Description:
 */
class H264Parse {
    // 位索引，所以需要在byte数组中索引时，要除以8
    public static int startIndex = 0;

    public static void parse(byte[] h264) {
        int forbidden_zero_bit=u(1, h264);
        int nal_ref_idc       =u(2, h264);
        int nal_unit_type     =u(5, h264);
        switch (nal_unit_type) {
            // ...
            case 7: // sps
                parseSPS(h264);
        }
    }

    private static void parseSPS(byte[] h264) {
        int profile_idc = u(8,h264);
    }

    /**
     * 无符号整数
     */
    public static int u(int bitCount,byte[] data) {
        int ret = 0;
        byte b;
        for(int i = 0; i < bitCount; i++) {
            b = data[startIndex / 8];
            ret <<= 1;
            if((b & (0x80 >> startIndex % 8)) != 0) ret += 1;
            startIndex++;
        }
        return ret;
    }

    /**
     * 无符号哥伦布编码
     */
    public static int Ue(byte[] data) {
        int zeroCount = 0;
        int length = data.length * 8;
        byte b;
        while(startIndex < length) {
            b = data[startIndex / 8];
            if((b & (0x80 >> startIndex % 8))!=0) break;
            zeroCount++;
            startIndex++;
        }
        startIndex++;
        int ret = 0;
        for(int i = 0; i < zeroCount; i++) {
            b = data[startIndex / 8];
            ret <<= 1;
            if((b & (0x80 >> startIndex % 8)) != 0) ret += 1;
            startIndex++;
        }
        // 算上第一个为 1 的比特位
//        int ret = u(zeroCount + 1,data) - 1;
        return ret;
    }
}
