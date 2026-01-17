package com.ground.api.routes

import com.ground.dto.*
import com.ground.service.*
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.plugins.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import com.ground.middleware.CurrentUserIdKey
import com.typesafe.config.ConfigFactory
import io.jsonwebtoken.Jwts
import io.jsonwebtoken.security.Keys
import java.time.Instant
import java.util.*

/**
 * User authentication routes for RCAC.
 * 
 * These routes handle user registration, login, and profile management
 * using the new User model (separate from the legacy Account model).
 */
fun Application.userAuthRoutes() {
    val userService = UserService()
    val businessService = BusinessService()
    
    val config = ConfigFactory.load()
    val jwtSecret = System.getenv("JWT_SECRET") ?: config.getString("jwt.secret")
    val jwtKey = Keys.hmacShaKeyFor(jwtSecret.toByteArray())
    
    routing {
        route("/v1/users") {
            
            // ═══════════════════════════════════════════════════════════
            // PUBLIC ROUTES (no auth required)
            // ═══════════════════════════════════════════════════════════
            
            /**
             * Register a new user account
             */
            post("/register") {
                val request = call.receive<RegisterUserRequest>()
                val ipAddress = call.getClientIpAddress()
                val userAgent = call.request.headers["User-Agent"]
                
                try {
                    // Validate email format
                    if (!isValidEmail(request.email)) {
                        call.respond(
                            HttpStatusCode.BadRequest,
                            ErrorResponse(ErrorDetail("INVALID_EMAIL", "Invalid email format", "validation_error"))
                        )
                        return@post
                    }
                    
                    // Validate password strength
                    if (request.password.length < 8) {
                        call.respond(
                            HttpStatusCode.BadRequest,
                            ErrorResponse(ErrorDetail("WEAK_PASSWORD", "Password must be at least 8 characters", "validation_error"))
                        )
                        return@post
                    }
                    
                    val user = userService.createUser(
                        email = request.email,
                        password = request.password,
                        firstName = request.first_name,
                        lastName = request.last_name,
                        ipAddress = ipAddress,
                        userAgent = userAgent
                    )
                    
                    // Generate tokens
                    val (accessToken, expiresIn) = generateAccessToken(user.id.toString(), jwtKey)
                    val refreshToken = generateRefreshToken()
                    
                    // TODO: Store refresh token in database
                    
                    call.respond(
                        HttpStatusCode.Created,
                        LoginResponse(
                            access_token = accessToken,
                            refresh_token = refreshToken,
                            expires_in = expiresIn,
                            user = user.toUserResponse()
                        )
                    )
                } catch (e: IllegalArgumentException) {
                    call.respond(
                        HttpStatusCode.Conflict,
                        ErrorResponse(ErrorDetail("EMAIL_EXISTS", e.message ?: "Email already in use", "validation_error"))
                    )
                }
            }
            
            /**
             * Login with email and password
             */
            post("/login") {
                val request = call.receive<LoginRequest>()
                val ipAddress = call.getClientIpAddress()
                val userAgent = call.request.headers["User-Agent"]
                
                when (val result = userService.authenticate(
                    email = request.email,
                    password = request.password,
                    ipAddress = ipAddress,
                    userAgent = userAgent
                )) {
                    is AuthResult.Success -> {
                        val user = result.user
                        
                        // Check if MFA is required
                        if (user.mfaEnabled && request.mfa_code == null) {
                            call.respond(
                                HttpStatusCode.OK,
                                LoginResponse(
                                    access_token = "",
                                    refresh_token = "",
                                    expires_in = 0,
                                    user = user.toUserResponse(),
                                    mfa_required = true
                                )
                            )
                            return@post
                        }
                        
                        // TODO: Verify MFA code if provided
                        
                        // Generate tokens
                        val (accessToken, expiresIn) = generateAccessToken(user.id.toString(), jwtKey)
                        val refreshToken = generateRefreshToken()
                        
                        // TODO: Store refresh token in database
                        
                        call.respond(LoginResponse(
                            access_token = accessToken,
                            refresh_token = refreshToken,
                            expires_in = expiresIn,
                            user = user.toUserResponse()
                        ))
                    }
                    is AuthResult.InvalidCredentials -> {
                        call.respond(
                            HttpStatusCode.Unauthorized,
                            ErrorResponse(ErrorDetail("INVALID_CREDENTIALS", "Invalid email or password", "authentication_error"))
                        )
                    }
                    is AuthResult.AccountLocked -> {
                        call.respond(
                            HttpStatusCode.TooManyRequests,
                            ErrorResponse(ErrorDetail("ACCOUNT_LOCKED", "Account locked until ${result.until}", "authentication_error"))
                        )
                    }
                    is AuthResult.AccountInactive -> {
                        call.respond(
                            HttpStatusCode.Forbidden,
                            ErrorResponse(ErrorDetail("ACCOUNT_INACTIVE", "Account is inactive or deleted", "authentication_error"))
                        )
                    }
                    is AuthResult.MfaRequired -> {
                        // This case is handled above but included for completeness
                        call.respond(
                            HttpStatusCode.OK,
                            mapOf("mfa_required" to true)
                        )
                    }
                }
            }
            
            // ═══════════════════════════════════════════════════════════
            // AUTHENTICATED ROUTES
            // ═══════════════════════════════════════════════════════════
            
            authenticate("bearer-auth") {
                
                /**
                 * Get current user profile
                 */
                get("/me") {
                    val userId = call.getCurrentUserId() ?: return@get call.respondUnauthorized()
                    
                    val user = userService.getUser(userId)
                    if (user == null) {
                        call.respond(
                            HttpStatusCode.NotFound,
                            ErrorResponse(ErrorDetail("USER_NOT_FOUND", "User not found", "not_found"))
                        )
                        return@get
                    }
                    
                    // Get user's businesses
                    val businesses = businessService.listUserBusinesses(userId)
                    
                    call.respond(mapOf(
                        "user" to user.toUserResponse(),
                        "businesses" to businesses.map { it.toBusinessWithRolesResponse() }
                    ))
                }
                
                /**
                 * Update current user profile
                 */
                patch("/me") {
                    val userId = call.getCurrentUserId() ?: return@patch call.respondUnauthorized()
                    val request = call.receive<UpdateUserRequest>()
                    val ipAddress = call.getClientIpAddress()
                    
                    val user = userService.updateUser(
                        userId = userId,
                        firstName = request.first_name,
                        lastName = request.last_name,
                        avatarUrl = request.avatar_url,
                        ipAddress = ipAddress
                    )
                    
                    if (user == null) {
                        call.respond(
                            HttpStatusCode.NotFound,
                            ErrorResponse(ErrorDetail("USER_NOT_FOUND", "User not found", "not_found"))
                        )
                        return@patch
                    }
                    
                    call.respond(user.toUserResponse())
                }
                
                /**
                 * Change password
                 */
                post("/me/change-password") {
                    val userId = call.getCurrentUserId() ?: return@post call.respondUnauthorized()
                    val request = call.receive<ChangePasswordRequest>()
                    val ipAddress = call.getClientIpAddress()
                    
                    if (request.new_password.length < 8) {
                        call.respond(
                            HttpStatusCode.BadRequest,
                            ErrorResponse(ErrorDetail("WEAK_PASSWORD", "Password must be at least 8 characters", "validation_error"))
                        )
                        return@post
                    }
                    
                    val success = userService.changePassword(
                        userId = userId,
                        currentPassword = request.current_password,
                        newPassword = request.new_password,
                        ipAddress = ipAddress
                    )
                    
                    if (!success) {
                        call.respond(
                            HttpStatusCode.BadRequest,
                            ErrorResponse(ErrorDetail("INVALID_PASSWORD", "Current password is incorrect", "validation_error"))
                        )
                        return@post
                    }
                    
                    call.respond(mapOf("message" to "Password changed successfully"))
                }
                
                /**
                 * Delete account
                 */
                delete("/me") {
                    val userId = call.getCurrentUserId() ?: return@delete call.respondUnauthorized()
                    val request = call.receive<DeleteAccountRequest>()
                    val ipAddress = call.getClientIpAddress()
                    
                    val success = userService.deleteUser(
                        userId = userId,
                        password = request.password,
                        ipAddress = ipAddress
                    )
                    
                    if (!success) {
                        call.respond(
                            HttpStatusCode.BadRequest,
                            ErrorResponse(ErrorDetail("INVALID_PASSWORD", "Password is incorrect", "validation_error"))
                        )
                        return@delete
                    }
                    
                    call.respond(mapOf("message" to "Account scheduled for deletion"))
                }
                
                /**
                 * Logout - revoke tokens
                 */
                post("/logout") {
                    val userId = call.getCurrentUserId() ?: return@post call.respondUnauthorized()
                    
                    // TODO: Revoke refresh tokens
                    
                    call.respond(mapOf("message" to "Successfully logged out"))
                }
            }
        }
    }
}

// ═══════════════════════════════════════════════════════════════
// HELPER FUNCTIONS
// ═══════════════════════════════════════════════════════════════

private fun generateAccessToken(userId: String, key: javax.crypto.SecretKey): Pair<String, Int> {
    val expiresIn = 3600 // 1 hour
    val now = Instant.now()
    
    val token = Jwts.builder()
        .subject(userId)
        .issuedAt(Date.from(now))
        .expiration(Date.from(now.plusSeconds(expiresIn.toLong())))
        .signWith(key)
        .compact()
    
    return token to expiresIn
}

private fun generateRefreshToken(): String {
    return "grt_${UUID.randomUUID().toString().replace("-", "")}"
}

private fun isValidEmail(email: String): Boolean {
    val emailRegex = "^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\$".toRegex()
    return email.matches(emailRegex)
}

// ═══════════════════════════════════════════════════════════════
// EXTENSION FUNCTIONS FOR RESPONSE MAPPING
// ═══════════════════════════════════════════════════════════════

private fun com.ground.service.UserResponse.toUserResponse() = UserResponse(
    id = this.id.toString(),
    email = this.email,
    first_name = this.firstName,
    last_name = this.lastName,
    avatar_url = this.avatarUrl,
    status = this.status,
    email_verified = this.emailVerifiedAt != null,
    mfa_enabled = this.mfaEnabled,
    last_login_at = this.lastLoginAt?.toString(),
    created_at = this.createdAt?.toString() ?: "",
    updated_at = this.updatedAt?.toString()
)

private fun com.ground.service.BusinessWithRoles.toBusinessWithRolesResponse() = BusinessWithRolesResponse(
    id = this.id.toString(),
    name = this.name,
    slug = this.slug,
    logo_url = this.logoUrl,
    description = this.description,
    website = this.website,
    plan = this.plan,
    status = this.status,
    is_owner = this.isOwner,
    roles = this.roles.map { it.toRoleInfoResponse() },
    created_at = this.createdAt?.toString() ?: "",
    updated_at = this.updatedAt?.toString()
)

private fun com.ground.service.RoleInfo.toRoleInfoResponse() = RoleInfoResponse(
    id = this.id.toString(),
    name = this.name,
    display_name = this.displayName,
    color = this.color,
    is_system = this.isSystem
)

// ═══════════════════════════════════════════════════════════════
// SHARED UTILITY FUNCTIONS
// ═══════════════════════════════════════════════════════════════

/**
 * Get the real client IP address, handling proxies
 */
fun ApplicationCall.getClientIpAddress(): String {
    // Check X-Forwarded-For (when behind a proxy/load balancer)
    val forwarded = request.header("X-Forwarded-For")
    if (forwarded != null) {
        // Take the first IP (original client)
        return forwarded.split(",").firstOrNull()?.trim() ?: "unknown"
    }
    
    // Check X-Real-IP
    val realIp = request.header("X-Real-IP")
    if (realIp != null) {
        return realIp.trim()
    }
    
    // Fall back to direct connection IP
    return request.origin.remoteHost ?: "unknown"
}

/**
 * Extension to get current user ID from call
 */
fun ApplicationCall.getCurrentUserId(): UUID? {
    return try {
        // Try from attribute first (set by middleware)
        attributes.getOrNull(CurrentUserIdKey)
            ?: principal<UserIdPrincipal>()?.name?.let { UUID.fromString(it) }
    } catch (e: Exception) {
        null
    }
}

suspend fun ApplicationCall.respondUnauthorized() {
    respond(
        HttpStatusCode.Unauthorized,
        ErrorResponse(ErrorDetail("UNAUTHORIZED", "Authentication required", "authentication_error"))
    )
}

suspend fun ApplicationCall.respondBadRequest(message: String) {
    respond(
        HttpStatusCode.BadRequest,
        ErrorResponse(ErrorDetail("BAD_REQUEST", message, "validation_error"))
    )
}

suspend fun ApplicationCall.respondForbidden(message: String) {
    respond(
        HttpStatusCode.Forbidden,
        ErrorResponse(ErrorDetail("FORBIDDEN", message, "authorization_error"))
    )
}
