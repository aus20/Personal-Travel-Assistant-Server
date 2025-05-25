// src/main/kotlin/com/aus20/security/CurrentUser.kt
package com.aus20.security

@Target(AnnotationTarget.VALUE_PARAMETER)
@Retention(AnnotationRetention.RUNTIME)
@MustBeDocumented
annotation class CurrentUser
