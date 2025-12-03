package br.com.fatec.campusface.models
import com.google.cloud.firestore.annotation.Exclude
import org.springframework.security.core.GrantedAuthority
import org.springframework.security.core.userdetails.UserDetails
import java.time.Instant


data class User(
    val id: String = "",
    val fullName: String = "",
    val email: String = "",
    val hashedPassword: String = "", // será criptografada
    val document: String = "",
    val faceImageId: String? = "",
    val createdAt: Instant = Instant.now(),
    val updatedAt: Instant = Instant.now()
): UserDetails {
    @Exclude
    override fun getAuthorities(): Collection<GrantedAuthority> {
        return emptyList()
    }

    @Exclude
    override fun getPassword(): String = hashedPassword

    @Exclude
    override fun getUsername(): String = email

    @Exclude
    override fun isAccountNonExpired(): Boolean = true

    @Exclude
    override fun isAccountNonLocked(): Boolean = true

    @Exclude
    override fun isCredentialsNonExpired(): Boolean = true

    @Exclude
    override fun isEnabled(): Boolean = true

}


