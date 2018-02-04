/**
 * Write the `InputStream` into `File`
 * @param targetFile The target file will written to
 * @param autoClose true,will close the inputstream when written
 * @return Return true is written successfully, or else false
 */
fun InputStream.toFile(targetFile: File, autoClose: Boolean = false): Boolean {
    if (targetFile.exists() && !targetFile.delete()) {
        Log.w(this, "Cannot write file ${targetFile.absolutePath} because it was readonly.")
        return false
    }
    val outputStream = targetFile.outputStream()
    return try {
        outputStream.write(this.readBytes())
        true
    } catch (e: IOException) {
        false
    } finally {
        outputStream.close()
        if (autoClose)
            this.close()
    }
}
