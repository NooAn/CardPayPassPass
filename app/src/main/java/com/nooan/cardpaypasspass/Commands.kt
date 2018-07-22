package com.nooan.cardpaypasspass

data class Command(
        var CLA: String = 0x00.toString(), //	Instruction class - indicates the type of command, e.g. interindustry or proprietary, length
        var INS: String = 0x00.toString(), // Instruction code - indicates the specific command, e.g. "write data", length = 1
        var P1: String = "", // Instruction parameters for the command, e.g. offset into file at which to write the data, length = 1
        var P2: String = "", // Instruction parameters for the command, e.g. offset into file at which to write the data, length = 1
        var Lc: String = "", /*Encodes the number (Nc) of bytes of command data to follow
                                                0 bytes denotes Nc=0
                                                1 byte with a value from 1 to 255 denotes Nc with the same value
                                                3 bytes, the first of which must be 0, denotes Nc in the range 1 to 65 535 (all three bytes may not be zero).
                                                Number of bytes present is command data field, length = 0 or 1
                                                */
        var Nc: String = "",
        var Le: String = "", /*Encodes the maximum number (Ne) of response bytes expected
                                            0 bytes denotes Ne=0
                                            1 byte in the range 1 to 255 denotes that value of Ne, or 0 denotes Ne=256
                                            2 bytes (if extended Lc was present in the command) in the range 1 to 65 535 denotes Ne of that value, or two zero bytes denotes 65 536
                                            3 bytes (if Lc was not present in the command), the first of which must be 0, denote Ne in the same way as two-byte Le*/
        var Nr: String = "", // response data, FCI or empty
        var SW1WS2: String = "" // 9000 (success or specific status bytes
) {
    fun split(): ByteArray {
        return getHexString().hexToByteArray()
    }

    fun getHexString() = (CLA.plus(INS).plus(P1).plus(P2).plus(Lc).plus(Nc).plus(Le).plus(Nr).plus(SW1WS2))
}

object Value {
    val TAG = "Host Card Emulator"
    val STATUS_SUCCESS = "9000"
    val STATUS_FAILED = "6F00"
    val CLA_NOT_SUPPORTED = "6E00"
    val INS_NOT_SUPPORTED = "6D00"
    val AID = "A0000002471001"
    val SELECT_INS = "A4"
    val DEFAULT_CLA = "00"
    val MIN_APDU_LENGTH = 12
}

object Commands {
    val SELECT_PPSE = Command(CLA = "00", INS = "A4", P1 = "04", P2 = "00", Lc = "0E", Nc = "32 50 41 59 2E 53 59 53 2E 44 44 46 30 31 00")

    val SELECT_APPLICATION = Command(CLA = "00", INS = "A4", P1 = "04", P2 = "00", Nc = "07")

    val GET_PROCESSING_OPTIONS = Command(CLA = "80", INS = "A8", P1 = "00", P2 = "00", Lc = "02", Nc = "83 00", Le = "00")

    val READ_RECORD_1 = Command(CLA = "00", INS = "B2", P1 = "01", P2 = "14", Lc = "00", Le = "00")

    val READ_RECORD_2 = Command(CLA = "00", INS = "B2", P1 = "01", P2 = "1C", Lc = "00", Le = "00")

    val READ_RECORD_3 = Command(CLA = "00", INS = "B2", P1 = "01", P2 = "24", Lc = "00", Le = "00")

    val READ_RECORD_4 = Command(CLA = "00", INS = "B2", P1 = "02", P2 = "24", Lc = "00", Le = "00")

    val COMPUTE_CRYPTOGRAPHIC_CHECKSUM = Command(CLA = "80", INS = "2A", P1 = "8E", P2 = "80", Le = "00")
}