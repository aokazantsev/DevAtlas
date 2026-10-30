package ru.aokazantsev.devatlas.themeformat.archive

import okio.FileHandle

internal class ZipDirectoryReader(private val handle: FileHandle) {

    fun entries(): List<ZipEntryRecord> {
        val size = handle.size()
        val tail = readTail(size)
        val end = (tail.size - END_RECORD_SIZE downTo 0).firstOrNull { tail.uInt32(it) == END_SIGNATURE }
            ?: throw ZipFormatException("не найден каталог zip-архива — файл повреждён или это не zip")
        val count = tail.uInt16(end + END_ENTRY_COUNT)
        val directorySize = tail.uInt32(end + END_DIRECTORY_SIZE)
        val directoryOffset = tail.uInt32(end + END_DIRECTORY_OFFSET)
        checkDirectory(count, directoryOffset, directorySize, size)
        val directory = ByteArray(directorySize.toInt())
        handle.read(directoryOffset, directory, 0, directory.size)
        return readEntries(directory, count)
    }

    fun localDataOffset(entry: ZipEntryRecord): Long {
        val header = ByteArray(LOCAL_HEADER_SIZE)
        if (handle.read(entry.localHeaderOffset, header, 0, LOCAL_HEADER_SIZE) != LOCAL_HEADER_SIZE ||
            header.uInt32(0) != LOCAL_SIGNATURE
        ) {
            throw ZipFormatException("запись «${entry.name}» повреждена")
        }
        return entry.localHeaderOffset + LOCAL_HEADER_SIZE + header.uInt16(LOCAL_NAME_LENGTH) + header.uInt16(LOCAL_EXTRA_LENGTH)
    }

    private fun readTail(size: Long): ByteArray {
        val tailSize = minOf(size, (END_RECORD_SIZE + MAX_COMMENT_SIZE).toLong()).toInt()
        if (tailSize < END_RECORD_SIZE) throw ZipFormatException("файл слишком мал для zip-архива")
        return ByteArray(tailSize).also { handle.read(size - tailSize, it, 0, tailSize) }
    }

    private fun checkDirectory(count: Int, offset: Long, directorySize: Long, size: Long) {
        if (count == ZIP64_MARKER_16 || offset == ZIP64_MARKER_32) {
            throw ZipFormatException("архив в формате ZIP64 не поддерживается")
        }
        if (offset + directorySize > size || directorySize > MAX_DIRECTORY_SIZE) {
            throw ZipFormatException("каталог zip-архива повреждён")
        }
    }

    private fun readEntries(directory: ByteArray, count: Int): List<ZipEntryRecord> {
        val entries = ArrayList<ZipEntryRecord>(count)
        var offset = 0
        repeat(count) {
            if (offset + CENTRAL_HEADER_SIZE > directory.size || directory.uInt32(offset) != CENTRAL_SIGNATURE) {
                throw ZipFormatException("каталог zip-архива повреждён")
            }
            val nameLength = directory.uInt16(offset + CENTRAL_NAME_LENGTH)
            val nameStart = offset + CENTRAL_HEADER_SIZE
            if (nameStart + nameLength > directory.size) throw ZipFormatException("каталог zip-архива повреждён")
            entries += readEntry(directory, offset, directory.copyOfRange(nameStart, nameStart + nameLength))
            offset = nameStart + nameLength +
                directory.uInt16(offset + CENTRAL_EXTRA_LENGTH) +
                directory.uInt16(offset + CENTRAL_COMMENT_LENGTH)
        }
        return entries
    }

    private fun readEntry(directory: ByteArray, offset: Int, nameBytes: ByteArray): ZipEntryRecord {
        val flags = directory.uInt16(offset + CENTRAL_FLAGS)
        val hostSystem = directory.uInt16(offset + CENTRAL_MADE_BY) ushr BYTE_BITS
        val fileType = (directory.uInt32(offset + CENTRAL_EXTERNAL_ATTRIBUTES) ushr SHORT_BITS) and FILE_TYPE_MASK
        return ZipEntryRecord(
            name = decodeName(nameBytes, utf8Flag = flags and UTF8_FLAG != 0),
            method = directory.uInt16(offset + CENTRAL_METHOD),
            encrypted = flags and ENCRYPTED_FLAG != 0,
            symlink = hostSystem == UNIX_HOST && fileType == SYMLINK_TYPE,
            compressedSize = directory.uInt32(offset + CENTRAL_COMPRESSED_SIZE),
            uncompressedSize = directory.uInt32(offset + CENTRAL_UNCOMPRESSED_SIZE),
            localHeaderOffset = directory.uInt32(offset + CENTRAL_LOCAL_OFFSET),
        )
    }

    private fun decodeName(bytes: ByteArray, utf8Flag: Boolean): String {
        val name = bytes.decodeToString()
        if (!utf8Flag && bytes.any { it < 0 } && REPLACEMENT_CHAR in name) {
            throw ZipFormatException("имя файла в архиве не в UTF-8 — назовите файлы латиницей или пересоберите архив")
        }
        return name
    }

    private fun ByteArray.uInt16(at: Int): Int = (this[at].toInt() and BYTE_MASK) or ((this[at + 1].toInt() and BYTE_MASK) shl BYTE_BITS)

    private fun ByteArray.uInt32(at: Int): Long = uInt16(at).toLong() or (uInt16(at + 2).toLong() shl SHORT_BITS)

    private companion object {
        const val END_SIGNATURE = 0x06054b50L
        const val CENTRAL_SIGNATURE = 0x02014b50L
        const val LOCAL_SIGNATURE = 0x04034b50L
        const val END_RECORD_SIZE = 22
        const val END_ENTRY_COUNT = 10
        const val END_DIRECTORY_SIZE = 12
        const val END_DIRECTORY_OFFSET = 16
        const val MAX_COMMENT_SIZE = 0xFFFF
        const val CENTRAL_HEADER_SIZE = 46
        const val CENTRAL_MADE_BY = 4
        const val CENTRAL_FLAGS = 8
        const val CENTRAL_METHOD = 10
        const val CENTRAL_COMPRESSED_SIZE = 20
        const val CENTRAL_UNCOMPRESSED_SIZE = 24
        const val CENTRAL_NAME_LENGTH = 28
        const val CENTRAL_EXTRA_LENGTH = 30
        const val CENTRAL_COMMENT_LENGTH = 32
        const val CENTRAL_EXTERNAL_ATTRIBUTES = 38
        const val CENTRAL_LOCAL_OFFSET = 42
        const val LOCAL_HEADER_SIZE = 30
        const val LOCAL_NAME_LENGTH = 26
        const val LOCAL_EXTRA_LENGTH = 28
        const val ZIP64_MARKER_16 = 0xFFFF
        const val ZIP64_MARKER_32 = 0xFFFFFFFFL
        const val MAX_DIRECTORY_SIZE = 16L * 1024 * 1024
        const val UTF8_FLAG = 0x0800
        const val ENCRYPTED_FLAG = 0x0001
        const val UNIX_HOST = 3
        const val FILE_TYPE_MASK = 0xF000L
        const val SYMLINK_TYPE = 0xA000L
        const val BYTE_MASK = 0xFF
        const val BYTE_BITS = 8
        const val SHORT_BITS = 16
        const val REPLACEMENT_CHAR = '\uFFFD'
    }
}
