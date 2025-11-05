import com.tbd.service.EncryptionService

fun main() {
    val masterKey = EncryptionService.generateMasterKey()
    println("=".repeat(60))
    println("MASTER ENCRYPTION KEY")
    println("=".repeat(60))
    println()
    println("Add this to your .env file:")
    println("MASTER_ENCRYPTION_KEY=$masterKey")
    println()
    println("⚠️  WARNING: Keep this key secure! Never commit it to git.")
    println("=".repeat(60))
}

