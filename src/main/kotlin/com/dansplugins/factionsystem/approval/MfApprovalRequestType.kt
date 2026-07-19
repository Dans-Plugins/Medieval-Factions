package com.dansplugins.factionsystem.approval

enum class MfApprovalRequestType {
    WAR,
    ALLY,
    VASSALIZE;

    val languageKey: String
        get() = when (this) {
            WAR -> "ApprovalRequestTypeWar"
            ALLY -> "ApprovalRequestTypeAlly"
            VASSALIZE -> "ApprovalRequestTypeVassalize"
        }
}
